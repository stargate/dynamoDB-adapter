package io.stargate.sgv2.dynamoapi.dynamo;

import static com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperFieldModel.DynamoDBAttributeType.valueOf;
import static java.lang.Integer.min;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperFieldModel;
import com.amazonaws.services.dynamodbv2.model.*;
import io.stargate.bridge.proto.QueryOuterClass;
import io.stargate.sgv2.api.common.cql.builder.Column;
import io.stargate.sgv2.api.common.cql.builder.ImmutableColumn;
import io.stargate.sgv2.api.common.cql.builder.Predicate;
import io.stargate.sgv2.api.common.cql.builder.QueryBuilder;
import io.stargate.sgv2.api.common.grpc.StargateBridgeClient;
import io.stargate.sgv2.dynamoapi.exception.DynamoDBException;
import io.stargate.sgv2.dynamoapi.models.PrimaryKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.validation.ValidationException;

@ApplicationScoped
public class TableProxy extends Proxy {

  private static final String TABLE_NAME = "table_name";
  private static final String SYSTEM_SCHEMA = "system_schema";
  private static final String TABLES = "tables";
  private static final String KEYSPACE_NAME_COLUMN = "keyspace_name";

  public CreateTableResult createTable(
      CreateTableRequest createTableRequest, StargateBridgeClient bridge) {
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
    try {
      bridge.executeQuery(query);
    } catch (Exception ex) {
      throw new DynamoDBException(ex);
    }

    TableDescription newTableDesc =
        this.getTableDescription(
            tableName,
            createTableRequest.getAttributeDefinitions(),
            createTableRequest.getKeySchema());
    return (new CreateTableResult()).withTableDescription(newTableDesc);
  }

  public DeleteTableResult deleteTable(
      DeleteTableRequest deleteTableRequest, StargateBridgeClient bridge) {
    final String tableName = deleteTableRequest.getTableName();
    QueryOuterClass.Query query = new QueryBuilder().drop().table(KEYSPACE_NAME, tableName).build();

    try {
      bridge.executeQuery(query);
    } catch (Exception ex) {
      throw new DynamoDBException(ex);
    }

    TableDescription tableDesc =
        new TableDescription()
            .withTableName(tableName)
            .withTableStatus(TableStatus.DELETING)
            .withTableArn(tableName);
    return (new DeleteTableResult()).withTableDescription(tableDesc);
  }

  public ListTablesResult listTables(
      ListTablesRequest listTablesRequest, StargateBridgeClient bridge) {
    final String startTableName = listTablesRequest.getExclusiveStartTableName();
    Integer limit = listTablesRequest.getLimit();
    // Range of limit: 1~100
    // Source:
    // https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_ListTables.html#API_ListTables_RequestSyntax
    if (limit == null) {
      limit = 100;
    }
    if (limit > 100) {
      throw new ValidationException("Limit in ListTables must be <= 100");
    }
    QueryBuilder.QueryBuilder__21 queryBuilder =
        new QueryBuilder()
            .select()
            .column(TABLE_NAME)
            .from(SYSTEM_SCHEMA, TABLES)
            .where(
                KEYSPACE_NAME_COLUMN,
                Predicate.EQ,
                DataMapper.fromDynamo(DataMapper.toDynamo(KEYSPACE_NAME)));
    QueryOuterClass.Response response;
    try {
      response = bridge.executeQuery(queryBuilder.build());
    } catch (Exception ex) {
      throw new DynamoDBException(ex);
    }
    List<String> allTableNames =
        response.getResultSet().getRowsList().stream()
            .map(row -> row.getValues(0))
            .map(QueryOuterClass.Value::getString)
            .collect(Collectors.toList());
    int startIndex = 0;
    if (startTableName != null && !startTableName.isEmpty()) {
      startIndex = allTableNames.indexOf(startTableName);
      startIndex = startIndex == -1 ? 0 : startIndex + 1;
    }
    if (startIndex == allTableNames.size()) {
      return new ListTablesResult();
    }
    int endIndex = min(allTableNames.size(), startIndex + limit);
    List<String> tableNames = allTableNames.subList(startIndex, endIndex);
    if (endIndex == allTableNames.size()) {
      return new ListTablesResult().withTableNames(tableNames);
    }
    return new ListTablesResult()
        .withTableNames(tableNames)
        .withLastEvaluatedTableName(tableNames.get(endIndex - 1));
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
