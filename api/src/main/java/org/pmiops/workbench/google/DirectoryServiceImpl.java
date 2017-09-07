package org.pmiops.workbench.google;

import java.util.Arrays;
import java.util.List;

import com.google.api.services.admin.directory.DirectoryScopes;
import org.springframework.stereotype.Service;

@Service
public class DirectoryServiceImpl implements DirectoryService {

  private static final List<String> SCOPES =
    Arrays.asList(DirectoryScopes.ADMIN_DIRECTORY_USER_READONLY);

  public Boolean getTrue() { return true; }
}
