package org.pmiops.workbench.google;

import com.google.api.services.cloudresourcemanager.v3.model.Project;
import java.io.IOException;
import java.util.List;
import org.pmiops.workbench.db.model.DbUser;

/** Encapsulate Google APIs for interfacing with Google Cloud ResourceManager. */
public interface CloudResourceManagerService {
  List<Project> getAllProjectsForUser(DbUser user) throws IOException;
}
