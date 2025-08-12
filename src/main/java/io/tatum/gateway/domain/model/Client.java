package io.tatum.gateway.domain.model;

import java.util.Map;
import java.util.Set;

public record Client(String id, String apiKey, Map<String, Integer> apiInvocations) {

  public void incrementInvocations(String method) {
    apiInvocations.merge(method, 1, Integer::sum);
  }

  public Set<Map.Entry<String, Integer>> getInvocations() {
    return apiInvocations.entrySet();
  }
}
