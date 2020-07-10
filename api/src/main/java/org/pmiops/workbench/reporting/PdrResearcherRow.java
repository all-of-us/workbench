package org.pmiops.workbench.reporting;

import java.sql.Timestamp;
import javax.persistence.Column;
import javax.persistence.Id;

public class PdrResearcherRow {
  private long userId;
  private String username;
  private String firstName;
  private String lastName;
  private Timestamp creationTime;
  private Long institutionId;
  private Short institionalRoleEnum;
  private String institutionRoleOtherText;

  public long getUserId() {
    return userId;
  }

  public String getUsername() {
    return username;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public Timestamp getCreationTime() {
    return creationTime;
  }

  public Long getInstitutionId() {
    return institutionId;
  }

  public Short getInstitionalRoleEnum() {
    return institionalRoleEnum;
  }

  public String getInstitutionRoleOtherText() {
    return institutionRoleOtherText;
  }

}
