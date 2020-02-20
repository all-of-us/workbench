package org.pmiops.workbench.mail;

// used by MailServiceImpl to make template substitutions
public enum EmailSubstitutionField {
  USERNAME("USERNAME"),
  PASSWORD("PASSWORD"),
  URL("URL"),
  HEADER_IMG("HEADER_IMG"),
  BULLET_1("BULLET_1"),
  BULLET_2("BULLET_2"),
  ACTION("ACTION"),
  BETA_ACCESS_REPORT("BETA_ACCESS_REPORT"),
  FIRST_NAME("FIRST_NAME"),
  LAST_NAME("LAST_NAME"),
  REMAINING_DAYS("REMAINING_DAYS"),
  USED_CREDITS("USED_CREDITS"),
  CREDIT_BALANCE("CREDIT_BALANCE"),
  EXPIRATION_DATE("EXPIRATION_DATE");

  private String value;

  EmailSubstitutionField(String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}
