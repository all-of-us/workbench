package org.pmiops.workbench.access.policies;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.access.modules.AccessModuleKey;
import org.pmiops.workbench.access.modules.AccessModuleService;
import org.pmiops.workbench.access.modules.AccessScore;
import org.pmiops.workbench.access.modules.FakeAccessModule;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class AccessPolicyEvaluatorServiceTest {

  private static final AccessPolicy ACCESS_POLICY_1 =
      new AccessPolicy(ImmutableSet.of(AccessModuleKey.BETA_ACCESS, AccessModuleKey.DUA_TRAINING));

  @Autowired AccessPolicyEvaluatorService accessPolicyEvaluatorService;

  @TestConfiguration
  @Import({AccessPolicyEvaluatorServiceImpl.class})
  public static class config {
    @Bean
    public Map<AccessModuleKey, AccessModuleService> getAccessModuleMap() {
      return ImmutableSet.of(
              buildEntry(AccessModuleKey.DUA_TRAINING, AccessScore.PASSED),
              buildEntry(AccessModuleKey.REQUEST_REGISTERED, AccessScore.PASSED),
              buildEntry(AccessModuleKey.BETA_ACCESS, AccessScore.FAILED))
          .stream()
          .collect(
              ImmutableMap.toImmutableMap(
                  SimpleImmutableEntry::getKey, SimpleImmutableEntry::getValue));
    }

    private SimpleImmutableEntry<AccessModuleKey, AccessModuleService> buildEntry(
        AccessModuleKey accessModuleKey, AccessScore accessScore) {
      return new SimpleImmutableEntry<>(
          accessModuleKey, new FakeAccessModule(accessModuleKey, accessScore));
    }
  }

  @Test
  public void testPassingPolicyWorks() {
    final DbUser user = mock(DbUser.class);

    final Map<AccessModuleKey, AccessScore> results =
        accessPolicyEvaluatorService.evaluateUser(ACCESS_POLICY_1, user);
    assertThat(results).hasSize(2);
    assertThat(results.get(AccessModuleKey.DUA_TRAINING)).isEqualTo(AccessScore.PASSED);
    assertThat(results.get(AccessModuleKey.BETA_ACCESS)).isEqualTo(AccessScore.FAILED);
  }
}
