package org.pmiops.workbench.db.model;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "institution_tier_requirement")
public class DbInstitutionTierRequirement {

  private long institutionTierRequirementId;
  private DbInstitution institution;
  private DbAccessTier accessTier;
  private boolean eraRequired;
  private RequirementEnum requirementEnum;

  public DbInstitutionTierRequirement() {}

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "institution_tier_requirement_id", nullable = false)
  public long getInstitutionTierRequirementId() {
    return institutionTierRequirementId;
  }

  public DbInstitutionTierRequirement setInstitutionTierRequirementId(long institutionTierRequirementId) {
    this.institutionTierRequirementId = institutionTierRequirementId;
    return this;
  }

  @ManyToOne
  @JoinColumn(name = "institution_id", nullable = false)
  public DbInstitution getInstitution() {
    return institution;
  }

  public DbInstitutionTierRequirement setInstitution(DbInstitution institution) {
    this.institution = institution;
    return this;
  }

  @ManyToOne()
  @JoinColumn(name = "access_tier_id", nullable = false)
  public DbAccessTier getAccessTier() {
    return accessTier;
  }

  public DbInstitutionTierRequirement setAccessTier(DbAccessTier accessTier) {
    this.accessTier = accessTier;
    return this;
  }

  @Column(name = "era_required")
  public boolean eraRequired() {
    return eraRequired;
  }

  public DbInstitutionTierRequirement setEraRequired(boolean eraRequired) {
    this.eraRequired = eraRequired;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "requirement_enum", nullable = false)
  public RequirementEnum getRequirementEnum() {
    return requirementEnum;
  }

  public DbInstitutionTierRequirement setRequirementEnum(RequirementEnum requirementEnum) {
    this.requirementEnum = requirementEnum;
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

    DbInstitutionTierRequirement that = (DbInstitutionTierRequirement) o;

    return Objects.equals(accessTier, that.accessTier)
        && Objects.equals(institution, that.institution)
        && Objects.equals(requirementEnum, that.requirementEnum);
  }

  @Override
  public int hashCode() {
    return Objects.hash(accessTier, institution, requirementEnum);
  }

  public enum RequirementEnum {
    DOMAINS,
    ADDRESSES,
    NO_ACCESS,
  }
}
