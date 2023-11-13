package org.pmiops.workbench.db.dao;

import org.pmiops.workbench.db.model.DbGoogleProjectPerCost;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface GoogleProjectPerCostDao extends CrudRepository<DbGoogleProjectPerCost, Long> {

  @Query(
      "SELECT w.cost "
          + "FROM DbGoogleProjectPerCost w "
          + "WHERE w.googleProject = (:googleProject)")
  Double getCostByGoogleProject(@Param("googleProject") String googleProject);
}
