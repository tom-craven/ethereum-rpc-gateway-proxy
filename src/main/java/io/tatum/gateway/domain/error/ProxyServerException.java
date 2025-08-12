package io.tatum.gateway.domain.error;

public class ProxyServerException extends RuntimeException {

  public ProxyServerException(String message) {
    super(message);
  }
}
