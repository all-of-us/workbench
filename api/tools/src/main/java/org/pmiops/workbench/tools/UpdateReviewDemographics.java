package org.pmiops.workbench.tools;

import java.util.Arrays;
import org.pmiops.workbench.api.BigQueryService;
import org.pmiops.workbench.db.dao.ParticipantCohortStatusDaoCustom;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Backfill script to adjust cohort reviews that don't include sex at birth. */
@Configuration
public class UpdateReviewDemographics {

  private static final String DISTINCT_CONCEPT_IDS =
      "select distinct sex_at_birth_concept_id FROM `${}.${}.person`";

  @Bean
  public CommandLineRunner run(
      BigQueryService bigQueryService,
      ParticipantCohortStatusDaoCustom participantCohortStatusDaoCustom) {
    return (args) -> {
      if (args.length != 1) {
        throw new IllegalArgumentException("Expected 1 args (dry_run). Got " + Arrays.asList(args));
      }
      boolean dryRun = Boolean.parseBoolean(args[0]);
    };
  }
}
