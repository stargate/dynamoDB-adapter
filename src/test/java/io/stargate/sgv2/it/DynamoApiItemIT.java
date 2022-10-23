package io.stargate.sgv2.it;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.stargate.sgv2.common.testresource.StargateTestResource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@QuarkusIntegrationTest
@QuarkusTestResource(StargateTestResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DynamoApiItemIT extends DynamoITBase {
  private String tableName = "item_api_test_table";

  @BeforeEach
  public void setUpTable() {
    createTable();
  }

  @AfterEach
  public void deleteTable() {
    awsClient.deleteTable(tableName);
    proxyClient.deleteTable(tableName);
  }

  @Test
  public void testBasicCreateAndGetItem() {
    DynamoDB proxyDynamoDB = new DynamoDB(proxyClient);
    Table proxyTable = proxyDynamoDB.getTable(tableName);

    // put a simple item first
    proxyTable.putItem(
        new Item()
            .withPrimaryKey("Name", "simpleName")
            .withNumber("Serial", 23)
            .withNumber("Price", 10.0));

    // put another simple item with no new column
    proxyTable.putItem(
        new Item()
            .withPrimaryKey("Name", "simpleName2")
            .withNumber("Serial", 20)
            .withNumber("Price", 0.0)
            .withString("Desc", "dummy text"));

    Map<String, Object> dict = new HashMap<>();
    dict.put("integerList", Arrays.asList(0, 1, 2));
    dict.put("stringList", Arrays.asList("aa", "bb"));
    // TODO: support null value in Stargate
    //    dict.put("nullKey", null);
    dict.put("hashMap", new HashMap<>());
    dict.put("doubleSet", new HashSet<>(Arrays.asList(1.0, 2.0)));
    Item item =
        new Item()
            .withPrimaryKey("Name", "testName")
            .withNumber("Serial", 123.0)
            .withString("ISBN", "121-1111111111")
            .withStringSet("Authors", new HashSet<String>(Arrays.asList("Author21", "Author 22")))
            .withNumber("Price", 20.1)
            .withString("Dimensions", "8.5x11.0x.75")
            .withNumber("PageCount", 500)
            .withBoolean("InPublication", true)
            .withString("ProductCategory", "Book")
            .withMap("Dict", dict);
    proxyTable.putItem(item);
    Item proxyResult = proxyTable.getItem("Name", "testName");

    DynamoDB awsDynamoDB = new DynamoDB(awsClient);
    Table awsTable = awsDynamoDB.getTable(tableName);
    awsTable.putItem(item);
    Item awsResult = awsTable.getItem("Name", "testName");
    assertEquals(awsResult, proxyResult);
  }

  /**
   * Test GetItem API with projectionExpression A few caveats that are not all documented by
   * DynamoDB:<br>
   * 1. Sets (StringSet, NumberSet, etc.) do not support offset-based projection<br>
   * 2. Any token can be replaced by a placeholder with '#' prefix. It doesn't have to be top-level
   * attribute. For example, "top.second[0]" can be replaced by "#t.#s[0]" with corresponding name
   * map<br>
   * 3. Projection results are merged if they belong to the same top-level attribute, e.g.
   * "dict.integerList[0],dict.stringList[1][2]" would be merged
   */
  @Test
  public void testGetItemWithProjection() {
    DynamoDB proxyDynamoDB = new DynamoDB(proxyClient);
    DynamoDB awsDynamoDB = new DynamoDB(awsClient);
    Table proxyTable = proxyDynamoDB.getTable(tableName);
    Table awsTable = awsDynamoDB.getTable(tableName);

    Map<String, Object> dict = new HashMap<>();
    dict.put("integerList", Arrays.asList(0, 1, 2));
    dict.put("stringList", Arrays.asList("aa", "bb"));
    Item item =
        new Item()
            .withPrimaryKey("Name", "simpleName")
            .withNumber("Serial", 23)
            .withNumber("Price", 10.0)
            .withList("Authors", Arrays.asList("Author21", "Author 22", dict, "Author44"))
            .withStringSet("StringSet", "ss1", "ss2", "ss3")
            .withNumberSet("NumberSet", 2, 4, 5)
            .withMap("dict", dict);
    proxyTable.putItem(item);
    awsTable.putItem(item);

    PrimaryKey key = new PrimaryKey("Name", "simpleName");
    String projection =
        "#N, Authors[0], Authors[2].#IL[0], Authors[3], #D.stringList[0], #D.#IL[1], NumberSet, StringSet[0]";
    Map<String, String> nameMap =
        new HashMap() {
          {
            put("#N", "Name");
            put("#D", "dict");
            put("#IL", "integerList");
          }
        };
    Item awsResult = awsTable.getItem(key, projection, nameMap);
    Item proxyResult = proxyTable.getItem(key, projection, nameMap);
    assertEquals(awsResult, proxyResult);
  }

  private void createTable() {
    CreateTableRequest req =
        new CreateTableRequest()
            .withTableName(tableName)
            .withProvisionedThroughput(
                new ProvisionedThroughput()
                    .withReadCapacityUnits(100L)
                    .withWriteCapacityUnits(100L))
            .withKeySchema(new KeySchemaElement("Name", KeyType.HASH))
            .withAttributeDefinitions(new AttributeDefinition("Name", ScalarAttributeType.S));
    proxyClient.createTable(req);
    awsClient.createTable(req);
  }
}
