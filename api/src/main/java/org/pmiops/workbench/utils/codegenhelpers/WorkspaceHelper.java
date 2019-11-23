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
    return initialize(new Workspace());
  }

  @Override
  public Workspace initialize(Workspace workspace) {
    if (workspace.getResearchPurpose() == null) {
      final ResearchPurpose researchPurpose = ResearchPurposeHelper.getInstance().create();
      workspace.setResearchPurpose(researchPurpose);
    }
    return workspace;
  }
}
