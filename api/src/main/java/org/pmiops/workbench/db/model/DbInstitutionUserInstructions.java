package org.pmiops.workbench.db.model;

import java.util.Objects;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "institution_user_instructions")
public class DbInstitutionUserInstructions {

  private long institutionUserInstructionsId;
  private DbInstitution institution;
  private String userInstructions;

  public DbInstitutionUserInstructions() {}

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "institution_user_instructions_id", nullable = false)
  public long getInstitutionUserInstructionsId() {
    return institutionUserInstructionsId;
  }

  public DbInstitutionUserInstructions setInstitutionUserInstructionsId(
      long institutionUserInstructionsId) {
    this.institutionUserInstructionsId = institutionUserInstructionsId;
    return this;
  }

  // Use CascadeType Detach to prevent Institution from deletion if researcher just wants to
  // deletes user instruction from existing Institution
  @OneToOne(cascade = CascadeType.DETACH)
  @JoinColumn(name = "institution_id")
  public DbInstitution getInstitution() {
    return institution;
  }

  public DbInstitutionUserInstructions setInstitution(DbInstitution institution) {
    this.institution = institution;
    return this;
  }

  @Column(name = "user_instructions", nullable = false)
  public String getUserInstructions() {
    return userInstructions;
  }

  public DbInstitutionUserInstructions setUserInstructions(String userInstructions) {
    this.userInstructions = userInstructions;
    return this;
  }

  // logical equality: data members without the ID field

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DbInstitutionUserInstructions that = (DbInstitutionUserInstructions) o;

    return Objects.equals(institution.getInstitutionId(), that.institution.getInstitutionId())
        && Objects.equals(userInstructions, that.userInstructions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institution.getInstitutionId(), userInstructions);
  }
}
