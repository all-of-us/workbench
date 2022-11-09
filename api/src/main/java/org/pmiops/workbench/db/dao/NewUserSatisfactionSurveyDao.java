package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurvey;
import org.springframework.data.repository.CrudRepository;

public interface NewUserSatisfactionSurveyDao
    extends CrudRepository<DbNewUserSatisfactionSurvey, Long> {}
