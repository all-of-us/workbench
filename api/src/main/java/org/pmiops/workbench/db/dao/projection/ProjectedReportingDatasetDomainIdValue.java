package org.pmiops.workbench.db.dao.projection;

// This is a Spring Data projection interface for the Hibernate entity
// class DbDatasetDomainIdValue. The properties listed correspond to query results
// that will be mapped into BigQuery rows in a (mostly) 1:1 fashion.
// Fields may not be renamed or reordered or have their types
// changed unless both the entity class and any queries returning
// this projection type are in complete agreement.

// This code was generated using reporting-wizard.rb at 2020-11-10T11:18:16-05:00.
// Manual modification should be avoided if possible as this is a one-time generation
// and does not run on every build and updates must be merged manually for now.

public interface ProjectedReportingDatasetDomainIdValue {
  Long getDatasetId();
  String getDomainId();
  String getValue();
}
