package org.pmiops.workbench.db.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.pmiops.workbench.model.AffiliationRole;

@Entity
@Table(name = "institutional_affiliation")
public class InstitutionalAffiliation {

  private long institutionalAffiliationId;
  private User user;
  private int orderIndex;
  private String institution;
  private String role;
  private Short affiliation;
  private String other;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "institutional_affiliation_id")
  public long getInstitutionalAffiliationId() {
    return institutionalAffiliationId;
  }

  public void setInstitutionalAffiliationId(long institutionalAffiliationId) {
    this.institutionalAffiliationId = institutionalAffiliationId;
  }

  @ManyToOne
  @JoinColumn(name = "user_id")
  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
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

  @Column(name = "affiliation")
  public Short getAffiliation() {
    return affiliation;
  }

  public void setAffiliation(Short affiliation) {
    this.affiliation = affiliation;
  }

  @Transient
  public AffiliationRole getAffiliationEnum() {
    return DemographicSurveyEnum.affiliationRoleFromStorage(this.affiliation);
  }

  public void setAffiliationEnum(AffiliationRole affiliation) {
    this.affiliation = DemographicSurveyEnum.affiliatioRoleToStorage(affiliation);
  }

  @Column(name = "other")
  public String getOther() {
    return other;
  }

  public void setOther(String other) {
    this.other = other;
  }
}
