package org.pmiops.workbench.google;

import com.google.api.services.cloudresourcemanager.model.Project;

import java.util.List;
import org.pmiops.workbench.db.model.User;

/**
 * Encapsulate Google APIs for interfacing with Google Cloud ResourceManager.
 */
public interface CloudResourceManagerService {
  List<Project> getAllProjectsForUser(User user);
}
