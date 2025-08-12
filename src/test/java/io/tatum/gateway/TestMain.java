package io.tatum.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import io.tatum.gateway.infrastructure.JsonRpcServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.rxjava3.core.Vertx;
import io.vertx.rxjava3.ext.web.client.WebClient;
import io.vertx.rxjava3.ext.web.codec.BodyCodec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(VertxExtension.class)
public class TestMain {

  private Vertx vertx;
  private WireMockServer wireMockServer;
  private WebClient client;

  @BeforeEach
  void setUp(VertxTestContext context) {
    vertx = Vertx.vertx();

    // Initialize WireMock server with HTTP
    wireMockServer = new WireMockServer(wireMockConfig()
      .port(8443)
    );
    wireMockServer.start();

    // Initialize Vert.x WebClient with trustAll for testing
    client = WebClient.create(vertx, new WebClientOptions()
      .setSsl(true)
      .setTrustAll(true)
      .setVerifyHost(false)
    );

    // Deploy the verticle
    vertx.rxDeployVerticle(new JsonRpcServer())
      .subscribe(
        id -> context.completeNow(),
        context::failNow
      );
  }

  @AfterEach
  void tearDown(VertxTestContext context) {
    wireMockServer.stop();
    vertx.rxClose()
      .subscribe(
        context::completeNow,
        context::failNow
      );
  }

  @Test
  public void givenSingleRequestThenReturnSuccess(VertxTestContext context) {

    JsonObject ethereumResponse = new JsonObject()
      .put("jsonrpc", "2.0")
      .put("result", "success")
      .put("id", 83)
      .put("result", "0x160d9f3");

    stubWireMock(ethereumResponse);

    JsonObject request = new JsonObject()
      .put("jsonrpc", "2.0")
      .put("method", "eth_blockNumber")
      .put("params", new String[]{})
      .put("id", 83);

    client.post(443, "localhost", "/rpc")
      .putHeader("Content-Type", "application/json")
      .putHeader("x-api-key", "123")
      .as(BodyCodec.jsonObject())
      .rxSendJsonObject(request)
      .subscribe(
        response -> context.verify(() -> {
          assertEquals(200, response.statusCode());
          assertEquals(ethereumResponse.encode(), response.body().encode());
          System.out.println("[SUCCESS - Test received response: " + response.body().toString() + " ]");
          context.completeNow();
        }),
        context::failNow
      );

  }

  private StubMapping stubWireMock(JsonObject ethereumResponse) {
    return wireMockServer.stubFor(
      WireMock.post("/")
        .willReturn(WireMock.aResponse()
          .withStatus(200)
          .withHeader("Content-Type", "application/json")
          .withBody(JsonArray.of(ethereumResponse).encode()))
    );
  }
  // given batch request

  // given malformed request

  // given missing api key

  // given proxy error

}
