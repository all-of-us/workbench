package org.pmiops.workbench.api;

import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ErrorResponse;
import org.pmiops.workbench.model.UpdateUserDisabledRequest;

import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * A delegate to be called by the {@link AuthDomainApiController}}.
 * Should be implemented as a controller but without the {@link org.springframework.stereotype.Controller} annotation.
 * Instead, use spring to autowire this class into the {@link AuthDomainApiController}.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:54:35.956-05:00")

public interface AuthDomainApiDelegate {

    /**
     * @see AuthDomainApi#createAuthDomain
     */
    ResponseEntity<EmptyResponse> createAuthDomain(String groupName);

    /**
     * @see AuthDomainApi#updateUserDisabledStatus
     */
    ResponseEntity<Void> updateUserDisabledStatus(UpdateUserDisabledRequest request);

}
