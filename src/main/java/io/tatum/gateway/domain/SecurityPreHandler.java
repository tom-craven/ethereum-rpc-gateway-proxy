package io.tatum.gateway.domain;

import io.tatum.gateway.domain.error.AuthorizationException;
import io.tatum.gateway.domain.model.Client;
import io.vertx.rxjava3.ext.web.RoutingContext;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static io.tatum.gateway.domain.error.ErrorUtils.createError;

public class SecurityPreHandler {

  private final ConcurrentHashMap<String, Client> clientKeyMap;

  public SecurityPreHandler(ConcurrentHashMap<String, Client> clientKeyMap) {
    this.clientKeyMap = clientKeyMap;
  }

  // todo: does the key exist in our records, is it expired?
  private static boolean apiKeyIsValid(String apiKey) {
    return apiKey != null && !apiKey.trim().isEmpty();
  }

  /**
   * @implNote Get the client id for a given api key, if the client does not exist make a new instance
   * and add the client to the request context
   * todo: consider sanitizing inputs, they are evil!
   */
  public void handle(RoutingContext routingContext) {
    final String apiKey = routingContext.request().headers().get("x-api-key");
    if (!apiKeyIsValid(apiKey)) {
      routingContext.response().setStatusCode(400)
        .end(createError(new AuthorizationException("x-api-key header is required"), 1001, null).encode());
    } else {
      //todo: client-id api-key mappings should not be stored in the app or it will not scale.
      Client client = this.clientKeyMap.get(apiKey);
      if (client == null) {
        client = new Client(UUID.randomUUID().toString(), apiKey, new ConcurrentHashMap<>());
        this.clientKeyMap.put(apiKey, client);
      }
      routingContext.put("client", client);
      //todo: correlation id should be instantiated at client or its hard to track cause
      routingContext.put("correlation-id", "cor-" + UUID.randomUUID());
      routingContext.next();
    }
  }
}
