package org.pmiops.workbench.db.dao;

import java.util.Optional;
import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurveyResponse;
import org.pmiops.workbench.db.model.DbUser;
import org.springframework.data.repository.CrudRepository;

public interface NewUserSatisfactionSurveyResponseDao
    extends CrudRepository<DbNewUserSatisfactionSurveyResponse, Long> {
  Optional<DbNewUserSatisfactionSurveyResponse> findByUser(DbUser user);
}
