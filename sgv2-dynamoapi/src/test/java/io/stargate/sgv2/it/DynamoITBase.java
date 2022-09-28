package io.stargate.sgv2.it;

import static io.restassured.RestAssured.given;

import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.local.embedded.DynamoDBEmbedded;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.stargate.sgv2.api.common.config.constants.HttpConstants;
import io.stargate.sgv2.common.IntegrationTestUtils;
import java.util.Properties;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestInstance;

@TestClassOrder(ClassOrderer.DisplayName.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DynamoITBase {
  protected AmazonDynamoDB awsClient = DynamoDBEmbedded.create().amazonDynamoDB();
  protected AmazonDynamoDB proxyClient;
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private String dynamoUrlBase;

  @BeforeEach
  public void setup() {
    dynamoUrlBase = RestAssured.basePath + ":" + RestAssured.port;
    String timestamp = "_" + System.currentTimeMillis();
    createKeyspace("dynamodb");

    Properties props = System.getProperties();
    props.setProperty("aws.accessKeyId", IntegrationTestUtils.getAuthToken());
    props.setProperty("aws.secretKey", "fake-secret-key");
    AwsClientBuilder.EndpointConfiguration endpointConfiguration =
        new AwsClientBuilder.EndpointConfiguration(dynamoUrlBase, "fake-region");
    proxyClient =
        AmazonDynamoDBClientBuilder.standard()
            .withEndpointConfiguration(endpointConfiguration)
            .build();
  }

  protected void createKeyspace(String keyspaceName) {
    // We are essentially doing this:
    // String cql =
    //    "CREATE KEYSPACE IF NOT EXISTS \"%s\" WITH replication = {'class': 'SimpleStrategy',
    // 'replication_factor': 1}"
    //        .formatted(keyspaceName);
    //
    // but use REST API itself to avoid having bootstrap CQL or Bridge client
    String createKeyspace = String.format("{\"name\": \"%s\", \"replicas\": 1}", keyspaceName);
    givenWithAuth()
        .contentType(ContentType.JSON)
        .body(createKeyspace)
        .when()
        .post(endpointPathForAllKeyspaces())
        .then()
        .statusCode(HttpStatus.SC_CREATED);
  }

  protected RequestSpecification givenWithAuth() {
    return givenWithAuthToken(IntegrationTestUtils.getAuthToken());
  }

  protected RequestSpecification givenWithoutAuth() {
    return given();
  }

  protected RequestSpecification givenWithAuthToken(String authTokenValue) {
    return given().header(HttpConstants.AUTHENTICATION_TOKEN_HEADER_NAME, authTokenValue);
  }

  /*
  /////////////////////////////////////////////////////////////////////////
  // Endpoint construction
  /////////////////////////////////////////////////////////////////////////
   */

  protected String endpointPathForAllKeyspaces() {
    return "/v2/schemas/keyspaces";
  }
}
