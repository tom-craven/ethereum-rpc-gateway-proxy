package io.tatum.gateway.infrastructure;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Single;
import io.tatum.gateway.domain.error.ProxyServerException;
import io.tatum.gateway.domain.model.Client;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.ext.web.client.HttpResponse;
import io.vertx.rxjava3.ext.web.client.WebClient;
import io.vertx.rxjava3.ext.web.codec.BodyCodec;

public class EthereumWebClient {

  private final WebClient webClient;
  private final String ethUrl;

  public EthereumWebClient(Vertx vertx, String ethUrl) {
    this.ethUrl = ethUrl;
    if (ethUrl.contains("https:")) {
      this.webClient = WebClient.create(vertx, new WebClientOptions()
        // todo: set pool size for concurrent connections
        .setSsl(true)
        // For testing; use trust store in production
        .setTrustAll(true));
    } else {
      this.webClient = WebClient.create(vertx);
    }
  }

  public @NonNull Single<JsonArray> forwardRequest(JsonArray request, Client client) {
    // todo: consider caching common requests
    // todo: consider circuit breaker for any gateway services which could go down
    // todo: consider dynamic gateway selection based on response time, availability
    return webClient
      .postAbs(ethUrl)
      // todo: request should timeout if SLA is breached
      // .timeout(5000)
      .putHeader("x-api-key", client.apiKey())
      .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json").as(BodyCodec.jsonArray())
      .rxSendJson(request)
      // .retry( todo: retry to add robustness from intermittent network issues)
      .map(HttpResponse::body)
      .doOnError(throwable -> System.out
        .println("[ERROR - Proxy Server Exception: " + throwable.getMessage() + "]"))
      .onErrorResumeWith(Single.error(new ProxyServerException("Ethereum returned and exception")));
  }
}
