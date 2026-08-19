package org.pmiops.workbench.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "institution_user_group")
public class DbInstitutionUserGroup {

  private long institutionUserGroupId;
  private DbInstitution institution;
  private DbAccessTier accessTier;
  private String userGroup;

  public DbInstitutionUserGroup() {}

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "institution_user_group_id", nullable = false)
  public long getInstitutionUserGroupId() {
    return institutionUserGroupId;
  }

  public DbInstitutionUserGroup setInstitutionUserGroupId(long institutionUserGroupId) {
    this.institutionUserGroupId = institutionUserGroupId;
    return this;
  }

  @ManyToOne
  @JoinColumn(name = "institution_id", nullable = false)
  public DbInstitution getInstitution() {
    return institution;
  }

  public DbInstitutionUserGroup setInstitution(DbInstitution institution) {
    this.institution = institution;
    return this;
  }

  @Column(name = "user_group", nullable = false)
  public String getUserGroup() {
    return userGroup;
  }

  public DbInstitutionUserGroup setUserGroup(String userGroup) {
    this.userGroup = userGroup;
    return this;
  }

  @ManyToOne()
  @JoinColumn(name = "access_tier_id", nullable = false)
  public DbAccessTier getAccessTier() {
    return accessTier;
  }

  public DbInstitutionUserGroup setAccessTier(DbAccessTier accessTier) {
    this.accessTier = accessTier;
    return this;
  }

  // omit ID field from equality so equivalent objects match regardless
  // of whether they are actually present in the DB

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DbInstitutionUserGroup that = (DbInstitutionUserGroup) o;

    return Objects.equals(userGroup, that.userGroup)
        && Objects.equals(institution, that.institution)
        && Objects.equals(accessTier, that.accessTier);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessTier, userGroup, institution);
  }
}
