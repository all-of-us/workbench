package org.pmiops.workbench.api;

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
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T15:08:16.594-06:00")

@Controller
public class OfflineBillingApiController implements OfflineBillingApi {
    private final OfflineBillingApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public OfflineBillingApiController(OfflineBillingApiDelegate delegate) {
        this.delegate = delegate;
    }


    public ResponseEntity<Void> billingProjectGarbageCollection() {
        // do some magic!
        return delegate.billingProjectGarbageCollection();
    }

    public ResponseEntity<Void> bufferBillingProjects() {
        // do some magic!
        return delegate.bufferBillingProjects();
    }

    public ResponseEntity<Void> checkFreeTierBillingUsage() {
        // do some magic!
        return delegate.checkFreeTierBillingUsage();
    }

    public ResponseEntity<Void> cleanBillingBuffer() {
        // do some magic!
        return delegate.cleanBillingBuffer();
    }

    public ResponseEntity<Void> syncBillingProjectStatus() {
        // do some magic!
        return delegate.syncBillingProjectStatus();
    }

}
