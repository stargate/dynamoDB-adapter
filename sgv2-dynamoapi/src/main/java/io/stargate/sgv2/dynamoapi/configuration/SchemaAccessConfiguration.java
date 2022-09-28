package io.stargate.sgv2.dynamoapi.configuration;

import io.stargate.bridge.proto.Schema;
import javax.inject.Singleton;

/** Defines all needed properties for schema access. */
public class SchemaAccessConfiguration {

  @Singleton
  Schema.SchemaRead.SourceApi sourceApi() {
    return Schema.SchemaRead.SourceApi.REST;
  }
}
