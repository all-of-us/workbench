package org.pmiops.workbench.zendesk;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.SpecificPopulation;
import org.pmiops.workbench.model.Workspace;

public class ZendeskRequestsTest {
  private DbUser user;

  @Before
  public void setUp() {
    user = new DbUser();
    user.setGivenName("Falco");
    user.setFamilyName("Lombardi");
    user.setUsername("falco@fake-research-aou.org");
    user.setContactEmail("user@gmail.com");
  }

  @Test
  public void testWorkspaceToReviewRequest_emptyResearchPurpose() {
    assertThat(
            ZendeskRequests.workspaceToReviewRequest(
                user, new Workspace().researchPurpose(new ResearchPurpose())))
        .isNotNull();
  }

  @Test
  public void testWorkspaceToReviewRequest_fullResearchPurpose() {
    assertThat(
            ZendeskRequests.workspaceToReviewRequest(
                user,
                new Workspace()
                    .name("my workspace")
                    .namespace("fc-123")
                    .id("wsid")
                    .researchPurpose(
                        new ResearchPurpose()
                            .diseaseFocusedResearch(true)
                            .diseaseOfFocus("cancer")
                            .methodsDevelopment(true)
                            .controlSet(true)
                            .ancestry(true)
                            .commercialPurpose(true)
                            .socialBehavioral(true)
                            .populationHealth(true)
                            .educational(true)
                            .drugDevelopment(true)
                            .population(true)
                            .populationDetails(ImmutableList.copyOf(SpecificPopulation.values()))
                            .additionalNotes("additional notes")
                            .reasonForAllOfUs("reason for aou")
                            .intendedStudy("intended study")
                            .anticipatedFindings("anticipated findings")
                            .timeRequested(1000L)
                            .timeReviewed(1500L)
                            .reviewRequested(true)
                            .approved(false))))
        .isNotNull();
  }

  @Test
  public void testWorkspaceToReviewRequest_primaryPurpose() {
    String zdBody =
        mustStripRawResearchPurposeJSON(
            ZendeskRequests.workspaceToReviewRequest(
                    user,
                    new Workspace()
                        .researchPurpose(
                            new ResearchPurpose()
                                .otherPurpose(true)
                                .otherPurposeDetails("my other details")
                                .methodsDevelopment(true)
                                .diseaseFocusedResearch(true)
                                .diseaseOfFocus("greyscale")))
                .getComment()
                .getBody());
    assertThat(zdBody).contains("my other details");
    assertThat(zdBody).contains("Methods");
    assertThat(zdBody).contains("greyscale");
    assertThat(zdBody).doesNotContain("Educational");
  }

  @Test
  public void testWorkspaceToReviewRequest_otherPopulation() {
    String zdBody =
        mustStripRawResearchPurposeJSON(
            ZendeskRequests.workspaceToReviewRequest(
                    user,
                    new Workspace()
                        .researchPurpose(
                            new ResearchPurpose()
                                .addPopulationDetailsItem(SpecificPopulation.AGE_GROUPS)
                                .addPopulationDetailsItem(SpecificPopulation.OTHER)
                                .otherPopulationDetails("targaryen bloodline")))
                .getComment()
                .getBody());
    assertThat(zdBody).contains(SpecificPopulation.AGE_GROUPS.toString());
    assertThat(zdBody).contains("targaryen bloodline");
    assertThat(zdBody).doesNotContain(SpecificPopulation.GEOGRAPHY.toString());
  }

  private static String mustStripRawResearchPurposeJSON(String body) {
    int index = body.indexOf(ZendeskRequests.RESEARCH_PURPOSE_RAW_JSON_HEADER);
    assertWithMessage("failed to find raw JSON header in body").that(index).isGreaterThan(0);
    return body.substring(0, index);
  }
}
