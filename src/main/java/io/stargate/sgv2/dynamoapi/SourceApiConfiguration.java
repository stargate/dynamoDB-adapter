package io.stargate.sgv2.dynamoapi;

import io.stargate.sgv2.api.common.grpc.SourceApiQualifier;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Produces;

@ApplicationScoped
public class SourceApiConfiguration {

  @Produces
  @SourceApiQualifier
  public String sourceApi() {
    // FIXME: we must use "rest" since this is defined in Schema.SchemaRead.SourceApi
    return "rest";
  }
}
