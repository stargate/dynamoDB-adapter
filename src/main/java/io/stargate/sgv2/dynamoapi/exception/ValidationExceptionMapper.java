package io.stargate.sgv2.dynamoapi.exception;

import io.stargate.sgv2.dynamoapi.constants.AmazonConstants;
import java.util.UUID;
import javax.validation.ValidationException;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidationExceptionMapper {

  private static final Logger logger = LoggerFactory.getLogger(ValidationExceptionMapper.class);

  @ServerExceptionMapper
  public RestResponse<DynamoError> mapException(ValidationException e) {
    String errorId = UUID.randomUUID().toString();
    logger.error("DynamoDB exception, errorId[{}]", errorId, e);
    DynamoError error =
        new DynamoError(
            DynamoDBException.class.getName(),
            String.format("ErrorId[%s], exception message: %s", errorId, e.getMessage()));

    return RestResponse.ResponseBuilder.create(Response.Status.BAD_REQUEST, error)
        .header(AmazonConstants.X_AMZN_ERROR_TYPE, AmazonConstants.VALIDATION_EXCEPTION)
        .build();
  }
}
