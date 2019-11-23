package org.pmiops.workbench.utils.codegenhelpers;

import java.util.ArrayList;
import org.pmiops.workbench.model.ResearchPurpose;

public class ResearchPurposeHelper implements GeneratedClassHelper<ResearchPurpose> {

  private ResearchPurposeHelper() {
  }
  private static ResearchPurposeHelper instance = null;

  public static ResearchPurposeHelper getInstance() {
    if (instance == null) {
      instance = new ResearchPurposeHelper();
    }
    return instance;
  }

  @Override
  public ResearchPurpose create() {
    return sanitize(new ResearchPurpose());
  }

  @Override
  public ResearchPurpose sanitize(ResearchPurpose instance) {
    if (instance == null) {
      instance = new ResearchPurpose();
    }
    return instance
        .populationDetails(new ArrayList<>());
  }
}
