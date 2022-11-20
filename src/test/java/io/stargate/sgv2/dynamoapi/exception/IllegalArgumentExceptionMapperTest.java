package io.stargate.sgv2.dynamoapi.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

public class IllegalArgumentExceptionMapperTest {

  @Test
  public void testExceptionMessage() {
    String message = "Given value is not valid";
    IllegalArgumentException ex = new IllegalArgumentException(message);

    IllegalArgumentExceptionMapper mapper = new IllegalArgumentExceptionMapper();

    try (RestResponse<DynamoError> response = mapper.mapException(ex)) {
      assertEquals(500, response.getStatus());
      assertTrue(
          response.getEntity().message().contains("exception message: Given value is not valid"));
    }
  }
}
