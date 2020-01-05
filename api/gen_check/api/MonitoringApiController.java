package org.pmiops.workbench.api;

import org.pmiops.workbench.model.BillingProjectBufferStatus;
import org.pmiops.workbench.model.ErrorResponse;

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
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T14:04:58.961-05:00")

@Controller
public class MonitoringApiController implements MonitoringApi {
    private final MonitoringApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public MonitoringApiController(MonitoringApiDelegate delegate) {
        this.delegate = delegate;
    }


    public ResponseEntity<BillingProjectBufferStatus> getBillingProjectBufferStatus() {
        // do some magic!
        return delegate.getBillingProjectBufferStatus();
    }

}
