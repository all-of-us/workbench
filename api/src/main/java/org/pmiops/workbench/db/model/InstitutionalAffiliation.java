package org.pmiops.workbench.db.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GenerationType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "institutional_affiliation")
public class InstitutionalAffiliation {

  private long institutionalAffiliationId;
  private long userId;
  private int orderIndex;
  private String institution;
  private String role;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "institutional_affiliation_id")
  public long getInstitutionalAffiliationId() {
    return institutionalAffiliationId;
  }

  public void setInstitutionalAffiliationId(long institutionalAffiliationId) {
    this.institutionalAffiliationId = institutionalAffiliationId;
  }

  @Column(name = "user_id")
  public long getUserId() {
    return userId;
  }

  public void setUserId(long userId) {
    this.userId = userId;
  }

  @Column(name = "order_index")
  public int getOrderIndex() {
    return orderIndex;
  }

  public void setOrderIndex(int orderIndex) {
    this.orderIndex = orderIndex;
  }

  @Column(name = "institution")
  public String getInstitution() {
    return institution;
  }

  public void setInstitution(String institution) {
    this.institution = institution;
  }

  @Column(name = "role")
  public String getRole() {
    return role;
  }

  public void setRole(String role) {
    this.role = role;
  }


}
