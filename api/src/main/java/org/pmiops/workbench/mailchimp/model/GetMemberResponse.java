package org.pmiops.workbench.mailchimp.model;

public class GetMemberResponse {
  private String contactEmail;
  private String status;

  public GetMemberResponse(String contactEmail, String status) {
    this.contactEmail = contactEmail;
    this.status = status;
  }

  public String getContactEmail() {
    return this.contactEmail;
  }

  public void setContactEmail(String contactEmail) {
    this.contactEmail = contactEmail;
  }

  public String getStatus() {
    return this.status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
