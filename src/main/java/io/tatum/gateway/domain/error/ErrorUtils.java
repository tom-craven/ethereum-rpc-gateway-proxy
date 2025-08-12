package io.tatum.gateway.domain.error;

import io.vertx.core.json.JsonObject;

public class ErrorUtils {

  public static JsonObject createError(Throwable err, int value, Object id) {
    return new JsonObject().put("jsonrpc", "2.0")
      .put("error", new JsonObject().put("code", value).put("message", "Error: " + err.getMessage()))
      .put("id", id);
  }
}
