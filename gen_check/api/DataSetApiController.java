package org.pmiops.workbench.api;

import org.pmiops.workbench.model.DataDictionaryEntry;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.DataSetCodeResponse;
import org.pmiops.workbench.model.DataSetExportRequest;
import org.pmiops.workbench.model.DataSetListResponse;
import org.pmiops.workbench.model.DataSetPreviewRequest;
import org.pmiops.workbench.model.DataSetPreviewResponse;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ErrorResponse;
import org.pmiops.workbench.model.MarkDataSetRequest;

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
public class DataSetApiController implements DataSetApi {
    private final DataSetApiDelegate delegate;

    @org.springframework.beans.factory.annotation.Autowired
    public DataSetApiController(DataSetApiDelegate delegate) {
        this.delegate = delegate;
    }


    public ResponseEntity<DataSet> createDataSet(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "" ,required=true )  @Valid @RequestBody DataSetRequest dataSetRequest) {
        // do some magic!
        return delegate.createDataSet(workspaceNamespace, workspaceId, dataSetRequest);
    }

    public ResponseEntity<EmptyResponse> deleteDataSet(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Data set ID",required=true ) @PathVariable("dataSetId") Long dataSetId) {
        // do some magic!
        return delegate.deleteDataSet(workspaceNamespace, workspaceId, dataSetId);
    }

    public ResponseEntity<EmptyResponse> exportToNotebook(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "" ,required=true )  @Valid @RequestBody DataSetExportRequest dataSetExportRequest) {
        // do some magic!
        return delegate.exportToNotebook(workspaceNamespace, workspaceId, dataSetExportRequest);
    }

    public ResponseEntity<DataSetCodeResponse> generateCode(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "",required=true ) @PathVariable("kernelType") String kernelType,
        @ApiParam(value = "" ,required=true )  @Valid @RequestBody DataSetRequest dataSet) {
        // do some magic!
        return delegate.generateCode(workspaceNamespace, workspaceId, kernelType, dataSet);
    }

    public ResponseEntity<DataDictionaryEntry> getDataDictionaryEntry(@ApiParam(value = "",required=true ) @PathVariable("cdrVersionId") Long cdrVersionId,
        @ApiParam(value = "",required=true ) @PathVariable("domain") String domain,
        @ApiParam(value = "",required=true ) @PathVariable("domainValue") String domainValue) {
        // do some magic!
        return delegate.getDataDictionaryEntry(cdrVersionId, domain, domainValue);
    }

    public ResponseEntity<DataSet> getDataSet(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Data set ID",required=true ) @PathVariable("dataSetId") Long dataSetId) {
        // do some magic!
        return delegate.getDataSet(workspaceNamespace, workspaceId, dataSetId);
    }

    public ResponseEntity<DataSetListResponse> getDataSetByResourceId(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
         @NotNull@ApiParam(value = "", required = true) @RequestParam(value = "resourceType", required = true) String resourceType,
         @NotNull@ApiParam(value = "", required = true) @RequestParam(value = "id", required = true) Long id) {
        // do some magic!
        return delegate.getDataSetByResourceId(workspaceNamespace, workspaceId, resourceType, id);
    }

    public ResponseEntity<DataSetListResponse> getDataSetsInWorkspace(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId) {
        // do some magic!
        return delegate.getDataSetsInWorkspace(workspaceNamespace, workspaceId);
    }

    public ResponseEntity<Boolean> markDirty(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = ""  )  @Valid @RequestBody MarkDataSetRequest markDataSetRequest) {
        // do some magic!
        return delegate.markDirty(workspaceNamespace, workspaceId, markDataSetRequest);
    }

    public ResponseEntity<DataSetPreviewResponse> previewDataSetByDomain(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "" ,required=true )  @Valid @RequestBody DataSetPreviewRequest dataSetPreviewRequest) {
        // do some magic!
        return delegate.previewDataSetByDomain(workspaceNamespace, workspaceId, dataSetPreviewRequest);
    }

    public ResponseEntity<DataSet> updateDataSet(@ApiParam(value = "The Workspace namespace",required=true ) @PathVariable("workspaceNamespace") String workspaceNamespace,
        @ApiParam(value = "The Workspace ID (a.k.a. the workspace's Firecloud name)",required=true ) @PathVariable("workspaceId") String workspaceId,
        @ApiParam(value = "Data set ID",required=true ) @PathVariable("dataSetId") Long dataSetId,
        @ApiParam(value = "data set definition"  )  @Valid @RequestBody DataSetRequest dataSet) {
        // do some magic!
        return delegate.updateDataSet(workspaceNamespace, workspaceId, dataSetId, dataSet);
    }

}
