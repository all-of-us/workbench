package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.Participant;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ParticipantDao extends CrudRepository<Participant, Long> {

    Page<Participant> findParticipantByParticipantKey_CohortIdAndParticipantKey_CdrVersionId(@Param("cohortId") long cohortId,
                                                                                             @Param("cdrVersionId") long cdrVersionId,
                                                                                             Pageable pageRequest);
}
