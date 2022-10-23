package io.stargate.sgv2.dynamoapi.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import io.stargate.bridge.grpc.Values;
import io.stargate.bridge.proto.QueryOuterClass;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class FromProtoConverterTest {
  private static final String TEST_COLUMN = "test_column";

  private static final FromProtoValueCodecs FROM_PROTO_VALUE_CODECS = new FromProtoValueCodecs();

  private static Arguments[] fromExternalSamples() {
    return new Arguments[] {
      arguments(
          new AttributeValue().withN("123"),
          basicType(QueryOuterClass.TypeSpec.Basic.INT),
          Values.of(123)),
      arguments(
          new AttributeValue().withN("-4567"),
          basicType(QueryOuterClass.TypeSpec.Basic.BIGINT),
          Values.of(-4567L)),
      arguments(
          new AttributeValue().withS("abc"),
          basicType(QueryOuterClass.TypeSpec.Basic.VARCHAR),
          Values.of("abc")),
    };
  }

  @ParameterizedTest
  @MethodSource("fromExternalSamples")
  @DisplayName("Should converted Bridge/gRPC value into expected external representation")
  public void strictExternalToBridgeValueTest(
      Object externalValue, QueryOuterClass.TypeSpec typeSpec, QueryOuterClass.Value bridgeValue) {
    FromProtoConverter conv = createConverter(typeSpec);
    Map<String, AttributeValue> result = conv.mapFromProtoValues(Arrays.asList(bridgeValue));

    assertThat(result.get(TEST_COLUMN)).isEqualTo(externalValue);
  }

  /*
  ///////////////////////////////////////////////////////////////////////
  // Helper methods for constructing scaffolding for Bridge/gRPC
  ///////////////////////////////////////////////////////////////////////
   */

  private static Set<Object> setOf(Object... values) {
    LinkedHashSet<Object> set = new LinkedHashSet<>();
    set.addAll(Arrays.asList(values));
    return set;
  }

  private static FromProtoConverter createConverter(QueryOuterClass.TypeSpec typeSpec) {
    QueryOuterClass.ColumnSpec column =
        QueryOuterClass.ColumnSpec.newBuilder().setName(TEST_COLUMN).setType(typeSpec).build();
    FromProtoValueCodec codec = FROM_PROTO_VALUE_CODECS.codecFor(column);
    return FromProtoConverter.construct(
        new String[] {TEST_COLUMN}, new FromProtoValueCodec[] {codec});
  }

  private static QueryOuterClass.TypeSpec basicType(QueryOuterClass.TypeSpec.Basic basicType) {
    return QueryOuterClass.TypeSpec.newBuilder().setBasic(basicType).build();
  }

  private static QueryOuterClass.TypeSpec listType(
      QueryOuterClass.TypeSpec.Basic basicElementType) {
    return listType(basicType(basicElementType));
  }

  private static QueryOuterClass.TypeSpec listType(QueryOuterClass.TypeSpec elementType) {
    return QueryOuterClass.TypeSpec.newBuilder()
        .setList(QueryOuterClass.TypeSpec.List.newBuilder().setElement(elementType).build())
        .build();
  }

  private static QueryOuterClass.TypeSpec setType(QueryOuterClass.TypeSpec.Basic basicElementType) {
    return setType(basicType(basicElementType));
  }

  private static QueryOuterClass.TypeSpec setType(QueryOuterClass.TypeSpec elementType) {
    return QueryOuterClass.TypeSpec.newBuilder()
        .setSet(QueryOuterClass.TypeSpec.Set.newBuilder().setElement(elementType).build())
        .build();
  }

  private static QueryOuterClass.TypeSpec mapType(
      QueryOuterClass.TypeSpec.Basic basicKeyType, QueryOuterClass.TypeSpec.Basic basicValueType) {
    return mapType(basicType(basicKeyType), basicType(basicValueType));
  }

  private static QueryOuterClass.TypeSpec mapType(
      QueryOuterClass.TypeSpec keyType, QueryOuterClass.TypeSpec valueType) {
    return QueryOuterClass.TypeSpec.newBuilder()
        .setMap(
            QueryOuterClass.TypeSpec.Map.newBuilder().setKey(keyType).setValue(valueType).build())
        .build();
  }
}
