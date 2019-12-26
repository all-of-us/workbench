package org.pmiops.workbench.api;

import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.RecentResource;
import org.pmiops.workbench.model.RecentResourceRequest;
import org.pmiops.workbench.model.RecentResourceResponse;

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
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:42:01.876-06:00")

@Controller
public class UserMetricsApiController implements UserMetricsApi {
    private final UserMetricsApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public UserMetricsApiController(UserMetricsApiDelegate delegate) {
        this.delegate = delegate;
    }


    public ResponseEntity<EmptyResponse> deleteRecentResource(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "request object for updating recent resource" ,required=true )  @Valid @RequestBody RecentResourceRequest recentResourceRequest) {
        // do some magic!
        return delegate.deleteRecentResource(workspaceNamespace, workspaceId, recentResourceRequest);
    }

    public ResponseEntity<RecentResourceResponse> getUserRecentResources() {
        // do some magic!
        return delegate.getUserRecentResources();
    }

    public ResponseEntity<RecentResource> updateRecentResource(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "request object for updating recent resource" ,required=true )  @Valid @RequestBody RecentResourceRequest recentResourceRequest) {
        // do some magic!
        return delegate.updateRecentResource(workspaceNamespace, workspaceId, recentResourceRequest);
    }

}
