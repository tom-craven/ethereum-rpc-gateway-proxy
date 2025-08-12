package io.tatum.gateway.domain;

import io.tatum.gateway.domain.error.InvalidRequestException;
import io.tatum.gateway.domain.error.ProxyServerException;
import io.tatum.gateway.domain.model.Client;
import io.tatum.gateway.domain.model.RequestUtils;
import io.tatum.gateway.infrastructure.EthereumWebClient;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.ext.web.RoutingContext;

import static io.tatum.gateway.domain.ClientMetricsHandler.incrementInvocations;
import static io.tatum.gateway.domain.ClientMetricsHandler.logRequest;
import static io.tatum.gateway.domain.error.ErrorUtils.createError;

public class JsonRPCProxyHandler {
  private final EthereumWebClient ethereumWebClient;

  public JsonRPCProxyHandler(EthereumWebClient ethereumWebClient) {
    this.ethereumWebClient = ethereumWebClient;
  }

  /**
   * @implNote Returns either a single JsonObject or JsonArray response size.
   * This mimics the behavior of the ethereum endpoint, however I have
   * some doubt, obfuscation of functionality is confusing to the
   * client and complicates ApiSchema Definition. Perhaps im wrong.
   */
  private static void handleSuccess(RoutingContext routingContext, JsonArray proxyResponse) {
    if (proxyResponse.size() == 1) {
      routingContext.response().setStatusCode(200)
        .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
        .end(proxyResponse.getJsonObject(0).encode());
    } else {
      routingContext.response().setStatusCode(200).putHeader("Content-Type", "application/json")
        .end(proxyResponse.encode());
    }
  }

  /**
   * @implNote handle basic error scenarios, also need to handle timeouts, rate
   * limits
   */
  public static void handleError(Throwable err, RoutingContext RoutingContext) {
    int statusCode;
    JsonObject errorJson;

    if (err instanceof InvalidRequestException) {
      statusCode = 400;
      errorJson = createError(err, -32700, null);
    } else if (err instanceof ProxyServerException) {
      statusCode = 502;
      errorJson = createError(err, -32000, null);
    } else {
      statusCode = 500;
      errorJson = createError(err, -32000, null);
    }
    RoutingContext.response().putHeader("Content-Type", "application/json").setStatusCode(statusCode)
      .end(errorJson.encode());
  }

  /**
   * @implNote FR#1 Implement a service that receives JSON-RPC 2.0 requests,
   * forwards them to a designated Ethereum node endpoint, and returns
   * the responses to the client.
   */
  public void handle(RoutingContext routingContext) {
    var client = (Client) routingContext.get("client");
    routingContext.request()
      .rxBody()
      .flatMap(RequestUtils::marshallJson)
      .doOnEvent((request, throwable) -> incrementInvocations(request, client))
      .doOnEvent((request, throwable) -> logRequest(request, routingContext, client))
      .flatMap(request -> ethereumWebClient.forwardRequest(request, client))
      .subscribe(response -> handleSuccess(routingContext, response),
        throwable -> handleError(throwable, routingContext));
  }
}
