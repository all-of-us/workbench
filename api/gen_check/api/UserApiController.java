package org.pmiops.workbench.api;

import org.pmiops.workbench.model.UserResponse;

import io.swagger.annotations.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import javax.validation.constraints.*;
import javax.validation.Valid;
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T12:07:33.190-05:00")

@Controller
public class UserApiController implements UserApi {
    private final UserApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public UserApiController(UserApiDelegate delegate) {
        this.delegate = delegate;
    }


    public ResponseEntity<UserResponse> user(@ApiParam(value = "String to find in user's name or email address. Search is a case-insensitive substring match.",required=true ) @PathVariable("term") String term,
        @ApiParam(value = "Pagination token retrieved from a previous call to user; used for retrieving additional pages of results. ") @RequestParam(value = "pageToken", required = false) String pageToken,
        @ApiParam(value = "Maximum number of results to return in a response. Defaults to 10. ") @RequestParam(value = "pageSize", required = false) Integer pageSize,
        @ApiParam(value = "Sort order, either 'asc' or 'desc'. Defaults to 'asc' on the user's email.") @RequestParam(value = "sortOrder", required = false) String sortOrder) {
        // do some magic!
        return delegate.user(term, pageToken, pageSize, sortOrder);
    }

}
