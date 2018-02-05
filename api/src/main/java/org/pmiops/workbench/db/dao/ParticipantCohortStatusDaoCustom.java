package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.cohortreview.util.PageRequest;
import org.pmiops.workbench.db.model.ParticipantCohortStatus;
import org.pmiops.workbench.model.Filter;

import java.util.List;

/**
 * This implementation manually creates batched sql statements. For unknown reasons Spring JPA nor
 * JDBC batching works in appengine running in the cloud. It's old school but it solves our batching
 * issue and can handle inserts up to 10,000 participants.
 */
public interface ParticipantCohortStatusDaoCustom {

    void saveParticipantCohortStatusesCustom(List<ParticipantCohortStatus> participantCohortStatuses);

    List<ParticipantCohortStatus> findAll(Long cohortReviewId, List<Filter> filterList, PageRequest pageRequest);
}
