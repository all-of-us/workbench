package org.pmiops.workbench.access.policies;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.function.Function;
import org.pmiops.workbench.access.modules.AccessModuleKey;
import org.pmiops.workbench.access.modules.AccessScore;
import org.pmiops.workbench.db.model.DbUser;

public interface AccessPolicyEvaluatorService {
  /**
   * Ask each module to rate this user and return a map. TODO(jaycarlton): include human-readable
   * descriptions for error conditions
   *
   * @param dbUser user to be evalluated.
   * @return map from module key to score.
   */
  default Map<AccessModuleKey, AccessScore> evaluateUser(AccessPolicy accessPolicy, DbUser dbUser) {
    return accessPolicy.getAccessModuleKeys().stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Function.identity(), accessModuleKey -> scoreUser(accessModuleKey, dbUser)));
  }

  /** Call the access module associated with an individual key and obtain a score. */
  AccessScore scoreUser(AccessModuleKey key, DbUser dbUser);
}
