package org.pmiops.workbench.db.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "googleproject_cost")
public class DbGoogleProjectPerCost {
  int id;
  String googleProject;
  Double cost;

  public DbGoogleProjectPerCost(String googleProject, Double cost) {
    this.googleProject = googleProject;
    this.cost = cost;
  }

  public DbGoogleProjectPerCost() {}

  @Id
  @Column(name = "google_project_id")
  public String getGoogleProject() {
    return googleProject;
  }

  public void setGoogleProject(String googleProject) {
    this.googleProject = googleProject;
  }

  @Column(name = "cost")
  public Double getCost() {
    return cost;
  }

  public void setCost(Double cost) {
    this.cost = cost;
  }
}
