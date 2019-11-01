package org.pmiops.workbench.audit.adapters;

import org.pmiops.workbench.model.Profile;

public interface ProfileAuditAdapterService {
  void fireCreateAction(Profile createdProfile);

  void fireUpdateAction(Profile previousProfile, Profile updatedProfile);

  void fireDeleteAction(long userId, String userEmail);
}
