package org.pmiops.workbench.api;

import org.pmiops.workbench.model.DataDictionaryEntry;
import org.pmiops.workbench.model.DataSet;
import org.pmiops.workbench.model.DataSetCodeResponse;
import org.pmiops.workbench.model.DataSetExportRequest;
import org.pmiops.workbench.model.DataSetListResponse;
import org.pmiops.workbench.model.DataSetPreviewRequest;
import org.pmiops.workbench.model.DataSetPreviewResponse;
import org.pmiops.workbench.model.DataSetRequest;
import org.pmiops.workbench.model.DomainValuesResponse;
import org.pmiops.workbench.model.EmptyResponse;
import org.pmiops.workbench.model.ErrorResponse;
import org.pmiops.workbench.model.MarkDataSetRequest;

import io.swagger.annotations.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * A delegate to be called by the {@link DataSetApiController}}.
 * Should be implemented as a controller but without the {@link org.springframework.stereotype.Controller} annotation.
 * Instead, use spring to autowire this class into the {@link DataSetApiController}.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.SpringCodegen", date = "2020-01-05T14:10:50.813-05:00")

public interface DataSetApiDelegate {

    /**
     * @see DataSetApi#createDataSet
     */
    ResponseEntity<DataSet> createDataSet(String workspaceNamespace,
        String workspaceId,
        DataSetRequest dataSetRequest);

    /**
     * @see DataSetApi#deleteDataSet
     */
    ResponseEntity<EmptyResponse> deleteDataSet(String workspaceNamespace,
        String workspaceId,
        Long dataSetId);

    /**
     * @see DataSetApi#exportToNotebook
     */
    ResponseEntity<EmptyResponse> exportToNotebook(String workspaceNamespace,
        String workspaceId,
        DataSetExportRequest dataSetExportRequest);

    /**
     * @see DataSetApi#generateCode
     */
    ResponseEntity<DataSetCodeResponse> generateCode(String workspaceNamespace,
        String workspaceId,
        String kernelType,
        DataSetRequest dataSet);

    /**
     * @see DataSetApi#getDataDictionaryEntry
     */
    ResponseEntity<DataDictionaryEntry> getDataDictionaryEntry(Long cdrVersionId,
        String domain,
        String domainValue);

    /**
     * @see DataSetApi#getDataSet
     */
    ResponseEntity<DataSet> getDataSet(String workspaceNamespace,
        String workspaceId,
        Long dataSetId);

    /**
     * @see DataSetApi#getDataSetByResourceId
     */
    ResponseEntity<DataSetListResponse> getDataSetByResourceId(String workspaceNamespace,
        String workspaceId,
        String resourceType,
        Long id);

    /**
     * @see DataSetApi#getDataSetsInWorkspace
     */
    ResponseEntity<DataSetListResponse> getDataSetsInWorkspace(String workspaceNamespace,
        String workspaceId);

    /**
     * @see DataSetApi#getValuesFromDomain
     */
    ResponseEntity<DomainValuesResponse> getValuesFromDomain(String workspaceNamespace,
        String workspaceId,
        String domain);

    /**
     * @see DataSetApi#markDirty
     */
    ResponseEntity<Boolean> markDirty(String workspaceNamespace,
        String workspaceId,
        MarkDataSetRequest markDataSetRequest);

    /**
     * @see DataSetApi#previewDataSetByDomain
     */
    ResponseEntity<DataSetPreviewResponse> previewDataSetByDomain(String workspaceNamespace,
        String workspaceId,
        DataSetPreviewRequest dataSetPreviewRequest);

    /**
     * @see DataSetApi#updateDataSet
     */
    ResponseEntity<DataSet> updateDataSet(String workspaceNamespace,
        String workspaceId,
        Long dataSetId,
        DataSetRequest dataSet);

}
