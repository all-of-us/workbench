package org.pmiops.workbench.tools;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;

public class WorkspaceExportRow {

  public String getCreatorUsername() {
    return creatorUsername;
  }

  public void setCreatorUsername(String creatorUsername) {
    this.creatorUsername = creatorUsername;
  }

  public String getCreatorContactEmail() {
    return creatorContactEmail;
  }

  public void setCreatorContactEmail(String creatorContactEmail) {
    this.creatorContactEmail = creatorContactEmail;
  }

  @CsvBindByName(column = "Username")
  @CsvBindByPosition(position = 1)
  private String creatorUsername;

  @CsvBindByName(column = "Email")
  @CsvBindByPosition(position = 0)
  private String creatorContactEmail;
}
