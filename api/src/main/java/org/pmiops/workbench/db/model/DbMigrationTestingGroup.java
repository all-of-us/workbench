package org.pmiops.workbench.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "migration_testing_group")
public class DbMigrationTestingGroup {
  private long migrationTestingGroupId;
  private long userId;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "migration_testing_group_id")
  public long getMigrationTestingGroupId() {
    return migrationTestingGroupId;
  }

  public DbMigrationTestingGroup setMigrationTestingGroupId(long migrationTestingGroupId) {
    this.migrationTestingGroupId = migrationTestingGroupId;
    return this;
  }

  @Column(name = "user_id", nullable = false)
  public long getUserId() {
    return userId;
  }

  public DbMigrationTestingGroup setUserId(long userId) {
    this.userId = userId;
    return this;
  }
}
