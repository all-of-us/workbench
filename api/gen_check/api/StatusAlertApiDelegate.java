package org.pmiops.workbench.api;

import org.pmiops.workbench.model.StatusAlert;

import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * A delegate to be called by the {@link StatusAlertApiController}}.
 * Should be implemented as a controller but without the {@link org.springframework.stereotype.Controller} annotation.
 * Instead, use spring to autowire this class into the {@link StatusAlertApiController}.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T14:04:58.961-05:00")

public interface StatusAlertApiDelegate {

    /**
     * @see StatusAlertApi#getStatusAlert
     */
    ResponseEntity<StatusAlert> getStatusAlert();

    /**
     * @see StatusAlertApi#postStatusAlert
     */
    ResponseEntity<StatusAlert> postStatusAlert(StatusAlert statusAlert);

}
