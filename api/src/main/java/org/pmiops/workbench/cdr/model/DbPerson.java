package org.pmiops.workbench.cdr.model;

import java.sql.Date;
import java.util.Objects;
import java.util.StringJoiner;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "cb_person")
public class DbPerson {

  private long personId;
  private Date dob;
  private int ageAtConsent;
  private int ageAtCdr;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "person_id")
  public long getPersonId() {
    return personId;
  }

  public void setPersonId(long personId) {
    this.personId = personId;
  }

  @Column(name = "dob")
  public Date getDob() {
    return dob;
  }

  public void setDob(Date dob) {
    this.dob = dob;
  }

  @Column(name = "age_at_consent")
  public int getAgeAtConsent() {
    return ageAtConsent;
  }

  public void setAgeAtConsent(int ageAtConsent) {
    this.ageAtConsent = ageAtConsent;
  }

  @Column(name = "age_at_cdr")
  public int getAgeAtCdr() {
    return ageAtCdr;
  }

  public void setAgeAtCdr(int ageAtCdr) {
    this.ageAtCdr = ageAtCdr;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DbPerson dbPerson = (DbPerson) o;
    return ageAtConsent == dbPerson.ageAtConsent
        && ageAtCdr == dbPerson.ageAtCdr
        && Objects.equals(dob, dbPerson.dob);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dob, ageAtConsent, ageAtCdr);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", DbPerson.class.getSimpleName() + "[", "]")
        .add("personId=" + personId)
        .add("dob=" + dob)
        .add("ageAtConsent=" + ageAtConsent)
        .add("ageAtCdr=" + ageAtCdr)
        .toString();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private long personId;
    private Date dob;
    private int ageAtConsent;
    private int ageAtCdr;

    private Builder() {}

    public Builder addPersonId(long personId) {
      this.personId = personId;
      return this;
    }

    public Builder addDob(Date dob) {
      this.dob = dob;
      return this;
    }

    public Builder addAgeAtConsent(int ageAtConsent) {
      this.ageAtConsent = ageAtConsent;
      return this;
    }

    public Builder addAgeAtCdr(int ageAtCdr) {
      this.ageAtCdr = ageAtCdr;
      return this;
    }

    public DbPerson build() {
      DbPerson dbPerson = new DbPerson();
      dbPerson.setPersonId(this.personId);
      dbPerson.setDob(this.dob);
      dbPerson.setAgeAtConsent(this.ageAtConsent);
      dbPerson.setAgeAtCdr(this.ageAtCdr);
      return dbPerson;
    }
  }
}
