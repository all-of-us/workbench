package org.pmiops.workbench.api;

import org.pmiops.workbench.model.ClusterListResponse;
import org.pmiops.workbench.model.ClusterLocalizeRequest;
import org.pmiops.workbench.model.ClusterLocalizeResponse;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ErrorResponse;
import org.pmiops.workbench.model.UpdateClusterConfigRequest;

import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * A delegate to be called by the {@link ClusterApiController}}.
 * Should be implemented as a controller but without the {@link org.springframework.stereotype.Controller} annotation.
 * Instead, use spring to autowire this class into the {@link ClusterApiController}.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2019-12-26T14:42:01.876-06:00")

public interface ClusterApiDelegate {

    /**
     * @see ClusterApi#deleteCluster
     */
    ResponseEntity<EmptyResponse> deleteCluster(String clusterNamespace,
        String clusterName);

    /**
     * @see ClusterApi#listClusters
     */
    ResponseEntity<ClusterListResponse> listClusters(String billingProjectId,
        String workspaceFirecloudName);

    /**
     * @see ClusterApi#localize
     */
    ResponseEntity<ClusterLocalizeResponse> localize(String clusterNamespace,
        String clusterName,
        ClusterLocalizeRequest body);

    /**
     * @see ClusterApi#updateClusterConfig
     */
    ResponseEntity<EmptyResponse> updateClusterConfig(UpdateClusterConfigRequest body);

}
