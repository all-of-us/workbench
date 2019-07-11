package org.pmiops.workbench.config;

import java.util.ArrayList;
import org.pmiops.workbench.model.FeaturedWorkspace;

/**
 * A class representing the featured workspaces configuration; parsed from JSON stored in the
 * database. See {@link CacheSpringConfiguration}. This should be kept in sync with files in the
 * config/ directory.
 */
public class FeaturedWorkspacesConfig {
  public ArrayList<FeaturedWorkspace> featuredWorkspaces;
}
