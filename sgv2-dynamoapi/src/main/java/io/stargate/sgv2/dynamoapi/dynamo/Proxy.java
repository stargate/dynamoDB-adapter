package io.stargate.sgv2.dynamoapi.dynamo;

import static com.amazonaws.services.dynamodbv2.model.KeyType.HASH;
import static com.amazonaws.services.dynamodbv2.model.KeyType.RANGE;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import io.stargate.bridge.proto.QueryOuterClass;
import io.stargate.sgv2.dynamoapi.grpc.BridgeProtoValueConverters;
import io.stargate.sgv2.dynamoapi.grpc.FromProtoConverter;
import io.stargate.sgv2.dynamoapi.models.PrimaryKey;
import io.stargate.sgv2.dynamoapi.utils.ListUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Proxy {
  private static final Logger logger = LoggerFactory.getLogger(Proxy.class);

  public static final String KEYSPACE_NAME = "dynamodb";
  public static final ObjectMapper awsRequestMapper =
      new ObjectMapper()
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .setSerializationInclusion(JsonInclude.Include.NON_NULL)
          .setPropertyNamingStrategy(
              // Map to AWS api style
              new PropertyNamingStrategy.UpperCamelCaseStrategy() {
                @Override
                public String translate(String input) {
                  String output = super.translate(input);

                  if (output != null && output.length() >= 2) {
                    switch (output) {
                      case "Ss":
                        return "SS";
                      case "Bool":
                        return "BOOL";
                      case "Ns":
                        return "NS";
                      default:
                        break;
                    }
                  }

                  return output;
                }
              });

  protected PrimaryKey getPrimaryKey(List<KeySchemaElement> keySchema) {
    PrimaryKey primaryKey = new PrimaryKey();
    for (KeySchemaElement keySchemaElement : keySchema) {
      String type = keySchemaElement.getKeyType();
      String name = keySchemaElement.getAttributeName();
      if (type.equals(HASH.toString())) {
        primaryKey.setPartitionKey(name);
      } else if (type.equals(RANGE.toString())) {
        primaryKey.setClusteringKey(name);
      }
    }
    return primaryKey;
  }

  /**
   * Given a raw key name in expression, retrieve its real key name
   *
   * <p>A raw key name might be a key name or a key placeholder. A placeholder starts with a '#'
   * pound sign, whose actual value needs to be retrieved from attributeNames.
   *
   * <p>See more at
   * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.ExpressionAttributeNames.html
   *
   * @param rawKeyName
   * @param attributeNames
   * @return
   */
  protected String getKeyName(String rawKeyName, Map<String, String> attributeNames) {
    if (rawKeyName.charAt(0) == '#') {
      return attributeNames.get(rawKeyName);
    } else {
      return rawKeyName;
    }
  }

  protected List<Map<String, AttributeValue>> convertRows(
      QueryOuterClass.ResultSet rs, int[] retainIndices) {
    FromProtoConverter converter =
        BridgeProtoValueConverters.instance()
            .fromProtoConverter(ListUtils.createSublist(rs.getColumnsList(), retainIndices));
    List<Map<String, AttributeValue>> resultRows = new ArrayList<>();
    List<QueryOuterClass.Row> rows = rs.getRowsList();
    for (QueryOuterClass.Row row : rows) {
      resultRows.add(
          converter.mapFromProtoValues(
              ListUtils.createSublist(row.getValuesList(), retainIndices)));
    }
    return resultRows;
  }

  protected AttributeValue getExpressionAttributeValue(
      Map<String, AttributeValue> map, String key) {
    AttributeValue value = map.get(key);
    if (value == null) {
      throw new IllegalArgumentException(key + " does not appear in expression attributes");
    }
    return value;
  }
}
