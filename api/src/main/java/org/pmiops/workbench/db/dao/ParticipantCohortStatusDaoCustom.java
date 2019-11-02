package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.db.model.DbParticipantCohortStatus;

/**
 * This implementation manually creates batched sql statements. For unknown reasons Spring JPA nor
 * JDBC batching works in appengine running in the cloud. It's old school but it solves our batching
 * issue and can handle inserts up to 10,000 participants.
 */
public interface ParticipantCohortStatusDaoCustom {

  void saveParticipantCohortStatusesCustom(List<DbParticipantCohortStatus> participantCohortStatuses);

  List<DbParticipantCohortStatus> findAll(Long cohortReviewId, PageRequest pageRequest);

  Long findCount(Long cohortReviewId, PageRequest pageRequest);
}
