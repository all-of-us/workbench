package org.pmiops.workbench.db.dao;

import java.util.List;
import org.pmiops.workbench.db.model.CohortReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CohortReviewDao extends JpaRepository<CohortReview, Long> {

  CohortReview findCohortReviewByCohortIdAndCdrVersionId(
      @Param("cohortId") long cohortId, @Param("cdrVersionId") long cdrVersionId);

  @Query(
      value =
          "select * from cohort_review cr "
              + "join cohort c on (cr.cohort_id = c.cohort_id) "
              + "join workspace ws on (c.workspace_id = ws.workspace_id) "
              + "where ws.workspace_namespace = :ns "
              + "and ws.firecloud_name = :fcName "
              + "and ws.active_status = :status",
      nativeQuery = true)
  List<CohortReview> findByFirecloudNameAndActiveStatus(
      @Param("ns") String workspaceNamespace,
      @Param("fcName") String fcName,
      @Param("status") short status);
}
