package org.pmiops.workbench.cdr.dao;

import java.util.List;
import org.pmiops.workbench.cdr.model.DbSurveyModule;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface SurveyModuleDao extends CrudRepository<DbSurveyModule, Long> {

  List<DbSurveyModule> findByParticipantCountNotOrderByOrderNumberAsc(
      @Param("participantCount") Long participantCount);
}
