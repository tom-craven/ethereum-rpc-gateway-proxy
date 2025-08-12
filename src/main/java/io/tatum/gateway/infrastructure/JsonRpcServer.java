package io.tatum.gateway.infrastructure;

import io.tatum.gateway.domain.ClientMetricsHandler;
import io.tatum.gateway.domain.JsonRPCProxyHandler;
import io.tatum.gateway.domain.SecurityPreHandler;
import io.tatum.gateway.domain.model.Client;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.rxjava3.core.AbstractVerticle;
import io.vertx.rxjava3.ext.web.Router;

import java.util.concurrent.ConcurrentHashMap;

public class JsonRpcServer extends AbstractVerticle {

  public JsonRpcServer() {
  }

  /**
   * @implNote FR#2 Secure incoming client connections using TLS. keystore should
   * be kept in secure store and pulled in on startup not stored in
   * gitHub!
   */
  private static HttpServerOptions getServerOptions() {
    return new HttpServerOptions().setSsl(true)
      // todo: reuse TCP connections to reduce overhead
      // .setReuseAddress(true)
      // .setReusePort(true)
      .setKeyCertOptions(new JksOptions().setPath("keystore.jks") // Path to keystore
        .setPassword("mypassword"));
  }

  @Override
  public void start() {
    // todo: get properties from config files
    final String ethUrl = System.getenv().get("ETH_URL") != null ? System.getenv().get("ETH_URL") : "http://localhost:8443";
    // todo: clientMap should be persisted and the api-key masked. We do not want expose api keys in memory!
    final ConcurrentHashMap<String, Client> clientMap = new ConcurrentHashMap<>();

    SecurityPreHandler securityPreHandler = new SecurityPreHandler(clientMap);
    ClientMetricsHandler clientMetricsHandler = new ClientMetricsHandler(clientMap);
    JsonRPCProxyHandler jsonRPCProxyHandler = new JsonRPCProxyHandler(new EthereumWebClient(vertx, ethUrl));

    Router router = Router.router(vertx);

    router.post("/rpc")
      // todo: implement a rate limiter using redis or it wont scale
      // .handler(MyRateLimiter())
      .handler(securityPreHandler::handle)
      .handler(jsonRPCProxyHandler::handle);

    // todo: consider eventing the metrics and decoupling the handler from the RPC server.
    router.get("/clients/invocations").handler(clientMetricsHandler::handle);

    vertx.createHttpServer(getServerOptions()).requestHandler(router).rxListen(443).subscribe(
      httpServer -> System.out.println("[OK - HTTP server listening on port 443]"),
      throwable -> System.out.println("[ERROR - HTTP server exception: " + throwable.getMessage() + " ]"));
  }
}
