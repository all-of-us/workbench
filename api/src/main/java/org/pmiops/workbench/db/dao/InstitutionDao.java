package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.dao.projection.ProjectedReportingInstitution;
import org.pmiops.workbench.db.model.DbInstitution;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface InstitutionDao extends CrudRepository<DbInstitution, Long> {
  Optional<DbInstitution> findOneByShortName(final String shortName);

  Optional<DbInstitution> findOneByDisplayName(final String displayName);
  // This JPQL query corresponds to the projection interface ProjectedReportingInstitution. Its
  // types and argument order must match the column names selected exactly, in name,
  // type, and order. Note that in some cases a projection query should JOIN one or more
  // other tables. Currently this is done by hand (with suitable renamings of the other entries
  //  in the projection

  // This code was generated using reporting-wizard.rb at 2020-10-01T12:18:28-04:00.
  // Manual modification should be avoided if possible as this is a one-time generation
  // and does not run on every build and updates must be merged manually for now.

  @Query(
      "SELECT\n"
          + "  i.displayName,\n"
          + "  i.duaTypeEnum,\n"
          + "  i.institutionId,\n"
          + "  i.organizationTypeEnum,\n"
          + "  i.organizationTypeOtherText,\n"
          + "  i.shortName\n"
          + "FROM DbInstitution i")
  List<ProjectedReportingInstitution> getReportingInstitutions();
}
