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
    return initialize(new ResearchPurpose());
  }

  @Override
  public ResearchPurpose initialize(ResearchPurpose defaultConstructedInstance) {
    return defaultConstructedInstance
        .populationDetails(new ArrayList<>());
  }
}
