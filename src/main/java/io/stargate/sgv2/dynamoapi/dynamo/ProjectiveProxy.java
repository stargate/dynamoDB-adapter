package io.stargate.sgv2.dynamoapi.dynamo;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.common.base.Preconditions;
import io.stargate.bridge.proto.QueryOuterClass;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ProjectiveProxy supports ProjectionExpression.
 *
 * <p>See
 * https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Expressions.Attributes.html for
 * the grammar for projection.
 *
 * <p>See
 * https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_GetItem.html#DDB-GetItem-request-ProjectionExpression
 * and
 * https://docs.aws.amazon.com/amazondynamodb/latest/APIReference/API_Query.html#DDB-Query-request-ProjectionExpression
 * for sample usage in GetItem and Query APIs.
 */
public abstract class ProjectiveProxy extends Proxy {
  private static final Logger logger = LoggerFactory.getLogger(ProjectiveProxy.class);

  /**
   * Transform Stargate results and apply projection query
   *
   * @param rs
   * @param projectionQuery
   * @param attrNames
   * @return
   */
  protected Collection<Map<String, AttributeValue>> collectResults(
      QueryOuterClass.ResultSet rs, String projectionQuery, Map<String, String> attrNames) {
    // collect (top-level attribute name -> remaining path) mapping
    Map<String, List<String>> projectionMap = new HashMap<>();
    if (StringUtils.isNotEmpty(projectionQuery)) {
      String[] projections = projectionQuery.split(",");
      for (String projection : projections) {
        projection = projection.trim();
        int i = 0;
        while (i < projection.length()) {
          if (projection.charAt(i) == '.' || projection.charAt(i) == '[') {
            // we only need top level attribute name "A" from "A.B" or "A[0]"
            break;
          }
          i++;
        }
        String topLevelName = getKeyName(projection.substring(0, i), attrNames);
        projectionMap
            .computeIfAbsent(topLevelName, k -> new ArrayList<>())
            .add(projection.substring(i));
      }
    }
    // record the indices of the columns to retain
    int[] retainIndices = new int[projectionMap.size()];
    for (int i = 0, j = 0, size = rs.getColumnsList().size(); i < size; i++) {
      QueryOuterClass.ColumnSpec col = rs.getColumnsList().get(i);
      if (projectionMap.containsKey(col.getName())) {
        retainIndices[j++] = i;
      }
    }

    // retrieve retained columns and extract projection values
    List<Map<String, AttributeValue>> results = convertRows(rs, retainIndices);
    for (Map<String, AttributeValue> item : results) {
      for (String name : item.keySet()) {
        item.put(name, extractProjectionValue(item.get(name), projectionMap.get(name), attrNames));
      }
    }
    return results;
  }

  private AttributeValue extractProjectionValue(
      AttributeValue fullValue, List<String> paths, Map<String, String> attrNames) {
    if (CollectionUtils.isEmpty(paths)) {
      return fullValue;
    }
    AttributeValue projectedValue = null;
    for (String path : paths) {
      AttributeValue subValue = extractProjectionValue(fullValue, path, attrNames);
      if (projectedValue == null) {
        projectedValue = subValue;
      } else {
        mergeAttributeValue(projectedValue, subValue);
      }
    }
    purgeListPlaceHolders(projectedValue);
    return projectedValue;
  }

  /**
   * Merge extracted (projected) value back to the final data structure For example, if a "list" is
   * projected to "list[0], list[2]", then we should extract the 1st and 3rd element from the
   * "list", and then create a new "list" with only these 2 elements.
   *
   * @param fullValue
   * @param subValue
   */
  private void mergeAttributeValue(AttributeValue fullValue, AttributeValue subValue) {
    // subValue can be null when the projection subexpression is invalid
    if (subValue == null) return;
    if (subValue.getM() != null) {
      Preconditions.checkArgument(fullValue.getM() != null);
      Preconditions.checkArgument(subValue.getM().size() == 1);
      String key = subValue.getM().keySet().iterator().next();
      if (fullValue.getM().containsKey(key)) {
        mergeAttributeValue(fullValue.getM().get(key), subValue.getM().get(key));
      } else {
        fullValue.getM().put(key, subValue.getM().get(key));
      }
    } else {
      Preconditions.checkArgument(fullValue.getL() != null && subValue.getL() != null);
      Preconditions.checkArgument(fullValue.getL().size() == subValue.getL().size());
      for (int i = 0, end = subValue.getL().size(); i < end; i++) {
        AttributeValue childValue = subValue.getL().get(i);
        if (childValue != null) {
          if (fullValue.getL().get(i) != null) {
            mergeAttributeValue(fullValue.getL().get(i), childValue);
          } else {
            fullValue.getL().set(i, childValue);
          }
        }
      }
    }
  }

  /**
   * Helper function to purge empty lists
   *
   * @param value
   */
  private void purgeListPlaceHolders(AttributeValue value) {
    if (value == null) return;
    if (value.getL() != null) {
      List<AttributeValue> newList = new ArrayList<>();
      for (AttributeValue v : value.getL()) {
        if (v != null) {
          purgeListPlaceHolders(v);
          newList.add(v);
        }
      }
      value.setL(newList);
    } else if (value.getM() != null) {
      for (AttributeValue v : value.getM().values()) {
        purgeListPlaceHolders(v);
      }
    }
  }

  private AttributeValue extractProjectionValue(
      AttributeValue fullValue, String path, Map<String, String> attrNames) {
    Preconditions.checkArgument(fullValue != null, "AttributeValue cannot be null");
    if (StringUtils.isEmpty(path)) {
      return fullValue;
    }
    if (path.charAt(0) == '.') {
      // map structure
      Preconditions.checkArgument(
          fullValue.getM() != null, "AttributeValue is not a valid map: " + fullValue);
      int i = 1;
      while (i < path.length() && path.charAt(i) != '.' && path.charAt(i) != '[') {
        i++;
      }
      String attr = getKeyName(path.substring(1, i), attrNames);
      AttributeValue childValue =
          extractProjectionValue(fullValue.getM().get(attr), path.substring(i), attrNames);
      AttributeValue newValue = new AttributeValue();
      newValue.setM(new HashMap<>(Collections.singletonMap(attr, childValue)));
      return newValue;
    } else {
      // array structure
      Preconditions.checkArgument(path.charAt(0) == '[', "Invalid projection expression: " + path);
      int num = 0;
      int i = 1;
      while (i < path.length() && path.charAt(i) != ']') {
        char ch = path.charAt(i);
        Preconditions.checkArgument(ch >= '0' && ch <= '9', "Array offset must be number: " + path);
        num *= 10;
        num += path.charAt(i) - '0';
        i++;
      }
      Preconditions.checkArgument(
          path.charAt(i) == ']', "Invalid array offset expression: " + path);
      List<AttributeValue> list = fullValue.getL();
      if (list != null && num < list.size()) {
        AttributeValue childValue =
            extractProjectionValue(list.get(num), path.substring(i + 1), attrNames);
        // we put the extracted value in its original position, and set
        // other positions of the array to null, so that we don't break
        // the relative positions needed in the merge step
        AttributeValue newValue = new AttributeValue();
        List<AttributeValue> newList = new ArrayList<>(list.size());
        for (int k = 0, end = list.size(); k < end; k++) {
          newList.add(k == num ? childValue : null);
        }
        newValue.setL(newList);
        return newValue;
      }
    }
    logger.debug(
        "Invalid projection path {} for value {}, ignore by setting the value to null",
        path,
        fullValue);
    return null;
  }
}
