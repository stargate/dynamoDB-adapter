package io.stargate.sgv2.dynamoapi.exception;

import io.stargate.sgv2.dynamoapi.constants.AmazonConstants;
import java.util.UUID;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DynamoDB does not expect a generic IllegalArgumentException in favor of concrete exception types.
 * This exception mapper is to capture individual exceptions that are not (yet) correctly
 * categorized into concrete exception types.
 */
public class IllegalArgumentExceptionMapper {
  private static final Logger logger =
      LoggerFactory.getLogger(IllegalArgumentExceptionMapper.class);

  @ServerExceptionMapper
  public RestResponse<DynamoError> mapException(IllegalArgumentException e) {
    String errorId = UUID.randomUUID().toString();
    logger.error("Illegal argument exception, errorId[{}]", errorId, e);
    DynamoError error =
        new DynamoError(
            IllegalArgumentException.class.getName(),
            String.format("ErrorId[%s], exception message: %s", errorId, e.getMessage()));

    return RestResponse.ResponseBuilder.create(Response.Status.INTERNAL_SERVER_ERROR, error)
        .header(AmazonConstants.X_AMZN_ERROR_TYPE, AmazonConstants.INTERNAL_SERVER_ERROR)
        .build();
  }
}
