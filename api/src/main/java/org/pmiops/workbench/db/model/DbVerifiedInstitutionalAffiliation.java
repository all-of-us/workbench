package org.pmiops.workbench.db.model;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import org.pmiops.workbench.model.InstitutionalRole;

@Entity
@Table(name = "verified_institutional_affiliation")
public class DbVerifiedInstitutionalAffiliation {

  private long verifiedInstitutionalAffiliationId;
  private DbUser user;
  private DbInstitution institution;
  private Short institutionalRoleEnum;
  private String institutionalRoleOtherText;

  public DbVerifiedInstitutionalAffiliation() {}

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "verified_institutional_affiliation_id")
  public long getVerifiedInstitutionalAffiliationId() {
    return verifiedInstitutionalAffiliationId;
  }

  public DbVerifiedInstitutionalAffiliation setVerifiedInstitutionalAffiliationId(
      long verifiedUserInstitutionId) {
    this.verifiedInstitutionalAffiliationId = verifiedUserInstitutionId;
    return this;
  }

  @OneToOne()
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  public DbUser getUser() {
    return user;
  }

  public DbVerifiedInstitutionalAffiliation setUser(DbUser user) {
    this.user = user;
    return this;
  }

  @ManyToOne()
  @JoinColumn(name = "institution_id", nullable = false)
  public DbInstitution getInstitution() {
    return institution;
  }

  public DbVerifiedInstitutionalAffiliation setInstitution(DbInstitution institution) {
    this.institution = institution;
    return this;
  }

  @Column(name = "institutional_role_enum", nullable = false)
  public InstitutionalRole getInstitutionalRoleEnum() {
    return DbStorageEnums.institutionalRoleFromStorage(institutionalRoleEnum);
  }

  public DbVerifiedInstitutionalAffiliation setInstitutionalRoleEnum(InstitutionalRole role) {
    this.institutionalRoleEnum = DbStorageEnums.institutionalRoleToStorage(role);
    return this;
  }

  @Column(name = "institutional_role_other_text")
  public String getInstitutionalRoleOtherText() {
    return institutionalRoleOtherText;
  }

  public DbVerifiedInstitutionalAffiliation setInstitutionalRoleOtherText(
      String institutionalRoleOtherText) {
    this.institutionalRoleOtherText = institutionalRoleOtherText;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DbVerifiedInstitutionalAffiliation)) {
      return false;
    }

    DbVerifiedInstitutionalAffiliation that = (DbVerifiedInstitutionalAffiliation) o;

    return Objects.equals(user, that.user)
        && Objects.equals(institution, that.institution)
        && Objects.equals(institutionalRoleEnum, that.institutionalRoleEnum)
        && Objects.equals(institutionalRoleOtherText, that.institutionalRoleOtherText);
  }

  @Override
  public int hashCode() {
    return Objects.hash(user, institution, institutionalRoleEnum, institutionalRoleOtherText);
  }
}
