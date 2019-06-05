package org.pmiops.workbench.google;

import com.google.api.services.cloudresourcemanager.CloudResourceManager.Builder;
import com.google.api.services.cloudresourcemanager.model.Project;
import org.pmiops.workbench.db.model.User;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CloudResourceManagerServiceImpl implements CloudResourceManagerService {
  @Override
  public List<Project> getAllProjectsForUser(User user) {
    return null;
  }
}