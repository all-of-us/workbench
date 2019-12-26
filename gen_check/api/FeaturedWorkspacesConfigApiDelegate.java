package org.pmiops.workbench.api;

import org.pmiops.workbench.model.FeaturedWorkspacesConfigResponse;

import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * A delegate to be called by the {@link FeaturedWorkspacesConfigApiController}}.
 * Should be implemented as a controller but without the {@link org.springframework.stereotype.Controller} annotation.
 * Instead, use spring to autowire this class into the {@link FeaturedWorkspacesConfigApiController}.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:53:13.438-06:00")

public interface FeaturedWorkspacesConfigApiDelegate {

    /**
     * @see FeaturedWorkspacesConfigApi#getFeaturedWorkspacesConfig
     */
    ResponseEntity<FeaturedWorkspacesConfigResponse> getFeaturedWorkspacesConfig();

}
