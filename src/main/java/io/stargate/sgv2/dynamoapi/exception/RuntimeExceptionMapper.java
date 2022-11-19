package io.stargate.sgv2.dynamoapi.exception;

import io.stargate.sgv2.dynamoapi.constants.AmazonConstants;
import java.util.UUID;
import javax.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RuntimeExceptionMapper {
  private static final Logger logger = LoggerFactory.getLogger(RuntimeExceptionMapper.class);

  @ServerExceptionMapper
  public RestResponse<DynamoError> mapRuntimeException(RuntimeException e) {
    String errorId = UUID.randomUUID().toString();
    logger.error("Non-captured runtime exception, errorId[{}]", errorId, e);
    DynamoError error =
        new DynamoError(
            RuntimeException.class.getName(),
            String.format("ErrorId[%s], exception message: %s", errorId, e.getMessage()));

    return RestResponse.ResponseBuilder.create(Response.Status.INTERNAL_SERVER_ERROR, error)
        .header(AmazonConstants.X_AMZN_ERROR_TYPE, AmazonConstants.INTERNAL_SERVER_ERROR)
        .build();
  }
}
