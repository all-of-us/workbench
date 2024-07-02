package org.pmiops.workbench.db.model;

import jakarta.persistence.*;

@Entity
@Table(name = "featured_workspace")
public class DbFeaturedWorkspace {

  private long id;
  private DbWorkspace workspace;
  private DbFeaturedCategory category;
  private String description;

  public DbFeaturedWorkspace() {}

  public DbFeaturedWorkspace(
      DbWorkspace workspace, DbFeaturedCategory category, String description) {
    this.workspace = workspace;
    this.category = category;
    this.description = description;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id")
  public long getId() {
    return id;
  }

  public DbFeaturedWorkspace setId(long id) {
    this.id = id;
    return this;
  }

  @OneToOne
  @JoinColumn(name = "workspace_id", nullable = false)
  public DbWorkspace getWorkspace() {
    return workspace;
  }

  public DbFeaturedWorkspace setWorkspace(DbWorkspace workspace) {
    this.workspace = workspace;
    return this;
  }

  @Column(name = "category")
  @Enumerated(EnumType.STRING)
  public DbFeaturedCategory getCategory() {
    return category;
  }

  public DbFeaturedWorkspace setCategory(DbFeaturedCategory category) {
    this.category = category;
    return this;
  }

  @Column(name = "description")
  public String getDescription() {
    return description;
  }

  public DbFeaturedWorkspace setDescription(String description) {
    this.description = description;
    return this;
  }

  public enum DbFeaturedCategory {
    TUTORIAL_WORKSPACES,
    DEMO_PROJECTS,
    PHENOTYPE_LIBRARY,
    COMMUNITY
  }
}
