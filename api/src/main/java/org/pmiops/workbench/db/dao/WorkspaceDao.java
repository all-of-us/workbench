package org.pmiops.workbench.db.dao;

import java.util.Collection;
import java.util.List;
import org.pmiops.workbench.db.model.User;
import org.pmiops.workbench.db.model.Workspace;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;


/**
 * Declaration of automatic query methods for Workspaces. The methods declared here are
 * automatically interpreted by Spring Data (see README).
 *
 * Use of @Query is discouraged; if desired, define aliases in WorkspaceService.
 */
public interface WorkspaceDao extends CrudRepository<Workspace, Long> {
  Workspace findByWorkspaceNamespaceAndFirecloudName(
      String workspaceNamespace, String firecloudName);
  Workspace findByWorkspaceNamespaceAndName(String workspaceNamespace, String name);
  @Query("SELECT w FROM Workspace w LEFT JOIN FETCH w.cohorts c LEFT JOIN FETCH c.cohortReviews" +
      " WHERE w.workspaceNamespace = (:ns) AND w.firecloudName = (:fcName)")
  Workspace findByFirecloudWithEagerCohorts(
      @Param("ns") String workspaceNamespace, @Param("fcName") String fcName);
  List<Workspace> findByWorkspaceNamespace(String workspaceNamespace);
  List<Workspace> findByCreatorOrderByNameAsc(User creator);
  List<Workspace> findByApprovedIsNullAndReviewRequestedTrueOrderByTimeRequested();
  List<Workspace> findAllByFirecloudUuidIn(Collection<String> firecloudUuids);
}
