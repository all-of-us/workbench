package org.pmiops.workbench.db.model;

import java.util.Objects;
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

  @OneToOne
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

    DbInstitutionUserInstructions that = (DbInstitutionUserInstructions) o;

    return Objects.equals(userInstructions, that.userInstructions)
        && Objects.equals(institution, that.institution);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userInstructions, institution);
  }
}
