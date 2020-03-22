package org.pmiops.workbench.db.model;

import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "institution_user_instructions")
public class DbInstitutionUserInstructions {

  private long institutionUserInstructionsId;
  private long institutionId;
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

  @Column(name = "institution_id", nullable = false)
  public long getInstitutionId() {
    return institutionId;
  }

  public DbInstitutionUserInstructions setInstitutionId(long institutionId) {
    this.institutionId = institutionId;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DbInstitutionUserInstructions that = (DbInstitutionUserInstructions) o;

    return Objects.equals(institutionUserInstructionsId, that.institutionUserInstructionsId)
        && Objects.equals(userInstructions, that.userInstructions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(institutionUserInstructionsId, userInstructions);
  }
}
