package org.pmiops.workbench.actionaudit.targetproperties;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.access.AccessTierService;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.model.Workspace;

public class WorkspaceTargetPropertyTest {
  private Workspace workspace1;
  private Workspace workspace2;

  @BeforeEach
  public void setUp() {
    ResearchPurpose researchPurposeAllFieldsPopulated =
        new ResearchPurpose()
            .diseaseOfFocus("Chicken Pox")
            .otherPopulationDetails("cool people")
            .populationDetails(List.of(SpecificPopulationEnum.ACCESS_TO_CARE))
            .reasonForAllOfUs("a triple dog dare")
            .intendedStudy("stubbed toes")
            .additionalNotes("I really like the cloud.")
            .timeRequested(101L)
            .timeReviewed(111L)
            .anticipatedFindings("cool stuff")
            .approved(true);

    ResearchPurpose researchPurpose2 =
        new ResearchPurpose()
            .intendedStudy("broken dreams")
            .additionalNotes("I changed my mind")
            .anticipatedFindings("a 4-leaf clover");

    long now = System.currentTimeMillis();

    workspace1 =
        new Workspace()
            .name("Workspace 1")
            .id("fc-id-1")
            .namespace("aou-rw-local1-c4be869a")
            .creator("user@fake-research-aou.org")
            .cdrVersionId("1")
            .researchPurpose(researchPurposeAllFieldsPopulated)
            .creationTime(now)
            .lastModifiedTime(now)
            .etag("etag_1")
            .accessTierShortName(AccessTierService.REGISTERED_TIER_SHORT_NAME)
            .published(false);

    workspace2 =
        new Workspace()
            .name("Workspace 2")
            .id("fc-id-1")
            .namespace("aou-rw-local1-c4be869a")
            .creator("user@fake-research-aou.org")
            .cdrVersionId("33")
            .researchPurpose(researchPurpose2)
            .creationTime(now)
            .lastModifiedTime(now)
            .etag("etag_1")
            .accessTierShortName(AccessTierService.REGISTERED_TIER_SHORT_NAME)
            .published(false);
  }

  @Test
  public void testExtractsStringPropertiesFromWorkspace() {
    Map<String, String> propertiesByName =
        TargetPropertyExtractor.getPropertyValuesByName(
            WorkspaceTargetProperty.values(), workspace1);

    assertEquals(WorkspaceTargetProperty.values().length, propertiesByName.size());
    assertEquals(
        "stubbed toes",
        propertiesByName.get(WorkspaceTargetProperty.INTENDED_STUDY.getPropertyName()));
    assertEquals(
        "1", propertiesByName.get(WorkspaceTargetProperty.CDR_VERSION_ID.getPropertyName()));
    assertEquals(
        "a triple dog dare",
        propertiesByName.get(WorkspaceTargetProperty.REASON_FOR_ALL_OF_US.getPropertyName()));
  }

  @Test
  public void testMapsChanges() {
    var workspace1Properties =
        TargetPropertyExtractor.getPropertyValuesByName(
            WorkspaceTargetProperty.values(), workspace1);
    var workspace2Properties =
        TargetPropertyExtractor.getPropertyValuesByName(
            WorkspaceTargetProperty.values(), workspace2);

    Map<String, PreviousNewValuePair> changesByPropertyName =
        TargetPropertyExtractor.getChangedValuesByName(
            WorkspaceTargetProperty.values(), workspace1, workspace2);

    assertFalse(changesByPropertyName.isEmpty());

    var allKeys = Sets.union(workspace1Properties.keySet(), workspace2Properties.keySet());
    var matchingEntries =
        Sets.intersection(workspace1Properties.entrySet(), workspace2Properties.entrySet());
    assertThat(changesByPropertyName).hasSize(allKeys.size() - matchingEntries.size());

    assertEquals(
        "I really like the cloud.",
        changesByPropertyName
            .get(WorkspaceTargetProperty.ADDITIONAL_NOTES.getPropertyName())
            .getPreviousValue());
    assertEquals(
        "I changed my mind",
        changesByPropertyName
            .get(WorkspaceTargetProperty.ADDITIONAL_NOTES.getPropertyName())
            .getNewValue());
  }

  @Test
  public void testHandlesMissingValues() {
    workspace2.setCdrVersionId(null);
    Map<String, PreviousNewValuePair> changesByName =
        TargetPropertyExtractor.getChangedValuesByName(
            WorkspaceTargetProperty.values(), workspace1, workspace2);

    assertEquals(
        "1",
        changesByName
            .get(WorkspaceTargetProperty.CDR_VERSION_ID.getPropertyName())
            .getPreviousValue());
    assertNull(
        changesByName.get(WorkspaceTargetProperty.CDR_VERSION_ID.getPropertyName()).getNewValue());

    Map<String, PreviousNewValuePair> reverseChangesByName =
        TargetPropertyExtractor.getChangedValuesByName(
            WorkspaceTargetProperty.values(), workspace2, workspace1);

    assertNull(
        reverseChangesByName
            .get(WorkspaceTargetProperty.CDR_VERSION_ID.getPropertyName())
            .getPreviousValue());
    assertEquals(
        "1",
        reverseChangesByName
            .get(WorkspaceTargetProperty.CDR_VERSION_ID.getPropertyName())
            .getNewValue());
  }

  @Test
  public void testComparisonToSelfIsEmpty() {
    assertTrue(
        TargetPropertyExtractor.getChangedValuesByName(
                WorkspaceTargetProperty.values(), workspace1, workspace1)
            .isEmpty());
  }
}
