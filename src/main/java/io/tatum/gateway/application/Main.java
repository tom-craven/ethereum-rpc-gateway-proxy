package io.tatum.gateway.application;

import io.tatum.gateway.infrastructure.JsonRpcServer;
import io.vertx.core.VertxOptions;
import io.vertx.rxjava3.core.Vertx;

public class Main {

  public static void main(String[] args) {
    //todo: under PT find a suitable event pool size for prod, default is 2x availableProcessors
    Vertx vertx = Vertx
      .vertx(new VertxOptions().setEventLoopPoolSize(Runtime.getRuntime().availableProcessors()));
    vertx.rxDeployVerticle(new JsonRpcServer())
      .subscribe(id -> System.out.println("[OK - Vertical deployed: " + id + " ]"),
        err -> System.err.println("[ERROR -Failed to deploy vertical: " + err.getMessage() + " ]"));
  }
}
