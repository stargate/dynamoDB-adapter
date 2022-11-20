package io.stargate.sgv2.dynamoapi.exception;

public record DynamoError(String __type, String message) {
  public DynamoError(String __type, String message) {
    this.__type = __type;
    this.message = message;
  }
}
