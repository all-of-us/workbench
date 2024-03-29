package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

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
