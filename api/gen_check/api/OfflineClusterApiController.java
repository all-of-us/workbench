package org.pmiops.workbench.api;

import org.pmiops.workbench.model.CheckClustersResponse;

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
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T11:49:26.055-05:00")

@Controller
public class OfflineClusterApiController implements OfflineClusterApi {
    private final OfflineClusterApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public OfflineClusterApiController(OfflineClusterApiDelegate delegate) {
        this.delegate = delegate;
    }


    public ResponseEntity<CheckClustersResponse> checkClusters() {
        // do some magic!
        return delegate.checkClusters();
    }

}
