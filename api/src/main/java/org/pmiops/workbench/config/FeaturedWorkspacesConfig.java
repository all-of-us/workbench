package org.pmiops.workbench.config;

import org.pmiops.workbench.model.FeaturedWorkspace;
import java.util.ArrayList;

/**
 * A class representing the featured workspaces configuration; parsed from JSON stored in the database.
 * See {@link CacheSpringConfiguration}. This should be kept in sync with files in the config/
 * directory.
 */
public class FeaturedWorkspacesConfig {
    public ArrayList<FeaturedWorkspace> featuredWorkspacesList;

    public static FeaturedWorkspacesConfig createEmptyConfig() {
        FeaturedWorkspacesConfig fwConfig = new FeaturedWorkspacesConfig();
        fwConfig.featuredWorkspacesList = new ArrayList<>();
        return fwConfig;
    }

}
