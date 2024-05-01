package org.pmiops.workbench.db.model;

import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Entity
@Table(name = "googleproject_cost")
public class DbGoogleProjectPerCost {
  String googleProjectId;
  Double cost;

  public DbGoogleProjectPerCost() {}

  public DbGoogleProjectPerCost(String googleProjectId, Double cost) {
    this.googleProjectId = googleProjectId;
    this.cost = cost;
  }

  public DbGoogleProjectPerCost(Map.Entry<String, Double> projectCostMap) {
    this.googleProjectId = projectCostMap.getKey();
    this.cost = projectCostMap.getValue();
  }

  @Id
  @Column(name = "google_project_id")
  public String getGoogleProjectId() {
    return googleProjectId;
  }

  public DbGoogleProjectPerCost setGoogleProjectId(String googleProject) {
    this.googleProjectId = googleProject;
    return this;
  }

  @Column(name = "cost")
  public Double getCost() {
    return cost;
  }

  public DbGoogleProjectPerCost setCost(Double cost) {
    this.cost = cost;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof DbGoogleProjectPerCost)) return false;

    DbGoogleProjectPerCost dbGoogleProjectPerCost = (DbGoogleProjectPerCost) o;

    return new EqualsBuilder()
        .append(googleProjectId, dbGoogleProjectPerCost.googleProjectId)
        .append(cost, dbGoogleProjectPerCost.cost)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37).append(googleProjectId).append(googleProjectId).toHashCode();
  }
}
