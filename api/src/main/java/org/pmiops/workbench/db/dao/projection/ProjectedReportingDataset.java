package org.pmiops.workbench.db.dao.projection;

// This is a Spring Data projection interface for the Hibernate entity
// class DbDataset. The properties listed correspond to query results
// that will be mapped into BigQuery rows in a (mostly) 1:1 fashion.
// Fields may not be renamed or reordered or have their types
// changed unless both the entity class and any queries returning
// this projection type are in complete agreement.

// This code was generated using reporting-wizard.rb at 2020-11-05T14:31:23-05:00.
// Manual modification should be avoided if possible as this is a one-time generation
// and does not run on every build and updates must be merged manually for now.

import java.sql.Timestamp;

public interface ProjectedReportingDataset {
  Timestamp getCreationTime();

  Long getCreatorId();

  Long getDatasetId();

  String getDescription();

  Boolean getIncludesAllParticipants();

  Timestamp getLastModifiedTime();

  String getName();

  Short getPrePackagedConceptSet();

  Long getWorkspaceId();
}
