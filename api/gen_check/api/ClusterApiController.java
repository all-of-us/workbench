package org.pmiops.workbench.api;

import org.pmiops.workbench.model.ClusterListResponse;
import org.pmiops.workbench.model.ClusterLocalizeRequest;
import org.pmiops.workbench.model.ClusterLocalizeResponse;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ErrorResponse;
import org.pmiops.workbench.model.UpdateClusterConfigRequest;

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
public class ClusterApiController implements ClusterApi {
    private final ClusterApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public ClusterApiController(ClusterApiDelegate delegate) {
        this.delegate = delegate;
    }


    public ResponseEntity<EmptyResponse> deleteCluster(@ApiParam(value = "clusterNamespace",required=true ) @PathVariable("clusterNamespace") String clusterNamespace,
        @ApiParam(value = "clusterName",required=true ) @PathVariable("clusterName") String clusterName) {
        // do some magic!
        return delegate.deleteCluster(clusterNamespace, clusterName);
    }

    public ResponseEntity<ClusterListResponse> listClusters(@ApiParam(value = "The unique identifier of the Google Billing Project containing the clusters",required=true ) @PathVariable("billingProjectId") String billingProjectId,
        @ApiParam(value = "The firecloudName of the workspace whose notebook we're looking at",required=true ) @PathVariable("workspaceFirecloudName") String workspaceFirecloudName) {
        // do some magic!
        return delegate.listClusters(billingProjectId, workspaceFirecloudName);
    }

    public ResponseEntity<ClusterLocalizeResponse> localize(@ApiParam(value = "clusterNamespace",required=true ) @PathVariable("clusterNamespace") String clusterNamespace,
        @ApiParam(value = "clusterName",required=true ) @PathVariable("clusterName") String clusterName,
        @ApiParam(value = "Localization request."  )  @Valid @RequestBody ClusterLocalizeRequest body) {
        // do some magic!
        return delegate.localize(clusterNamespace, clusterName, body);
    }

    public ResponseEntity<EmptyResponse> updateClusterConfig(@ApiParam(value = "Cluster config update request."  )  @Valid @RequestBody UpdateClusterConfigRequest body) {
        // do some magic!
        return delegate.updateClusterConfig(body);
    }

}
