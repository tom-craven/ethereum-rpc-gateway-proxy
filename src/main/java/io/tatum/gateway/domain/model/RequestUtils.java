package io.tatum.gateway.domain.model;

import io.reactivex.rxjava3.core.Single;
import io.tatum.gateway.domain.error.InvalidRequestException;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class RequestUtils {

  /**
   * @implNote marshal request to JSON and validate RPC 2.0 format
   * ## todo: consider using a schema model and OpenApi
   */
  public static Single<JsonArray> marshallJson(Buffer buffer) {
    Object json = buffer.toJsonValue();
    if (json instanceof JsonObject) {
      if (isInvalidRequest((JsonObject) json)) {
        return Single.error(new InvalidRequestException("Not a valid RPC 2.0 format"));
      }
      return Single.just(new JsonArray().add(json));
    } else if (json instanceof JsonArray) {
      if (((JsonArray) json).size() > 99) {
        // todo: what is a reasonable limit for batch requests?
        return Single.error(new InvalidRequestException("Batch requests cannot exceed 99"));
      }
      for (Object item : (JsonArray) ((JsonArray) json).iterator()) {
        if (isInvalidRequest((JsonObject) item)) {
          return Single.error(new InvalidRequestException("Not a valid RPC 2.0 format"));
        }
      }
      return Single.just((JsonArray) json);
    } else {
      return Single.error(new InvalidRequestException("Not a valid json object"));
    }
  }

  // todo: consider limiting the allowed methods and returning -32601 Method not found
  private static boolean isInvalidRequest(JsonObject request) {
    return !request.containsKey("jsonrpc") || !request.getString("jsonrpc").equals("2.0")
      || !request.containsKey("method") || request.getString("method") == null || !request.containsKey("id");
  }
}
