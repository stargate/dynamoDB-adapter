package io.stargate.sgv2.dynamoapi.dynamo;

import static com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperFieldModel.DynamoDBAttributeType.valueOf;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperFieldModel;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import com.amazonaws.services.dynamodbv2.model.TableStatus;
import io.stargate.bridge.proto.QueryOuterClass;
import io.stargate.sgv2.api.common.cql.builder.Column;
import io.stargate.sgv2.api.common.cql.builder.ImmutableColumn;
import io.stargate.sgv2.api.common.cql.builder.QueryBuilder;
import io.stargate.sgv2.api.common.grpc.StargateBridgeClient;
import io.stargate.sgv2.dynamoapi.models.PrimaryKey;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TableProxy extends Proxy {
  public CreateTableResult createTable(
      CreateTableRequest createTableRequest, StargateBridgeClient bridge) throws IOException {
    final String tableName = createTableRequest.getTableName();
    List<Column> columns = new ArrayList<>();
    final PrimaryKey primaryKey = getPrimaryKey(createTableRequest.getKeySchema());
    for (AttributeDefinition columnDef : createTableRequest.getAttributeDefinitions()) {
      final String columnName = columnDef.getAttributeName();
      final DynamoDBMapperFieldModel.DynamoDBAttributeType type =
          valueOf(columnDef.getAttributeType());
      ImmutableColumn.Builder column =
          ImmutableColumn.builder().name(columnName).type(DataTypeMapper.fromDynamo(type));
      if (columnName.equals(primaryKey.getPartitionKey())) {
        column.kind(Column.Kind.PARTITION_KEY);
      } else if (columnName.equals(primaryKey.getClusteringKey())) {
        column.kind(Column.Kind.CLUSTERING);
      }
      columns.add(column.build());
    }

    QueryOuterClass.Query query =
        new QueryBuilder().create().table(KEYSPACE_NAME, tableName).column(columns).build();

    bridge.executeQuery(query);

    TableDescription newTableDesc =
        this.getTableDescription(
            tableName,
            createTableRequest.getAttributeDefinitions(),
            createTableRequest.getKeySchema());
    return (new CreateTableResult()).withTableDescription(newTableDesc);
  }

  public DeleteTableResult deleteTable(
      DeleteTableRequest deleteTableRequest, StargateBridgeClient bridge) throws IOException {
    final String tableName = deleteTableRequest.getTableName();
    QueryOuterClass.Query query = new QueryBuilder().drop().table(KEYSPACE_NAME, tableName).build();

    bridge.executeQuery(query);
    // TODO: throws appropriate exception when it fails

    TableDescription tableDesc =
        new TableDescription()
            .withTableName(tableName)
            .withTableStatus(TableStatus.DELETING)
            .withTableArn(tableName);
    return (new DeleteTableResult()).withTableDescription(tableDesc);
  }

  private TableDescription getTableDescription(
      String tableName,
      Collection<AttributeDefinition> attributeDefinitions,
      Collection<KeySchemaElement> keySchema) {
    TableDescription tableDescription =
        (new TableDescription())
            .withTableName(tableName)
            .withAttributeDefinitions(attributeDefinitions)
            .withKeySchema(keySchema)
            .withTableStatus(TableStatus.ACTIVE)
            .withCreationDateTime(new Date())
            .withTableArn(tableName);

    return tableDescription;
  }
}
