package org.pmiops.workbench;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import javax.inject.Provider;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.pmiops.workbench.db.model.DbUser;
import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.SpecificPopulationEnum;
import org.pmiops.workbench.model.Workspace;
import org.pmiops.workbench.zendesk.ZendeskConfig;
import org.pmiops.workbench.zendesk.ZendeskRequests;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.zendesk.client.v2.Zendesk;
import org.zendesk.client.v2.model.Request;

// Run nightly only, as it creates a Zendesk request on every run without cleanup (spammy).
@Category(NightlyTests.class)
@RunWith(SpringRunner.class)
public class ZendeskIntegrationTest extends BaseIntegrationTest {

  @TestConfiguration
  @Import(ZendeskConfig.class)
  static class Configuration {
    @Bean
    DbUser user() {
      DbUser user = new DbUser();
      user.setUsername("testing@fake-resarch-aou.org");
      user.setGivenName("Integration");
      user.setFamilyName("Testerson");
      return user;
    }
  }

  // This must be created in the request scope as it requires the active username, hence the
  // provider.
  @Autowired private Provider<Zendesk> zendeskProvider;
  @Autowired private DbUser user;

  @Test
  public void testCreateRequest() throws Exception {
    Request req =
        zendeskProvider
            .get()
            .createRequest(
                ZendeskRequests.workspaceToReviewRequest(
                    user,
                    new Workspace()
                        .name("nightly integration test (feel free to delete)")
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
                                .populationDetails(
                                    ImmutableList.copyOf(SpecificPopulationEnum.values()))
                                .additionalNotes("additional notes")
                                .reasonForAllOfUs("reason for aou")
                                .intendedStudy("intended study")
                                .anticipatedFindings("anticipated findings")
                                .timeRequested(1000L)
                                .timeReviewed(1500L)
                                .reviewRequested(true)
                                .approved(false))));
    assertThat(req.getId()).isNotNull();
  }
}
