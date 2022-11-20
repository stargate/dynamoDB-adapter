package io.stargate.sgv2.dynamoapi;

import io.stargate.sgv2.api.common.config.constants.HttpConstants;
import io.stargate.sgv2.dynamoapi.constants.DynamoOpenApiConstants;
import io.stargate.sgv2.dynamoapi.exception.DynamoError;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeIn;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

@OpenAPIDefinition(
    // note that info is defined via the properties
    info = @Info(title = "", version = ""),
    components =
        @Components(

            // security schemes
            securitySchemes = {
              @SecurityScheme(
                  securitySchemeName = DynamoOpenApiConstants.SecuritySchemes.TOKEN,
                  type = SecuritySchemeType.APIKEY,
                  in = SecuritySchemeIn.HEADER,
                  apiKeyName = HttpConstants.AUTHENTICATION_TOKEN_HEADER_NAME)
            },
            // reusable examples
            examples = {
              @ExampleObject(
                  name = DynamoOpenApiConstants.Examples.GENERAL_BAD_REQUEST,
                  value =
                      """
                        {
                            "code": 400,
                            "description": "Request invalid: payload not provided."
                        }
                        """),
              @ExampleObject(
                  name = DynamoOpenApiConstants.Examples.GENERAL_UNAUTHORIZED,
                  value =
                      """
                        {
                            "code": 401,
                            "description": "Unauthorized operation.",
                            "grpcStatus": "PERMISSION_DENIED"
                        }
                        """),
              @ExampleObject(
                  name = DynamoOpenApiConstants.Examples.GENERAL_SERVER_SIDE_ERROR,
                  value =
                      """
                        {
                            "code": 500,
                            "description": "Internal server error."
                        }
                        """),
            },

            // reusable response
            responses = {
              @APIResponse(
                  name = DynamoOpenApiConstants.Responses.GENERAL_204,
                  responseCode = "204",
                  description = "No content"),
              @APIResponse(
                  name = DynamoOpenApiConstants.Responses.GENERAL_400,
                  responseCode = "400",
                  description = "Bad request",
                  content =
                      @Content(
                          mediaType = MediaType.APPLICATION_JSON,
                          examples = {
                            @ExampleObject(
                                ref = DynamoOpenApiConstants.Examples.GENERAL_BAD_REQUEST),
                          },
                          schema = @Schema(implementation = DynamoError.class))),
              @APIResponse(
                  name = DynamoOpenApiConstants.Responses.GENERAL_401,
                  responseCode = "401",
                  description = "Unauthorized",
                  content =
                      @Content(
                          mediaType = MediaType.APPLICATION_JSON,
                          examples = {
                            @ExampleObject(
                                ref = DynamoOpenApiConstants.Examples.GENERAL_UNAUTHORIZED),
                          },
                          schema = @Schema(implementation = DynamoError.class))),
              @APIResponse(
                  name = DynamoOpenApiConstants.Responses.GENERAL_404,
                  responseCode = "404",
                  description = "Not Found",
                  content =
                      @Content(
                          mediaType = MediaType.APPLICATION_JSON,
                          schema = @Schema(implementation = DynamoError.class))),
              @APIResponse(
                  name = DynamoOpenApiConstants.Responses.GENERAL_500,
                  responseCode = "500",
                  description = "Internal server error",
                  content =
                      @Content(
                          mediaType = MediaType.APPLICATION_JSON,
                          examples = {
                            @ExampleObject(
                                ref = DynamoOpenApiConstants.Examples.GENERAL_SERVER_SIDE_ERROR),
                          },
                          schema = @Schema(implementation = DynamoError.class))),
            }))
public class StargateDynamoApi extends Application {}
