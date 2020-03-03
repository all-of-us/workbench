package org.pmiops.workbench.cdr.dao;

import org.pmiops.workbench.cdr.model.DbSurveyModule;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SurveyModuleDao extends CrudRepository<DbSurveyModule, Long> {

  List<DbSurveyModule> findByParticipantCountNotOrderByOrderNumberAsc(
      @Param("participantCount") Long participantCount);
}
