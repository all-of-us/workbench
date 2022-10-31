package org.pmiops.workbench.db.dao;

import java.util.Optional;
import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurvey;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.data.repository.CrudRepository;

public interface NewUserSatisfactionSurveyDao
    extends CrudRepository<DbNewUserSatisfactionSurvey, Long> {
  Optional<DbNewUserSatisfactionSurvey> findByUser(DbUser user);
}
