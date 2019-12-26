package org.pmiops.workbench.api;

import org.pmiops.workbench.model.ConfigResponse;

import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * A delegate to be called by the {@link ConfigApiController}}.
 * Should be implemented as a controller but without the {@link org.springframework.stereotype.Controller} annotation.
 * Instead, use spring to autowire this class into the {@link ConfigApiController}.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:53:13.438-06:00")

public interface ConfigApiDelegate {

    /**
     * @see ConfigApi#getConfig
     */
    ResponseEntity<ConfigResponse> getConfig();

}
