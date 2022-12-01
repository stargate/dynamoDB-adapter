package io.stargate.sgv2.dynamoapi.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.stargate.sgv2.dynamoapi.constants.AmazonConstants;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

public class DynamoDBExceptionMapperTest {

  @Test
  public void testExceptionMessage() {
    DynamoDBException ex =
        new DynamoDBException(
            Response.Status.BAD_REQUEST,
            AmazonConstants.TABLE_ALREADY_EXISTS_EXCEPTION,
            "table xxx already exists");

    DynamoDBExceptionMapper mapper = new DynamoDBExceptionMapper();

    try (RestResponse<DynamoError> response = mapper.mapException(ex)) {
      assertEquals(400, response.getStatus());
      assertTrue(
          response.getEntity().message().contains("exception message: table xxx already exists"));
    }
  }
}
