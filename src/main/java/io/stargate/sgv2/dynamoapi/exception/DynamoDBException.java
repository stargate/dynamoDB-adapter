package io.stargate.sgv2.dynamoapi.exception;

import io.stargate.sgv2.dynamoapi.constants.AmazonConstants;
import javax.ws.rs.core.Response;

/** Generic exception type for any unclassified exception */
public class DynamoDBException extends RuntimeException {

  final Response.Status status;
  final String errorType;

  public DynamoDBException(Throwable cause) {
    super(cause);
    this.status = Response.Status.INTERNAL_SERVER_ERROR;
    this.errorType = AmazonConstants.INTERNAL_SERVER_ERROR;
  }

  public DynamoDBException(Response.Status status, String errorType, String message) {
    super(message);
    this.status = status;
    this.errorType = errorType;
  }

  public DynamoDBException(
      Response.Status status, String errorType, String message, Throwable cause) {
    super(message, cause);
    this.status = status;
    this.errorType = errorType;
  }

  public Response.Status getStatus() {
    return status;
  }

  public String getErrorType() {
    return errorType;
  }
}
