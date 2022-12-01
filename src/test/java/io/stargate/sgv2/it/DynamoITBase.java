package io.stargate.sgv2.it;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.amazonaws.AmazonServiceException;
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
    dynamoUrlBase = RestAssured.baseURI + ":" + RestAssured.port + "/v2";
    createKeyspace();

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

  protected void createKeyspace() {
    givenWithAuth()
        .contentType(ContentType.JSON)
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

  protected void assertException(AmazonServiceException expected, AmazonServiceException actual) {
    assertEquals(expected.getErrorCode(), actual.getErrorCode());
    assertEquals(expected.getErrorType(), actual.getErrorType());
    // Our system also records a unique identifier of the exception, which amazon does not
    assertTrue(expected.getMessage().contains(actual.getMessage()));
  }

  /*
  /////////////////////////////////////////////////////////////////////////
  // Endpoint construction
  /////////////////////////////////////////////////////////////////////////
   */

  protected String endpointPathForAllKeyspaces() {
    return "/v2/keyspace/create";
  }
}
