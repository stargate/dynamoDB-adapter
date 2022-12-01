package io.stargate.sgv2.dynamoapi.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.validation.ValidationException;
import org.jboss.resteasy.reactive.RestResponse;
import org.junit.jupiter.api.Test;

public class ValidationExceptionMapperTest {

  @Test
  public void testExceptionMessage() {
    ValidationException ex = new ValidationException("Limit in ListTables must be <= 100");

    ValidationExceptionMapper mapper = new ValidationExceptionMapper();

    try (RestResponse<DynamoError> response = mapper.mapException(ex)) {
      assertEquals(400, response.getStatus());
      assertTrue(
          response
              .getEntity()
              .message()
              .contains("exception message: Limit in ListTables must be <= 100"));
    }
  }
}
