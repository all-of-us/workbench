package org.pmiops.workbench.utils.codegenhelpers;

import org.pmiops.workbench.model.ResearchPurpose;
import org.pmiops.workbench.model.Workspace;

public class WorkspaceHelper implements GeneratedClassHelper<Workspace> {

  private WorkspaceHelper() {
  }

  private static WorkspaceHelper instance = new WorkspaceHelper();
  public static WorkspaceHelper getInstance() {
    return instance;
  }

  @Override
  public Workspace create() {
    return sanitize(new Workspace());
  }

  @Override
  public Workspace sanitize(Workspace instance) {
    if (instance.getResearchPurpose() == null) {
      final ResearchPurpose researchPurpose = ResearchPurposeHelper.getInstance().create();
      instance.setResearchPurpose(researchPurpose);
    }
    return instance;
  }
}
