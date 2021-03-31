package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.DbWgsExtractCromwellSubmission;
import org.pmiops.workbench.db.model.DbWorkspace;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface WgsExtractCromwellSubmissionDao
    extends CrudRepository<DbWgsExtractCromwellSubmission, Long> {
  List<DbWgsExtractCromwellSubmission> findAllByWorkspace(DbWorkspace dbWorkspace);
}
