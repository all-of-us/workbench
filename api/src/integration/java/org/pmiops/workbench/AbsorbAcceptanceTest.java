package org.pmiops.workbench;

import static com.google.common.truth.Truth.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.pmiops.workbench.absorb.AbsorbService;
import org.pmiops.workbench.absorb.AbsorbServiceImpl;
import org.pmiops.workbench.google.CloudStorageClient;
import org.pmiops.workbench.google.DirectoryServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

public class AbsorbAcceptanceTest extends BaseIntegrationTest {
  @Autowired private CloudStorageClient cloudStorageClient;

  @Autowired private AbsorbService absorbService;

  private final String nonexistantUserEmail = "absorb_acceptance_test_fake@fake-research-aou.org";
  private final String loggedInRTIncompleteUserEmail =
      "absorb_acceptance_test_03@fake-research-aou.org";
  private final String loggedInRTCompleteUserEmail =
      "absorb_acceptance_test_04@fake-research-aou.org";

  @TestConfiguration
  @ComponentScan(basePackageClasses = DirectoryServiceImpl.class)
  @Import({AbsorbServiceImpl.class})
  static class Configuration {}

  @Test
  @Disabled("RW-11039")
  public void testUserHasLoggedIntoAbsorb_True() throws Exception {
    // Setup:
    // - The user has logged into Absorb
    assertThat(absorbService.userHasLoggedIntoAbsorb(loggedInRTIncompleteUserEmail)).isTrue();
  }

  @Test
  @Disabled("RW-11039")
  public void testUserHasLoggedIntoAbsorb_False() throws Exception {
    // Setup:
    // - A user with this email does not exist, therefore they have not logged into Absorb
    assertThat(absorbService.userHasLoggedIntoAbsorb(nonexistantUserEmail)).isFalse();
  }

  @Test
  @Disabled("RW-11039")
  public void testGetEnrollmentsRtIncomplete() throws Exception {
    // Setup:
    // - The user has logged into Absorb
    // - The user has not completed RT training
    var enrollments = absorbService.getActiveEnrollmentsForUser(loggedInRTIncompleteUserEmail);
    assertThat(enrollments.size()).isEqualTo(1);

    var rtTrainingEnrollment =
        enrollments.stream()
            .filter(e -> e.courseId.equals(config.absorb.rtTrainingCourseId))
            .findFirst();
    assertThat(rtTrainingEnrollment.isPresent()).isTrue();
    assertThat(rtTrainingEnrollment.get().completionTime).isNull();

    var ctTrainingEnrollment =
        enrollments.stream()
            .filter(e -> e.courseId.equals(config.absorb.ctTrainingCourseId))
            .findFirst();
    assertThat(ctTrainingEnrollment.isPresent()).isFalse();
  }

  @Test
  @Disabled("RW-11039")
  public void testGetEnrollmentsRtComplete() throws Exception {
    // Setup:
    // - The user has completed RT training in Absorb
    // - The user has not completed CT training in Absorb
    var enrollments = absorbService.getActiveEnrollmentsForUser(loggedInRTCompleteUserEmail);

    // After completing RT training, it may take a few minutes to be auto-enrolled
    // in CT training. In that case, this line fails.
    // See https://docs.google.com/document/d/1ByiB1UVQDWHtRR2LOeNmvjEBbmqT7IRX2MVcwwRKA68
    assertThat(enrollments.size()).isEqualTo(2);

    var rtTrainingEnrollment =
        enrollments.stream()
            .filter(e -> e.courseId.equals(config.absorb.rtTrainingCourseId))
            .findFirst();
    assertThat(rtTrainingEnrollment.isPresent()).isTrue();
    // Completed at 2:56PM EST on 2023-10-13
    assertThat(rtTrainingEnrollment.get().completionTime)
        .isEqualTo(Instant.parse("2023-10-13T18:56:38.500Z"));

    var ctTrainingEnrollment =
        enrollments.stream()
            .filter(e -> e.courseId.equals(config.absorb.ctTrainingCourseId))
            .findFirst();
    assertThat(ctTrainingEnrollment.isPresent()).isTrue();
    assertThat(ctTrainingEnrollment.get().completionTime).isNull();
  }
}
