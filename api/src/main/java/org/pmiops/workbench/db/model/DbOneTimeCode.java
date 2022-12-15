package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "one_time_code")
public class DbOneTimeCode {
  private UUID id;
  private DbUser user;
  private Timestamp creationTime;
  private Timestamp usedTime;

  @Id
  @GeneratedValue(generator = "uuid2")
  @GenericGenerator(name = "uuid2", strategy = "uuid2")
  @Column(name = "one_time_code_id", columnDefinition = "VARCHAR(36)", nullable = false)
  @Type(type = "uuid-char")
  public UUID getId() {
    return id;
  }

  public DbOneTimeCode setId(UUID one_time_code_id) {
    this.id = one_time_code_id;
    return this;
  }

  @OneToOne
  @JoinColumn(name = "user_id")
  public DbUser getUser() {
    return user;
  }

  public DbOneTimeCode setUser(DbUser user) {
    this.user = user;
    return this;
  }

  @CreationTimestamp
  @Column(name = "creation_time", nullable = false, updatable = false)
  public Timestamp getCreationTime() {
    return creationTime;
  }

  public DbOneTimeCode setCreationTime(Timestamp creationTime) {
    this.creationTime = creationTime;
    return this;
  }

  @Column(name = "used_time")
  public Timestamp getUsedTime() {
    return usedTime;
  }

  public DbOneTimeCode setUsedTime(Timestamp usedTime) {
    this.usedTime = usedTime;
    return this;
  }
}
