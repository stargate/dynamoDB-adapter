package io.stargate.sgv2.dynamoapi.resources;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Definition of REST API DDL endpoint methods including JAX-RS and OpenAPI annotations. No
 * implementations.
 */
@ApplicationScoped
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface DynamoResourceApi {
  @POST
  Response handleRequest(@HeaderParam("X-Amz-Target") String target, String payload);

  @GET
  @Path("/keyspace/create")
  Response createKeyspace();
}
