package io.stargate.sgv2.dynamoapi.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.smallrye.mutiny.Uni;
import io.stargate.sgv2.api.common.security.HeaderAuthenticationRequest;
import io.stargate.sgv2.api.common.security.HeaderBasedAuthenticationMechanism;
import io.vertx.ext.web.RoutingContext;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamoAuthenticationMechanism extends HeaderBasedAuthenticationMechanism {

  private static final Pattern CREDENTIAL_PATTERN =
      Pattern.compile("Credential=([^/]+)/[^/]+/[^/]+/dynamodb");
  private static final Logger LOG = LoggerFactory.getLogger(DynamoAuthenticationMechanism.class);

  /** The name of the header to be used for the authentication. */
  private final String headerName;

  /** Object mapper for custom response. */
  private final ObjectMapper objectMapper;

  public DynamoAuthenticationMechanism(String headerName, ObjectMapper objectMapper) {
    super(headerName, objectMapper);
    this.headerName = headerName;
    this.objectMapper = objectMapper;
  }

  @Override
  public Uni<SecurityIdentity> authenticate(
      RoutingContext context, IdentityProviderManager identityProviderManager) {
    String token = context.request().getHeader(headerName);
    if (token == null) {
      String credential = context.request().getHeader("Authorization");
      if (StringUtils.isNotEmpty(credential)) {
        Matcher m = CREDENTIAL_PATTERN.matcher(credential);
        if (m.find()) {
          // retrieve Stargate token from AWS-generated authorization string
          token = m.group(1);
        } else {
          // normal Stargate token
          token = credential;
        }
      }
    }
    if (null != token) {
      HeaderAuthenticationRequest request = new HeaderAuthenticationRequest(headerName, token);
      HttpSecurityUtils.setRoutingContextAttribute(request, context);
      return identityProviderManager.authenticate(request);
    }

    // No suitable header has been found in this request,
    return Uni.createFrom().optional(Optional.empty());
  }
}
