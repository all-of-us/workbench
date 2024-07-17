package org.pmiops.workbench.actionaudit.targetproperties;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.Workspace;

class TargetPropertyExtractorTest {
  private Workspace workspace;

  @BeforeEach
  void setUp() {
    var researchPurpose1 =
        new ResearchPurpose()
            .intendedStudy("stubbed toes")
            .additionalNotes("I really like the cloud.");

    long now = System.currentTimeMillis();

    workspace =
        new Workspace()
            .name("DbWorkspace 1")
            .terraName("fc-id-1")
            .namespace("aou-rw-local1-c4be869a")
            .creator("user@fake-research-aou.org")
            .cdrVersionId("1")
            .researchPurpose(researchPurpose1)
            .creationTime(now)
            .lastModifiedTime(now)
            .etag("etag_1")
            .accessTierShortName(AccessTierService.REGISTERED_TIER_SHORT_NAME)
            .published(false);
  }

  @Test
  void testGetsWorkspaceProperties() {
    var propertyValuesByName =
        ModelBackedTargetProperty.getPropertyValuesByName(
            WorkspaceTargetProperty.values(), workspace);
    assertThat(propertyValuesByName.get(WorkspaceTargetProperty.NAME.getPropertyName()))
        .isEqualTo("DbWorkspace 1");
    assertThat(propertyValuesByName).hasSize(19);
  }
}
