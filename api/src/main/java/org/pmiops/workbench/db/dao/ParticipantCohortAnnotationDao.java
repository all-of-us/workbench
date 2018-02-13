package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.ParticipantCohortAnnotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface ParticipantCohortAnnotationDao extends JpaRepository<ParticipantCohortAnnotation, Long> {

    ParticipantCohortAnnotation findByCohortReviewIdAndCohortAnnotationDefinitionIdAndParticipantId(
            @Param("cohortReviewId") long cohortReviewId,
            @Param("cohortAnnotationDefinitionId") long cohortAnnotationDefinitionId,
            @Param("participantId") long participantId);
}
