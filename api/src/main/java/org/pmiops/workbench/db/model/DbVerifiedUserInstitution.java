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
@Table(name = "verified_user_institution")
public class DbVerifiedUserInstitution {

  private long verifiedUserInstitutionId;
  private DbUser user;
  private DbInstitution institution;
  private Short institutionalRoleEnum;
  private String institutionalRoleOtherText;

  public DbVerifiedUserInstitution() {}

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "verified_user_institution_id")
  public long getVerifiedUserInstitutionId() {
    return verifiedUserInstitutionId;
  }

  public DbVerifiedUserInstitution setVerifiedUserInstitutionId(long verifiedUserInstitutionId) {
    this.verifiedUserInstitutionId = verifiedUserInstitutionId;
    return this;
  }

  @OneToOne()
  @JoinColumn(name = "user_id", nullable = false, unique = true)
  public DbUser getUser() {
    return user;
  }

  public DbVerifiedUserInstitution setUser(DbUser user) {
    this.user = user;
    return this;
  }

  @ManyToOne()
  @JoinColumn(name = "institution_id", nullable = false)
  public DbInstitution getInstitution() {
    return institution;
  }

  public DbVerifiedUserInstitution setInstitution(DbInstitution institution) {
    this.institution = institution;
    return this;
  }

  @Column(name = "institutional_role_enum", nullable = false)
  public InstitutionalRole getInstitutionalRoleEnum() {
    return DbStorageEnums.institutionalRoleFromStorage(institutionalRoleEnum);
  }

  public DbVerifiedUserInstitution setInstitutionalRoleEnum(InstitutionalRole role) {
    this.institutionalRoleEnum = DbStorageEnums.institutionalRoleToStorage(role);
    return this;
  }

  @Column(name = "institutional_role_other_text")
  public String getInstitutionalRoleOtherText() {
    return institutionalRoleOtherText;
  }

  public DbVerifiedUserInstitution setInstitutionalRoleOtherText(
      String institutionalRoleOtherText) {
    this.institutionalRoleOtherText = institutionalRoleOtherText;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DbVerifiedUserInstitution)) {
      return false;
    }

    DbVerifiedUserInstitution that = (DbVerifiedUserInstitution) o;

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
