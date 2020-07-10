package org.pmiops.workbench.db.model;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "vw_pdr_researcher")
public class DbPdrResearcher {
  private long userId;
  private String username;
  private String firstName;
  private String lastName;
  private Timestamp creationTime;
  private Long institutionId;
  private Short institionalRoleEnum;
  private String institutionRoleOtherText;

  @Id
  @Column(name = "user_id")
  public long getUserId() {
    return userId;
  }

  public void setUserId(long userId) {
    this.userId = userId;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public void setCreationTime(Timestamp creationTime) {
    this.creationTime = creationTime;
  }

  public void setInstitutionId(Long institutionId) {
    this.institutionId = institutionId;
  }

  public void setInstitionalRoleEnum(Short institionalRoleEnum) {
    this.institionalRoleEnum = institionalRoleEnum;
  }

  public void setInstitutionRoleOtherText(String institutionRoleOtherText) {
    this.institutionRoleOtherText = institutionRoleOtherText;
  }

  @Column(name = "username")
  public String getUsername() {
    return username;
  }

  @Column(name = "given_name")
  public String getFirstName() {
    return firstName;
  }

  @Column(name = "last_name")
  public String getLastName() {
    return lastName;
  }

  @Column(name = "creation_time")
  public Timestamp getCreationTime() {
    return creationTime;
  }

  @Column(name = "institution_id")
  public Long getInstitutionId() {
    return institutionId;
  }

  @Column(name = "instittional_role_enum")
  public Short getInstitionalRoleEnum() {
    return institionalRoleEnum;
  }

  @Column(name = "instittional_role_other_text")
  public String getInstitutionRoleOtherText() {
    return institutionRoleOtherText;
  }
}
