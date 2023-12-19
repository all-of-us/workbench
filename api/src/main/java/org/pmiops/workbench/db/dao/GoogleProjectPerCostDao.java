package org.pmiops.workbench.db.dao;

import java.util.List;
import java.util.Set;
import org.pmiops.workbench.db.model.DbGoogleProjectPerCost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class GoogleProjectPerCostDao {
  private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
  private GoogleProjectPerCostRepository projectCostCrudRepository;

  @Autowired
  public GoogleProjectPerCostDao(
      NamedParameterJdbcTemplate namedParameterJdbcTemplate,
      GoogleProjectPerCostRepository projectCostCrudRepository) {
    this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
    this.projectCostCrudRepository = projectCostCrudRepository;
  }

  // We use native SQL here as there may be a large number of rows within a
  // given cohort review; this avoids loading them into memory.
  public void batchInsertProjectPerCost(List<DbGoogleProjectPerCost> dbGoogleProjectPerCosts) {
    String sqlStatement =
        "INSERT INTO googleproject_cost (google_project_id, cost) VALUES "
            + "(:google_project_id, :cost)";

    MapSqlParameterSource[] sqlParameterSourceList =
        dbGoogleProjectPerCosts.stream()
            .map(
                entity ->
                    new MapSqlParameterSource()
                        .addValue("google_project_id", entity.getGoogleProjectId())
                        .addValue("cost", entity.getCost()))
            .toArray(MapSqlParameterSource[]::new);
    namedParameterJdbcTemplate.batchUpdate(sqlStatement, sqlParameterSourceList);
  }

  public Iterable<DbGoogleProjectPerCost> findAllByGoogleProjectId(
      Set<String> googleProjectsForUserSet) {
    return projectCostCrudRepository.findAllById(googleProjectsForUserSet);
  }

  public Iterable<DbGoogleProjectPerCost> findAll() {
    return projectCostCrudRepository.findAll();
  }

  public void deleteAll() {
    projectCostCrudRepository.deleteAll();
  }
}
