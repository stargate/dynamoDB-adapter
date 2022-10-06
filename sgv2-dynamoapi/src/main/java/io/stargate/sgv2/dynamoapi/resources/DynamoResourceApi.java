package io.stargate.sgv2.dynamoapi.resources;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;

/**
 * Definition of REST API DDL endpoint methods including JAX-RS and OpenAPI annotations. No
 * implementations.
 */
@ApplicationScoped
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public interface DynamoResourceApi {
  @POST
  @Consumes("application/x-amz-json-1.0")
  @Path("/")
  Response handleRequest(
      @Context HttpHeaders headers,
      @Parameter(name = "X-Amz-Target") @HeaderParam("X-Amz-Target") String target,
      @RequestBody(required = true) String payload);

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Path("/keyspace/create")
  Response createKeyspace();
}
