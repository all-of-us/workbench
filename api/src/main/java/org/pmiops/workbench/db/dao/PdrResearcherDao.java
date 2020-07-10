package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Optional;
import org.pmiops.workbench.db.model.DbPdrResearcher;
import org.springframework.data.repository.CrudRepository;

public interface PdrResearcherDao extends CrudRepository<DbPdrResearcher, Long> {
  List<DbPdrResearcher> getAllByLastName(String lastNmae);
  Optional<DbPdrResearcher> findByUserId(long userId);
}
