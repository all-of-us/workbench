package org.pmiops.workbench.notebooks;

import org.pmiops.workbench.exceptions.WorkbenchException;
import org.pmiops.workbench.leonardo.ApiException;
import org.pmiops.workbench.leonardo.model.LeonardoAppType;

public interface LeonardoAppClient {
    void createLeonardoApp(String googleProject, String name, LeonardoAppType appType)
            throws WorkbenchException, ApiException;

    String getLeonardoApp(String googleProject, String name)
            throws WorkbenchException, ApiException;
}
