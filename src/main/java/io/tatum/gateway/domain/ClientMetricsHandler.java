package io.tatum.gateway.domain;

import io.tatum.gateway.domain.model.Client;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava3.ext.web.RoutingContext;

import java.util.concurrent.ConcurrentHashMap;

public class ClientMetricsHandler {

  private final ConcurrentHashMap<String, Client> clientKeyMap;

  public ClientMetricsHandler(ConcurrentHashMap<String, Client> clientKeyMap) {
    this.clientKeyMap = clientKeyMap;
  }

  /**
   * @implNote FR#4 Maintain a count of invocations for each distinct JSON-RPC
   * method.
   * todo: needs to be in Redis or event to aggregate elsewhere or won't scale
   */
  public static void incrementInvocations(JsonArray objects, Client client) {
    objects.forEach(object -> {
      var json = (JsonObject) object;
      client.incrementInvocations(json.getString("method"));
    });
  }

  /**
   * @implNote FR#3 Log basic information about incoming requests
   * todo: consider use of proper MDC context and logger like SLF4J
   */
  public static void logRequest(JsonArray request, RoutingContext routingContext, Client client) {
    var correlationId = routingContext.get("correlation-id");
    var requestMethod = routingContext.request().method();
    var requestUri = routingContext.request().uri();
    var message = "[OK - client: " + client.id() + " correlation-id: " + correlationId + " " + requestMethod + " " + requestUri + " body: " + request + " ]";
    System.out.println(message);
  }

  public void handle(RoutingContext routingContext) {
    final JsonArray response = new JsonArray();
    clientKeyMap.values()
      .forEach(client -> {
        JsonObject clientNode = new JsonObject();
        clientNode.put("client-id", client.id());
        client.getInvocations().stream().map(stringIntegerEntry -> new JsonObject()
            .put("method", stringIntegerEntry.getKey())
            .put("count", stringIntegerEntry.getValue())
          )
          .forEach(entries -> clientNode.put("invocations", entries));
        response.add(clientNode);
      });

    routingContext.response().setStatusCode(200).putHeader("Content-Type", "application/json")
      .end(response.encode());
  }
}
