package org.pmiops.workbench.api;

import org.pmiops.workbench.model.StatusResponse;

import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * A delegate to be called by the {@link StatusApiController}}.
 * Should be implemented as a controller but without the {@link org.springframework.stereotype.Controller} annotation.
 * Instead, use spring to autowire this class into the {@link StatusApiController}.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T14:04:58.961-05:00")

public interface StatusApiDelegate {

    /**
     * @see StatusApi#getStatus
     */
    ResponseEntity<StatusResponse> getStatus();

}
