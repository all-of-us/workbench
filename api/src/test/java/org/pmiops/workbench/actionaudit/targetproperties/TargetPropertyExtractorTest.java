package org.pmiops.workbench.actionaudit.targetproperties;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.model.User;
import org.pmiops.workbench.model.Workspace;

class TargetPropertyExtractorTest {
  private Workspace workspace;

  @BeforeEach
  void setUp() {
    long now = System.currentTimeMillis();
    User creator = new User();
    creator.setEmail("user@fake-research-aou.org");

    var researchPurpose1 =
        new ResearchPurpose()
            .intendedStudy("stubbed toes")
            .additionalNotes("I really like the cloud.")
            .approved(true)
            .ancestry(true)
            .anticipatedFindings("I expect to find a lot of stubbed toes.")
            .commercialPurpose(true)
            .controlSet(true)
            .diseaseFocusedResearch(true)
            .diseaseOfFocus("toes")
            .drugDevelopment(true)
            .educational(true)
            .intendedStudy("stubbed toes")
            .methodsDevelopment(true)
            .otherPopulationDetails("Something about toes I guess")
            .populationDetails(
                List.of(
                    SpecificPopulationEnum.DISABILITY_STATUS,
                    SpecificPopulationEnum.SEXUAL_ORIENTATION,
                    SpecificPopulationEnum.SEXUAL_ORIENTATION))
            .populationHealth(true)
            .reasonForAllOfUs("I like it.")
            .reviewRequested(true)
            .socialBehavioral(true)
            .timeRequested(now)
            .timeReviewed(now);

    workspace =
        new Workspace()
            .etag("etag_1")
            .name("DbWorkspace 1")
            .terraName("dbworkspace1")
            .namespace("aou-rw-local1-c4be869a")
            .cdrVersionId("1")
            .creator(creator)
            .accessTierShortName(AccessTierService.REGISTERED_TIER_SHORT_NAME)
            .researchPurpose(researchPurpose1);
  }

  @Test
  void testGetsWorkspaceProperties() {
    var propertyValuesByName =
        ModelBackedTargetProperty.getPropertyValuesByName(
            WorkspaceTargetProperty.values(), workspace);
    assertThat(propertyValuesByName.get(WorkspaceTargetProperty.NAME.getPropertyName()))
        .isEqualTo("DbWorkspace 1");
    assertThat(propertyValuesByName).hasSize(WorkspaceTargetProperty.values().length);
  }
}
