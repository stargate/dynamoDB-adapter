package io.stargate.sgv2.dynamoapi.dynamo;

import static com.amazonaws.services.dynamodbv2.model.ReturnValue.ALL_OLD;

import com.amazonaws.services.dynamodbv2.model.*;
import io.stargate.bridge.proto.QueryOuterClass;
import io.stargate.bridge.proto.Schema;
import io.stargate.sgv2.api.common.cql.builder.Predicate;
import io.stargate.sgv2.api.common.cql.builder.QueryBuilder;
import io.stargate.sgv2.api.common.grpc.StargateBridgeClient;
import io.stargate.sgv2.dynamoapi.exception.DynamoDBException;
import io.stargate.sgv2.dynamoapi.grpc.BridgeProtoTypeTranslator;
import io.stargate.sgv2.dynamoapi.parser.ExpressionLexer;
import io.stargate.sgv2.dynamoapi.parser.ExpressionParser;
import io.stargate.sgv2.dynamoapi.parser.FilterExpressionVisitor;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.commons.collections4.CollectionUtils;
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
    QueryOuterClass.Response response;
    try {
      response = bridge.executeQuery(queryBuilder.build());

    } catch (Exception ex) {
      throw new DynamoDBException(ex);
    }
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
      try {
        bridge.executeQuery(alterTableBuilder.build());
      } catch (Exception ex) {
        throw new DynamoDBException(ex);
      }
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
    try {
      bridge.executeQuery(writeDataBuilder.build());
    } catch (Exception ex) {
      throw new DynamoDBException(ex);
    }
    // Step 4 (TODO): Write index if applicable
    return new PutItemResult();
  }

  // CQL: DELETE FROM key.table WHERE PK = <val> [AND CK = <val>] [IF <condition>]
  public DeleteItemResult deleteItem(
      final DeleteItemRequest deleteItemRequest, final StargateBridgeClient bridge) {
    final String tableName = deleteItemRequest.getTableName();
    final String conditionExpression = deleteItemRequest.getConditionExpression();
    // get PK (and SK, if exists)
    final Map<String, AttributeValue> keyMap = deleteItemRequest.getKey();
    Schema.CqlTable table =
        bridge
            .getTable(KEYSPACE_NAME, tableName, false)
            .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableName));
    QueryOuterClass.ColumnSpec partitionKey = table.getPartitionKeyColumns(0);
    List<QueryOuterClass.ColumnSpec> clusteringKeys = table.getClusteringKeyColumnsList();
    Optional<QueryOuterClass.ColumnSpec> clusteringKey = Optional.empty();
    if (CollectionUtils.isNotEmpty(clusteringKeys)) {
      assert clusteringKeys.size() == 1;
      clusteringKey = Optional.of(clusteringKeys.get(0));
    }

    // Query first
    QueryBuilder.QueryBuilder__21 queryBuilder =
        new QueryBuilder()
            .select()
            .from(KEYSPACE_NAME, tableName)
            .where(
                partitionKey.getName(),
                Predicate.EQ,
                DataMapper.fromDynamo(keyMap.get(partitionKey.getName())));
    if (clusteringKey.isPresent()) {
      final String clusterKeyName = clusteringKey.get().getName();
      queryBuilder =
          queryBuilder.where(
              clusterKeyName, Predicate.EQ, DataMapper.fromDynamo(keyMap.get(clusterKeyName)));
    }
    QueryOuterClass.Response response;
    try {
      response = bridge.executeQuery(queryBuilder.build());
    } catch (Exception ex) {
      throw new DynamoDBException(ex);
    }
    Collection<Map<String, AttributeValue>> resultRows =
        collectResults(response.getResultSet(), "", new HashMap<String, String>());
    // if no result, exit
    if (resultRows.isEmpty()) {
      return new DeleteItemResult();
    }
    // DeleteItem only affect 1 row. Multiple delete need other requests combination (query+delete /
    // BatchWrite)
    assert resultRows.size() == 1;

    // Regard ConditionExpression as FilterExpression
    // Construct Predicate
    java.util.function.Predicate<Map<String, AttributeValue>> predicate;
    if (conditionExpression == null || conditionExpression.isEmpty()) {
      predicate = item -> true;
    } else {
      CharStream chars = CharStreams.fromString(conditionExpression);
      Lexer lexer = new ExpressionLexer(chars);
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      ExpressionParser parser = new ExpressionParser(tokens);
      FilterExpressionVisitor visitor =
          new FilterExpressionVisitor(
              deleteItemRequest.getExpressionAttributeNames(),
              deleteItemRequest.getExpressionAttributeValues());
      ParseTree tree = parser.expr();
      predicate = item -> visitor.isMatch(tree, item);
    }
    List<Map<String, AttributeValue>> resultList =
        resultRows.stream().filter(predicate).collect(Collectors.toList());

    // if no result, exit
    if (resultList.isEmpty()) {
      return new DeleteItemResult();
    }

    QueryBuilder.QueryBuilder__43 deleteBuilder =
        new QueryBuilder()
            .delete()
            .from(KEYSPACE_NAME, tableName)
            .where(
                partitionKey.getName(),
                Predicate.EQ,
                DataMapper.fromDynamo(keyMap.get(partitionKey.getName())));

    if (clusteringKey.isPresent()) {
      final String clusterKeyName = clusteringKey.get().getName();
      deleteBuilder =
          deleteBuilder.where(
              clusterKeyName, Predicate.EQ, DataMapper.fromDynamo(keyMap.get(clusterKeyName)));
    }
    try {
      bridge.executeQuery(deleteBuilder.build());
    } catch (Exception ex) {
      throw new DynamoDBException(ex);
    }
    final String returnValues = deleteItemRequest.getReturnValues();
    if (returnValues.equals(ALL_OLD.toString())) {
      return new DeleteItemResult().withAttributes(resultList.get(0));
    }
    return new DeleteItemResult();
  }
}
