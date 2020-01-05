package org.pmiops.workbench.api;

import org.pmiops.workbench.model.StatusAlert;

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
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T14:10:50.813-05:00")

@Controller
public class StatusAlertApiController implements StatusAlertApi {
    private final StatusAlertApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public StatusAlertApiController(StatusAlertApiDelegate delegate) {
        this.delegate = delegate;
    }


    public ResponseEntity<StatusAlert> getStatusAlert() {
        // do some magic!
        return delegate.getStatusAlert();
    }

    public ResponseEntity<StatusAlert> postStatusAlert(@ApiParam(value = "the new status alert to use"  )  @Valid @RequestBody StatusAlert statusAlert) {
        // do some magic!
        return delegate.postStatusAlert(statusAlert);
    }

}
