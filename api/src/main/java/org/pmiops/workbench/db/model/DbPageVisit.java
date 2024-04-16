package org.pmiops.workbench.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.sql.Timestamp;

@Entity
@Table(name = "page_visit")
public class DbPageVisit {

  private long pageVisitId;
  private String pageId;
  private Timestamp firstVisit;
  private DbUser user;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "page_visit_id")
  public long getPageVisitId() {
    return pageVisitId;
  }

  public DbPageVisit setPageVisitId(long pageVisitId) {
    this.pageVisitId = pageVisitId;
    return this;
  }

  @ManyToOne
  @JoinColumn(name = "user_id")
  public DbUser getUser() {
    return user;
  }

  public DbPageVisit setUser(DbUser user) {
    this.user = user;
    return this;
  }

  @Column(name = "page_id")
  public String getPageId() {
    return pageId;
  }

  public DbPageVisit setPageId(String pageId) {
    this.pageId = pageId;
    return this;
  }

  @Column(name = "first_visit")
  public Timestamp getFirstVisit() {
    return firstVisit;
  }

  public DbPageVisit setFirstVisit(Timestamp firstVisit) {
    this.firstVisit = firstVisit;
    return this;
  }
}
