package io.stargate.sgv2.it;

import static org.junit.jupiter.api.Assertions.*;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.model.*;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.stargate.sgv2.common.testresource.StargateTestResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@QuarkusIntegrationTest
@QuarkusTestResource(StargateTestResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DynamoApiTableIT extends DynamoITBase {
  @Test
  public void testDeleteNonExistentTable() {
    DeleteTableRequest req = new DeleteTableRequest().withTableName("non-existent");
    assertThrows(AmazonDynamoDBException.class, () -> awsClient.deleteTable(req));
    AmazonDynamoDBException actual =
        assertThrows(AmazonDynamoDBException.class, () -> proxyClient.deleteTable(req));
    // TODO: Unfortunately, unless we parse the error message from gRPC, there is no way
    // that we can identify and correctly categorize the exception thrown by Cassandra
    // Thus, the final exception (error code, error message) that user receives is NOT
    // exactly the same as Amazon DynamoDB's behavior
    assertTrue(actual.getMessage().contains("Table 'dynamodb.non-existent' doesn't exist"));
  }

  @Test
  public void testCreateThenDeleteTable() {
    CreateTableRequest req =
        new CreateTableRequest()
            .withTableName("foo")
            .withProvisionedThroughput(
                new ProvisionedThroughput()
                    .withReadCapacityUnits(100L)
                    .withWriteCapacityUnits(100L))
            .withKeySchema(new KeySchemaElement("Name", KeyType.HASH))
            .withAttributeDefinitions(new AttributeDefinition("Name", ScalarAttributeType.S));

    awsClient.createTable(req);
    proxyClient.createTable(req);

    awsClient.deleteTable("foo");
    proxyClient.deleteTable("foo");
  }

  @Test
  public void testListTables() {
    CreateTableRequest req1 =
        new CreateTableRequest()
            .withTableName("aaa")
            .withProvisionedThroughput(
                new ProvisionedThroughput()
                    .withReadCapacityUnits(100L)
                    .withWriteCapacityUnits(100L))
            .withKeySchema(new KeySchemaElement("Name", KeyType.HASH))
            .withAttributeDefinitions(new AttributeDefinition("Name", ScalarAttributeType.S));
    CreateTableRequest req2 = req1.clone().withTableName("bbb");
    CreateTableRequest req3 = req1.clone().withTableName("ccc");
    CreateTableRequest req4 = req1.clone().withTableName("ddd");

    awsClient.createTable(req1);
    awsClient.createTable(req2);
    awsClient.createTable(req3);
    awsClient.createTable(req4);
    proxyClient.createTable(req1);
    proxyClient.createTable(req2);
    proxyClient.createTable(req3);
    proxyClient.createTable(req4);

    ListTablesResult awsResult;
    ListTablesResult proxyResult;

    // no args
    awsResult = awsClient.listTables();
    proxyResult = proxyClient.listTables();
    assertEquals(awsResult, proxyResult);

    // startTableName
    awsResult = awsClient.listTables("bbb");
    proxyResult = proxyClient.listTables("bbb");
    assertEquals(awsResult, proxyResult);

    // limit
    awsResult = awsClient.listTables(2);
    proxyResult = proxyClient.listTables(2);
    assertEquals(awsResult, proxyResult);

    // start + limit
    awsResult = awsClient.listTables("bbb", 2);
    proxyResult = proxyClient.listTables("bbb", 2);
    assertEquals(awsResult, proxyResult);

    // when # of tables << # of query
    awsResult = awsClient.listTables(100);
    proxyResult = proxyClient.listTables(100);
    assertEquals(awsResult, proxyResult);

    // overflow
    AmazonServiceException awsEx =
        assertThrows(AmazonServiceException.class, () -> awsClient.listTables(999));
    AmazonServiceException stargateEx =
        assertThrows(AmazonServiceException.class, () -> proxyClient.listTables(999));
    assertException(awsEx, stargateEx);
  }
}
