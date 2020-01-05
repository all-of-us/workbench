package org.pmiops.workbench.api;

import org.pmiops.workbench.model.UserResponse;

import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * A delegate to be called by the {@link UserApiController}}.
 * Should be implemented as a controller but without the {@link org.springframework.stereotype.Controller} annotation.
 * Instead, use spring to autowire this class into the {@link UserApiController}.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:54:35.956-05:00")

public interface UserApiDelegate {

    /**
     * @see UserApi#user
     */
    ResponseEntity<UserResponse> user(String term,
        String pageToken,
        Integer pageSize,
        String sortOrder);

}
