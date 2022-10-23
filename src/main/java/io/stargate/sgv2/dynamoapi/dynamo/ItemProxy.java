package io.stargate.sgv2.dynamoapi.dynamo;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import io.stargate.bridge.proto.QueryOuterClass;
import io.stargate.bridge.proto.Schema;
import io.stargate.sgv2.api.common.cql.builder.Predicate;
import io.stargate.sgv2.api.common.cql.builder.QueryBuilder;
import io.stargate.sgv2.api.common.grpc.StargateBridgeClient;
import io.stargate.sgv2.dynamoapi.grpc.BridgeProtoTypeTranslator;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ItemProxy extends ProjectiveProxy {

  private static final Logger logger = LoggerFactory.getLogger(ItemProxy.class);

  public GetItemResult getItem(GetItemRequest getItemRequest, StargateBridgeClient bridge) {
    final String tableName = getItemRequest.getTableName();
    Map<String, AttributeValue> keyMap = getItemRequest.getKey();
    QueryBuilder.QueryBuilder__21 queryBuilder =
        new QueryBuilder().select().from(KEYSPACE_NAME, tableName);
    for (Map.Entry<String, AttributeValue> entry : keyMap.entrySet()) {
      queryBuilder =
          queryBuilder.where(entry.getKey(), Predicate.EQ, DataMapper.fromDynamo(entry.getValue()));
    }
    QueryOuterClass.Response response = bridge.executeQuery(queryBuilder.build());
    Collection<Map<String, AttributeValue>> resultRows =
        collectResults(
            response.getResultSet(),
            getItemRequest.getProjectionExpression(),
            getItemRequest.getExpressionAttributeNames());
    GetItemResult result = new GetItemResult();
    if (!resultRows.isEmpty()) {
      assert resultRows.size() == 1;
      result.setItem(resultRows.iterator().next());
    }
    return result;
  }

  public PutItemResult putItem(PutItemRequest putItemRequest, StargateBridgeClient bridge)
      throws IOException {
    // Step 1: Fetch table schema
    final String tableName = putItemRequest.getTableName();
    Schema.CqlTable table =
        bridge
            .getTable(KEYSPACE_NAME, tableName, false)
            .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableName));
    Map<String, QueryOuterClass.ColumnSpec> columnMap =
        table.getColumnsList().stream()
            .collect(Collectors.toMap(QueryOuterClass.ColumnSpec::getName, Function.identity()));
    QueryOuterClass.ColumnSpec partitionKey = table.getPartitionKeyColumns(0);
    columnMap.put(partitionKey.getName(), partitionKey);
    List<QueryOuterClass.ColumnSpec> clusteringKeys = table.getClusteringKeyColumnsList();
    if (!clusteringKeys.isEmpty()) {
      assert clusteringKeys.size() == 1;
      QueryOuterClass.ColumnSpec clusteringKey = clusteringKeys.get(0);
      columnMap.put(clusteringKey.getName(), clusteringKey);
    }

    // Step 2: Alter table column definition if needed
    QueryBuilder.QueryBuilder__29 alterTableBuilder = null;
    Map<String, AttributeValue> attributes = putItemRequest.getItem();
    for (Map.Entry<String, AttributeValue> attr : attributes.entrySet()) {
      final String key = attr.getKey();
      final AttributeValue value = attr.getValue();
      QueryOuterClass.ColumnSpec columnSpec = columnMap.get(key);
      if (columnSpec != null) {
        // Validate type is consistent. This is a requirement from Cassandra but not
        // really from DynamoDB. In DynamoDB, you could have a column with mixed types
        // of values, but it is not allowed in Cassandra.
        String dataType = DataTypeMapper.fromDynamo(value);
        String schemaType =
            BridgeProtoTypeTranslator.cqlTypeFromBridgeTypeSpec(columnSpec.getType(), false);
        if (!Objects.equals(dataType, schemaType)) {
          throw new IllegalArgumentException(
              String.format(
                  "Column %s should be of %s type, but data is of %s type",
                  key, schemaType, dataType));
        }
      } else {
        // Alter the table
        if (alterTableBuilder == null) {
          alterTableBuilder =
              new QueryBuilder()
                  .alter()
                  .table(KEYSPACE_NAME, tableName)
                  .addColumn(key, DataTypeMapper.fromDynamo(value));
        } else {
          alterTableBuilder = alterTableBuilder.addColumn(key, DataTypeMapper.fromDynamo(value));
        }
      }
    }
    if (alterTableBuilder != null) {
      bridge.executeQuery(alterTableBuilder.build());
    }

    // Step 3: Write data
    QueryBuilder.QueryBuilder__18 writeDataBuilder = null;
    for (Map.Entry<String, AttributeValue> attr : attributes.entrySet()) {
      final String key = attr.getKey();
      final AttributeValue value = attr.getValue();
      Pair<String, Object> pair;
      try {
        pair = ImmutablePair.of(key, DataMapper.fromDynamo(value));
      } catch (Exception ex) {
        // TODO
        continue;
      }
      if (writeDataBuilder == null) {
        writeDataBuilder =
            new QueryBuilder()
                .insertInto(KEYSPACE_NAME, tableName)
                .value(pair.getKey(), pair.getValue());
      } else {
        writeDataBuilder = writeDataBuilder.value(pair.getKey(), pair.getValue());
      }
    }
    bridge.executeQuery(writeDataBuilder.build());
    // Step 4 (TODO): Write index if applicable
    return new PutItemResult();
  }
}
