package org.pmiops.workbench.db.dao;


import java.util.List;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingDatasetCohort;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingDatasetConceptSet;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingDatasetDomainIdValue;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.Repository;

/**
 * A less-smart DAO to allow pulling simple types from join tables for reporting w/o changing how
 * we model the relationshipos in real code.
 */
public interface DatasetJoinTableNativeDao extends CrudRepository<ProjectedReportingDatasetCohort, Long> {

  // This JPQL query corresponds to the projection interface ProjectedReportingDatasetCohort. Its
  // types and argument order must match the column names selected exactly, in name,
  // type, and order. Note that in some cases a projection query should JOIN one or more
  // other tables. Currently this is done by hand (with suitable renamings of the other entries
  //  in the projection

  // This code was generated using reporting-wizard.rb at 2020-11-10T11:18:15-05:00.
  // Manual modification should be avoided if possible as this is a one-time generation
  // and does not run on every build and updates must be merged manually for now.

  @Query(name = "SELECT\n"
      + "  d.cohort_id,\n"
      + "  d.dataset_id\n"
      + "FROM data_set_cohort d", nativeQuery = true)
  List<ProjectedReportingDatasetCohort> getReportingDatasetCohorts();

  // This JPQL query corresponds to the projection interface ProjectedReportingDatasetConceptSet. Its
  // types and argument order must match the column names selected exactly, in name,
  // type, and order. Note that in some cases a projection query should JOIN one or more
  // other tables. Currently this is done by hand (with suitable renamings of the other entries
  //  in the projection

  // This code was generated using reporting-wizard.rb at 2020-11-10T11:18:15-05:00.
  // Manual modification should be avoided if possible as this is a one-time generation
  // and does not run on every build and updates must be merged manually for now.

  @Query(value = "SELECT\n"
      + "  d.concept_set_id,\n"
      + "  d.data_set_id\n"
      + "FROM data_set_concept_set d", nativeQuery = true)
  List<ProjectedReportingDatasetConceptSet> obtain_ReportingDatasetConceptSets();

  // This JPQL query corresponds to the projection interface ProjectedReportingDatasetDomainIdValue. Its
// types and argument order must match the column names selected exactly, in name,
// type, and order. Note that in some cases a projection query should JOIN one or more
// other tables. Currently this is done by hand (with suitable renamings of the other entries
//  in the projection

// This code was generated using reporting-wizard.rb at 2020-11-10T11:18:16-05:00.
// Manual modification should be avoided if possible as this is a one-time generation
// and does not run on every build and updates must be merged manually for now.

  @Query(value = "SELECT\n"
      + "  d.data_set_id,\n"
      + "  d.domain_id,\n"
      + "  d.value\n"
      + "FROM workbench.data_set_values d",
      nativeQuery = true)
  List<ProjectedReportingDatasetDomainIdValue> obtain_ReportingDatasetDomainIdValues();
}
