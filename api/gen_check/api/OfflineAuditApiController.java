package org.pmiops.workbench.api;

import org.pmiops.workbench.model.AuditBigQueryResponse;

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
public class OfflineAuditApiController implements OfflineAuditApi {
    private final OfflineAuditApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public OfflineAuditApiController(OfflineAuditApiDelegate delegate) {
        this.delegate = delegate;
    }


    public ResponseEntity<AuditBigQueryResponse> auditBigQuery() {
        // do some magic!
        return delegate.auditBigQuery();
    }

}
