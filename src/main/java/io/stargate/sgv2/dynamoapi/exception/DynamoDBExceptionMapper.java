package io.stargate.sgv2.dynamoapi.exception;

import io.stargate.sgv2.dynamoapi.constants.AmazonConstants;
import java.util.UUID;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamoDBExceptionMapper {

  private static final Logger logger = LoggerFactory.getLogger(DynamoDBExceptionMapper.class);

  @ServerExceptionMapper
  public RestResponse<DynamoError> mapException(DynamoDBException e) {
    String errorId = UUID.randomUUID().toString();
    logger.error("DynamoDB exception, errorId[{}]", errorId, e);
    DynamoError error =
        new DynamoError(
            DynamoDBException.class.getName(),
            String.format("ErrorId[%s], exception message: %s", errorId, e.getMessage()));

    return RestResponse.ResponseBuilder.create(e.getStatus(), error)
        .header(AmazonConstants.X_AMZN_ERROR_TYPE, e.getErrorType())
        .build();
  }
}
