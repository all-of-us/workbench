package org.pmiops.workbench.config;

import org.pmiops.workbench.model.FeaturedWorkspace;

import java.util.ArrayList;

public class FeaturedWorkspacesConfig {
    public ArrayList<FeaturedWorkspace> featuredWorkspacesList;

    public static FeaturedWorkspacesConfig createEmptyConfig() {
        FeaturedWorkspacesConfig fwConfig = new FeaturedWorkspacesConfig();
        fwConfig.featuredWorkspacesList = new ArrayList<>();
        return fwConfig;
    }

}
