package org.pmiops.workbench.google;

import com.google.api.services.cloudresourcemanager.model.Project;
import java.util.List;
import org.pmiops.workbench.db.model.DbUser;

/** Encapsulate Google APIs for interfacing with Google Cloud ResourceManager. */
public interface CloudResourceManagerService {
  List<Project> getAllProjectsForUser(DbUser user);
}
