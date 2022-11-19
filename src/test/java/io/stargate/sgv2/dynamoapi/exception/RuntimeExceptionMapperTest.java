package io.stargate.sgv2.dynamoapi.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

public class RuntimeExceptionMapperTest {

  @Test
  public void testExceptionMessage() {
    String message = "Unknown exception";
    RuntimeException ex = new RuntimeException(message);

    RuntimeExceptionMapper mapper = new RuntimeExceptionMapper();

    try (RestResponse<DynamoError> response = mapper.mapRuntimeException(ex)) {
      assertEquals(500, response.getStatus());
      assertTrue(response.getEntity().message().contains("exception message: Unknown exception"));
    }
  }
}
