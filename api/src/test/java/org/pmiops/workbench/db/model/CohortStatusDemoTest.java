package org.pmiops.workbench.db.model;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.model.CohortStatus;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class CohortStatusDemoTest {

  @Test
  public void testGivesCorrectStorageAndClientEnum() {
    for (CohortStatusDemo cohortStatusDemo : CohortStatusDemo.values()) {
      final short storage = cohortStatusDemo.toStorage();
      final CohortStatus clientEnum = cohortStatusDemo.toClientCohortStatus();
      assertThat(StorageEnums.cohortStatusFromStorage(storage)).isEqualTo(clientEnum);
      assertThat(CohortStatus.fromValue(cohortStatusDemo.name())).isEqualTo(clientEnum);
    }
  }

  @Test
  public void testFromClientCohortStatus() {
    for (CohortStatus clientValue : CohortStatus.values()) {
      final CohortStatusDemo translated = CohortStatusDemo.fromClientCohortStatus(clientValue);
      assertThat(translated.toClientCohortStatus()).isEqualTo(clientValue);
    }
  }

  @Test
  public void testFromStorage() {
    ImmutableList<Integer> storageIntValues = ImmutableList.of(0, 1, 2, 3);
    for (int ordinal : storageIntValues) {
      final short storage = (short) ordinal;
      final CohortStatusDemo translated = CohortStatusDemo.fromStorage(storage);
      assertThat(translated.toStorage()).isEqualTo(storage);
      assertThat(translated.toClientCohortStatus()).isEqualTo(StorageEnums.cohortStatusFromStorage(storage));
    }
  }

  @Test
  public void testIsHappy() {
    assertThat(CohortStatusDemo.EXCLUDED.isHappy()).isFalse();
    assertThat(CohortStatusDemo. INCLUDED.isHappy()).isTrue();
  }

  @Test
  public void testDescription() {
    assertThat(CohortStatusDemo.NEEDS_FURTHER_REVIEW.getDescription()).isEqualTo("One of these days");
  }
}
