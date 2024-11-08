package org.pmiops.workbench.initialcredits;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.mockito.ArgumentMatcher;

public class InitialCreditsExpiryTaskMatchers {

  static class UserListMatcher implements ArgumentMatcher<List<Long>> {
    private final List<Long> expectedUserIds;

    public UserListMatcher(List<Long> expectedUserIds) {
      this.expectedUserIds = expectedUserIds;
    }

    @Override
    public boolean matches(List<Long> userIds) {
      Set<Long> expectedUserIdsSet = new HashSet<>(expectedUserIds);
      Set<Long> userIdsSet = new HashSet<>(userIds);
      return expectedUserIdsSet.equals(userIdsSet);
    }
  }

  static class MapMatcher implements ArgumentMatcher<Map<Long, Double>> {

    private final Map<Long, Double> expectedMap;

    public MapMatcher(Map<Long, Double> expectedMap) {
      this.expectedMap = expectedMap;
    }

    @Override
    public boolean matches(Map<Long, Double> argument) {
      return expectedMap.equals(argument);
    }
  }
}
