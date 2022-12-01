package io.stargate.sgv2.dynamoapi.resources;

import static io.stargate.sgv2.dynamoapi.dynamo.Proxy.awsRequestMapper;

import com.amazonaws.AmazonWebServiceResult;
import com.amazonaws.services.dynamodbv2.model.*;
import io.stargate.bridge.proto.QueryOuterClass;
import io.stargate.sgv2.api.common.cql.builder.QueryBuilder;
import io.stargate.sgv2.api.common.cql.builder.Replication;
import io.stargate.sgv2.api.common.grpc.StargateBridgeClient;
import io.stargate.sgv2.dynamoapi.dynamo.ItemProxy;
import io.stargate.sgv2.dynamoapi.dynamo.Proxy;
import io.stargate.sgv2.dynamoapi.dynamo.QueryProxy;
import io.stargate.sgv2.dynamoapi.dynamo.TableProxy;
import io.stargate.sgv2.dynamoapi.exception.DynamoDBException;
import io.stargate.sgv2.dynamoapi.models.DynamoStatementType;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import javax.inject.Inject;
import javax.validation.ValidationException;
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
  public Response handleRequest(HttpHeaders headers, String target, String payload)
      throws IOException {
    target = target.split("\\.")[1];
    DynamoStatementType statementType = DynamoStatementType.valueOf(target);
    byte[] response;
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
      case DeleteItem:
        DeleteItemRequest deleteItemRequest =
            awsRequestMapper.readValue(payload, DeleteItemRequest.class);
        result = itemProxy.deleteItem(deleteItemRequest, bridge);
        break;
      case ListTables:
        ListTablesRequest listTablesRequest =
            awsRequestMapper.readValue(payload, ListTablesRequest.class);
        result = tableProxy.listTables(listTablesRequest, bridge);
        break;
      case Query:
        QueryRequest queryRequest = awsRequestMapper.readValue(payload, QueryRequest.class);
        result = queryProxy.query(queryRequest, bridge);
        break;
      default:
        throw new ValidationException("Invalid statement type: " + target);
    }
    response = awsRequestMapper.writeValueAsBytes(result);
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
    try {
      bridge.executeQuery(query);
    } catch (Exception ex) {
      throw new DynamoDBException(ex);
    }
    final Map<String, Object> responsePayload =
        Collections.singletonMap("name", Proxy.KEYSPACE_NAME);
    return Response.status(Response.Status.CREATED).entity(responsePayload).build();
  }
}
