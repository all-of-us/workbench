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
import org.pmiops.workbench.model.NonAcademicAffiliation;

@Entity
@Table(name = "institutional_affiliation")
public class DbInstitutionalAffiliation {

  private long institutionalAffiliationId;
  private DbUser user;
  private int orderIndex;
  private String institution;
  private String role;
  private Short nonAcademicAffiliation;
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
  public DbUser getUser() {
    return user;
  }

  public void setUser(DbUser user) {
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

  @Column(name = "non_academic_affiliation")
  public Short getNonAcademicAffiliation() {
    return nonAcademicAffiliation;
  }

  public void setNonAcademicAffiliation(Short nonAcademicAffiliation) {
    this.nonAcademicAffiliation = nonAcademicAffiliation;
  }

  @Transient
  public NonAcademicAffiliation getNonAcademicAffiliationEnum() {
    return DbStorageEnums.nonAcademicAffiliationFromStorage(this.nonAcademicAffiliation);
  }

  public void setNonAcademicAffiliationEnum(NonAcademicAffiliation affiliation) {
    this.nonAcademicAffiliation =
        DbStorageEnums.nonAcademicAffiliationToStorage(affiliation);
  }

  @Column(name = "other")
  public String getOther() {
    return other;
  }

  public void setOther(String other) {
    this.other = other;
  }
}
