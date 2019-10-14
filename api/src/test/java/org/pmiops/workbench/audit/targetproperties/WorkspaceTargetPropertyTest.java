package org.pmiops.workbench.audit.targetproperties;

import static com.google.common.truth.Truth.assertThat;

import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pmiops.workbench.model.DataAccessLevel;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.Workspace;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class WorkspaceTargetPropertyTest {

  private Workspace workspace1;
  private Workspace workspace2;
  private Workspace emptyWorkspace;

  @Before
  public void setUp() {
    final ResearchPurpose researchPurpose1 = new ResearchPurpose();
    researchPurpose1.setIntendedStudy("stubbed toes");
    researchPurpose1.setAdditionalNotes("I really like the cloud.");

    final ResearchPurpose researchPurpose2 = new ResearchPurpose();
    researchPurpose2.setIntendedStudy("broken dreams");
    researchPurpose2.setAdditionalNotes("I changed my mind");
    researchPurpose2.setAnticipatedFindings("a 4-leaf clover");

    final long now = System.currentTimeMillis();

    workspace1 = new Workspace();
    workspace1.setName("Workspace 1");
    workspace1.setId("fc-id-1");
    workspace1.setNamespace("aou-rw-local1-c4be869a");
    workspace1.setCreator("user@fake-research-aou.org");
    workspace1.setCdrVersionId("1");
    workspace1.setResearchPurpose(researchPurpose1);
    workspace1.setCreationTime(now);
    workspace1.setLastModifiedTime(now);
    workspace1.setEtag("etag_1");
    workspace1.setDataAccessLevel(DataAccessLevel.REGISTERED);
    workspace1.setPublished(false);

    workspace2 = new Workspace();
    workspace2.setName("Workspace 2");
    workspace2.setId("fc-id-1");
    workspace2.setNamespace("aou-rw-local1-c4be869a");
    workspace2.setCreator("user@fake-research-aou.org");
    workspace2.setCdrVersionId("33");
    workspace2.setResearchPurpose(researchPurpose2);
    workspace2.setCreationTime(now);
    workspace2.setLastModifiedTime(now);
    workspace2.setEtag("etag_1");
    workspace2.setDataAccessLevel(DataAccessLevel.REGISTERED);
    workspace2.setPublished(false);

    emptyWorkspace = new Workspace();
    emptyWorkspace.setResearchPurpose(new ResearchPurpose());
  }



  @Test
  public void testExtractsStringPropertiesFromWorkspace() {
    Map<String, String> propertiesByName = WorkspaceTargetProperty.getPropertyValuesByName(workspace1);

    assertThat(propertiesByName).hasSize(6);
    assertThat(propertiesByName.get(WorkspaceTargetProperty.INTENDED_STUDY.getPropertyName()))
        .isEqualTo("stubbed toes");
    assertThat(propertiesByName.get(WorkspaceTargetProperty.CDR_VERSION_ID.getPropertyName()))
        .isEqualTo("1");
    assertThat(propertiesByName.get(WorkspaceTargetProperty.REASON_FOR_ALL_OF_US.getPropertyName()))
        .isNull();
  }

  @Test
  public void testEmptyWorkspaceGivesEmptyMap() {
    assertThat(WorkspaceTargetProperty.getPropertyValuesByName(emptyWorkspace))
        .isEmpty();
    assertThat(WorkspaceTargetProperty.getPropertyValuesByName(null))
        .isEmpty();
  }

  @Test
  public void testMapsChanges() {
    Map<String, PreviousNewValuePair> changesByPropertyName = WorkspaceTargetProperty
        .getChangedValuesByName(workspace1, workspace2);

    assertThat(changesByPropertyName).hasSize(5);

    assertThat(changesByPropertyName.get(WorkspaceTargetProperty.ADDITIONAL_NOTES.getPropertyName()).getPreviousValue())
        .isEqualTo("I really like the cloud.");

    assertThat(changesByPropertyName.get(WorkspaceTargetProperty.ADDITIONAL_NOTES.getPropertyName()).getNewValue())
        .isEqualTo("I changed my mind");
  }

  @Test
  public void testHandlesMissingValues() {
    workspace2.setCdrVersionId(null);
    Map<String, PreviousNewValuePair> changesByName = WorkspaceTargetProperty
        .getChangedValuesByName(workspace1, workspace2);
    assertThat(changesByName.get(WorkspaceTargetProperty.CDR_VERSION_ID.getPropertyName()).getPreviousValue())
        .isEqualTo("1");
    assertThat(changesByName.get(WorkspaceTargetProperty.CDR_VERSION_ID.getPropertyName()).getNewValue())
        .isNull();

    Map<String, PreviousNewValuePair> reverseChangesByName = WorkspaceTargetProperty
        .getChangedValuesByName(workspace2, workspace1);
    assertThat(reverseChangesByName.get(WorkspaceTargetProperty.CDR_VERSION_ID.getPropertyName()).getPreviousValue())
        .isNull();
    assertThat(reverseChangesByName.get(WorkspaceTargetProperty.CDR_VERSION_ID.getPropertyName()).getNewValue())
        .isEqualTo("1");
  }

  @Test
  public void testComparisonToSelfIsEmpty() {
    assertThat(WorkspaceTargetProperty.getChangedValuesByName(workspace1, workspace1)).isEmpty();
    assertThat(WorkspaceTargetProperty.getChangedValuesByName(null, null))
        .isEmpty();
  }

  @Test
  public void testComparisonToNullMatchesAllProperties() {
    assertThat(WorkspaceTargetProperty.getChangedValuesByName(workspace1, null))
        .hasSize(6);
  }
}
