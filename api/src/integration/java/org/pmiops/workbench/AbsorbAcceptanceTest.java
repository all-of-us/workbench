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

  @TestConfiguration
  @ComponentScan(basePackageClasses = DirectoryServiceImpl.class)
  @Import({AbsorbServiceImpl.class})
  static class Configuration {}

  @Test
  @Disabled("RW-11039")
  public void testGetEnrollments() throws Exception {
    // Setup:
    // - A user with this email exists
    // - The user has completed RT training in Absorb
    // - The user has not completed CT training in Absorb
    var email = "absorb_acceptance_test_02@fake-research-aou.org";

    var enrollments = absorbService.getActiveEnrollmentsForUser(email);
    assertThat(enrollments.size()).isEqualTo(2);

    var rtTrainingEnrollment =
        enrollments.stream()
            .filter(e -> e.courseId.equals(config.absorb.rtTrainingCourseId))
            .findFirst();
    assertThat(rtTrainingEnrollment.isPresent()).isTrue();
    // Completed at 2:14PM EST on 2023-10-03
    assertThat(rtTrainingEnrollment.get().completionTime)
        .isEqualTo(Instant.parse("2023-10-03T18:14:03.977Z"));

    var ctTrainingEnrollment =
        enrollments.stream()
            .filter(e -> e.courseId.equals(config.absorb.ctTrainingCourseId))
            .findFirst();
    assertThat(ctTrainingEnrollment.isPresent()).isTrue();
    assertThat(ctTrainingEnrollment.get().completionTime).isNull();
  }
}
