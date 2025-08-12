package io.tatum.gateway.domain.error;

public class AuthorizationException extends RuntimeException {
  public AuthorizationException(String message) {
    super(message);
  }
}
