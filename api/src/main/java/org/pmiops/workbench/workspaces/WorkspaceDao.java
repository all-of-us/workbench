package org.pmiops.workbench.workspaces;

import java.util.Collection;
import java.util.List;
import org.pmiops.workbench.db.model.Workspace;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Declaration of automatic query methods for Workspaces. The methods declared here are
 * automatically interpreted by Spring Data (see README).
 *
 * <p>Use of @Query is discouraged; if desired, define aliases in WorkspaceService.
 */
@Repository
public interface WorkspaceDao extends CrudRepository<Workspace, Long> {

  Workspace findByWorkspaceNamespaceAndFirecloudNameAndActiveStatus(
      String workspaceNamespace, String firecloudName, short activeStatus);

  Workspace findByWorkspaceNamespaceAndNameAndActiveStatus(
      String workspaceNamespace, String name, short activeStatus);

  @Query(
      "SELECT w FROM Workspace w LEFT JOIN FETCH w.cohorts c LEFT JOIN FETCH c.cohortReviews"
          + " WHERE w.workspaceNamespace = (:ns) AND w.firecloudName = (:fcName)"
          + " AND w.activeStatus = (:status)")
  Workspace findByFirecloudNameAndActiveStatusWithEagerCohorts(
      @Param("ns") String workspaceNamespace,
      @Param("fcName") String fcName,
      @Param("status") short status);

  List<Workspace> findByApprovedIsNullAndReviewRequestedTrueOrderByTimeRequested();

  List<Workspace> findAllByFirecloudUuidIn(Collection<String> firecloudUuids);
}
