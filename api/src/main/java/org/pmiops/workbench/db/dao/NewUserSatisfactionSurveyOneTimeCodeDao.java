package org.pmiops.workbench.db.dao;

import java.util.UUID;
import org.pmiops.workbench.db.model.DbNewUserSatisfactionSurveyOneTimeCode;
import org.springframework.data.repository.CrudRepository;

public interface NewUserSatisfactionSurveyOneTimeCodeDao
    extends CrudRepository<DbNewUserSatisfactionSurveyOneTimeCode, UUID> {}
