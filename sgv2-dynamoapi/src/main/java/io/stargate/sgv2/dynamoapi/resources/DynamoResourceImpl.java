package io.stargate.sgv2.dynamoapi.resources;

import static io.stargate.sgv2.dynamoapi.dynamo.Proxy.awsRequestMapper;

import com.amazonaws.AmazonWebServiceResult;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DeleteTableRequest;
import com.amazonaws.services.dynamodbv2.model.GetItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.QueryRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.stargate.bridge.proto.QueryOuterClass;
import io.stargate.sgv2.api.common.cql.builder.QueryBuilder;
import io.stargate.sgv2.api.common.cql.builder.Replication;
import io.stargate.sgv2.api.common.grpc.StargateBridgeClient;
import io.stargate.sgv2.dynamoapi.dynamo.ItemProxy;
import io.stargate.sgv2.dynamoapi.dynamo.Proxy;
import io.stargate.sgv2.dynamoapi.dynamo.QueryProxy;
import io.stargate.sgv2.dynamoapi.dynamo.TableProxy;
import io.stargate.sgv2.dynamoapi.models.DynamoStatementType;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamoResourceImpl implements DynamoResourceApi {
  private static final Logger logger = LoggerFactory.getLogger(DynamoResourceImpl.class);

  @Inject StargateBridgeClient bridge;
  @Inject TableProxy tableProxy;
  @Inject ItemProxy itemProxy;
  @Inject QueryProxy queryProxy;

  @Override
  public Response handleRequest(HttpHeaders headers, String target, String payload) {
    target = target.split("\\.")[1];
    DynamoStatementType statementType = DynamoStatementType.valueOf(target);
    byte[] response;
    try {
      AmazonWebServiceResult result;
      switch (statementType) {
        case CreateTable:
          CreateTableRequest createTableRequest =
              awsRequestMapper.readValue(payload, CreateTableRequest.class);
          result = tableProxy.createTable(createTableRequest, bridge);
          break;
        case DeleteTable:
          DeleteTableRequest deleteTableRequest =
              awsRequestMapper.readValue(payload, DeleteTableRequest.class);
          result = tableProxy.deleteTable(deleteTableRequest, bridge);
          break;
        case PutItem:
          PutItemRequest putItemRequest = awsRequestMapper.readValue(payload, PutItemRequest.class);
          result = itemProxy.putItem(putItemRequest, bridge);
          break;
        case GetItem:
          GetItemRequest getItemRequest = awsRequestMapper.readValue(payload, GetItemRequest.class);
          result = itemProxy.getItem(getItemRequest, bridge);
          break;
        case Query:
          QueryRequest queryRequest = awsRequestMapper.readValue(payload, QueryRequest.class);
          result = queryProxy.query(queryRequest, bridge);
          break;
        default:
          throw new WebApplicationException(
              "Invalid statement type: " + target, Response.Status.BAD_REQUEST);
      }
      response = awsRequestMapper.writeValueAsBytes(result);
    } catch (JsonProcessingException ex) {
      throw new WebApplicationException("Invalid payload", Response.Status.BAD_REQUEST);
    } catch (IOException ex) {
      throw new WebApplicationException(
          "An error occurred when connecting to Cassandra", Response.Status.INTERNAL_SERVER_ERROR);
    } catch (Exception ex) {
      throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
    }
    return Response.status(Response.Status.OK).entity(response).build();
  }

  @Override
  public Response createKeyspace() {
    QueryOuterClass.Query query =
        new QueryBuilder()
            .create()
            .keyspace(Proxy.KEYSPACE_NAME)
            .ifNotExists()
            .withReplication(Replication.simpleStrategy(1))
            .build();

    bridge.executeQuery(query);
    final Map<String, Object> responsePayload =
        Collections.singletonMap("name", Proxy.KEYSPACE_NAME);
    return Response.status(Response.Status.CREATED).entity(responsePayload).build();
  }
}
