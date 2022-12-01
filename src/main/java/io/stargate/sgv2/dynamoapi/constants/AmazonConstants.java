package io.stargate.sgv2.dynamoapi.constants;

public interface AmazonConstants {
  // Exception related constants
  String X_AMZN_ERROR_TYPE = "x-amzn-ErrorType";

  // The error codes must be consistent with AmazonDynamoDBClient.java. Unfortunately,
  // these string literals are spread across amazon client sdk codebase, and thus we
  // have to manually collect them and list them here
  // TODO: convert them into enum
  String INTERNAL_SERVER_ERROR = "InternalServerError";

  String TABLE_NOT_FOUND_EXCEPTION = "TableNotFoundException";

  String TABLE_ALREADY_EXISTS_EXCEPTION = "TableAlreadyExistsException";

  String VALIDATION_EXCEPTION = "ValidationException";
}
