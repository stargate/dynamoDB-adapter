package io.stargate.sgv2.dynamoapi.models;

public enum DynamoStatementType {
  PutItem,
  GetItem,
  CreateTable,
  DeleteTable,
  ListTables,
  DescribeTable,
  DeleteItem,
  Query;

  public static DynamoStatementType valueOfLowerCase(String arg) {
    DynamoStatementType[] stmtValues = DynamoStatementType.values();

    for (DynamoStatementType stmtValue : stmtValues) {
      if (stmtValue.toString().toLowerCase().equals(arg.toLowerCase())) {
        return stmtValue;
      }
    }
    throw new RuntimeException("invalid argument for DynamoDB Statement Type");
  }
}
