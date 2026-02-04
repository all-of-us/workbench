import React, { useEffect, useState } from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import * as fp from 'lodash/fp';
import { Accordion, AccordionTab } from 'primereact/accordion';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';
import { TabPanel, TabView } from 'primereact/tabview';
import { faCopy } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import {
  UserRole,
  VwbWorkspaceAdminView,
  VwbWorkspaceAuditLog,
  WorkspaceAccessLevel,
} from 'generated/fetch';

import { environment } from 'environments/environment';
import { Button } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import { NewWindowIcon } from 'app/components/icons';
import { Error as ErrorDiv } from 'app/components/inputs';
import {
  Modal,
  ModalBody,
  ModalFooter,
  ModalTitle,
} from 'app/components/modals';
import { Spinner, SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { EgressEventsTable } from 'app/pages/admin/egress-events-table';
import { RESEARCH_PURPOSE_MAPPING } from 'app/pages/admin/vwb/vwb-research-purpose-text';
import { WorkspaceInfoField } from 'app/pages/admin/workspace/workspace-info-field';
import { vwbWorkspaceAdminApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import {
  AuthorityGuardedAction,
  renderIfAuthorized,
} from 'app/utils/authorities';
import { MatchParams, profileStore, serverConfigStore } from 'app/utils/stores';

const styles = reactStyles({
  sectionContentContainer: {
    marginLeft: '1.5rem',
  },
  sectionHeader: {
    fontSize: '16px',
    fontWeight: 600,
    color: colors.primary,
    marginTop: '1.5rem',
  },
  sectionSubHeader: {
    fontSize: '14px',
    fontWeight: 600,
    color: colors.primary,
    marginTop: '0.75rem',
  },
  subSectionText: {
    fontSize: '14px',
    fontWeight: 400,
    color: colors.primary,
    lineHeight: '22px',
  },
});

interface ResearchPurposeItem {
  [key: string]: boolean | string;
}

interface WorkspaceResource {
  metadata: {
    resourceId: string;
    name: string;
    description?: string;
    resourceType: string;
    stewardshipType: string;
    cloningInstructions: string;
    controlledResourceMetadata?: {
      region?: string;
      privateResourceUser?: {
        userName: string;
      };
    };
    createdBy?: string;
    state?: string;
  };
  resourceAttributes?: {
    gcpGcsBucket?: {
      bucketName: string;
    };
    gcpDataprocCluster?: {
      clusterId: string;
      region: string;
    };
    gcpBigQueryDataset?: {
      datasetId: string;
    };
    gcpGceInstance?: {
      instanceId: string;
      zone: string;
      status?: string;
    };
  };
}

const collabList = (users: UserRole[]) => {
  return users?.length > 0
    ? users.map((c) => (
        <div style={{ marginLeft: '1rem' }} key={c.email}>
          {c.email}
        </div>
      ))
    : 'None';
};

const getCommandText = (id: string) =>
  `wb workspace access --reason='AoU Prod Support' --workspace=${id}`;

interface Props
  extends WithSpinnerOverlayProps,
    RouteComponentProps<MatchParams> {}

export const AdminVwbWorkspace = fp.flow(withRouter)((props: Props) => {
  const [workspaceDetails, setWorkspaceDetails] =
    useState<VwbWorkspaceAdminView>();
  const [workspaceActivity, setWorkspaceActivity] =
    useState<VwbWorkspaceAuditLog[]>();
  const [loadingWorkspace, setLoadingWorkspace] = useState<boolean>(false);
  const [loadingWorkspaceActivity, setLoadingWorkspaceActivity] =
    useState<boolean>(false);
  const [dataLoadError, setDataLoadError] = useState<Response>();
  const [researchPurpose, setResearchPurpose] = useState<ResearchPurposeItem>();
  const [fetchingResources, setFetchingResources] = useState<boolean>(false);
  const [workspaceResources, setWorkspaceResources] = useState<
    WorkspaceResource[]
  >([]);
  const [resourcesFetched, setResourcesFetched] = useState<boolean>(false);
  const [deletingResourceId, setDeletingResourceId] = useState<string | null>(
    null
  );
  const [confirmDeleteResource, setConfirmDeleteResource] =
    useState<boolean>(false);
  const [resourceToDelete, setResourceToDelete] = useState<{
    id: string;
    type: string;
    name: string;
  } | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  const handleDataLoadError = async (error) => {
    if (error instanceof Response) {
      setDataLoadError(error);
    }
  };

  const getWorkspaceActivity = async (workspaceId: string) => {
    setLoadingWorkspaceActivity(true);

    vwbWorkspaceAdminApi()
      .getVwbWorkspaceAuditLogs(workspaceId)
      .then(setWorkspaceActivity)
      .catch((error) => handleDataLoadError(error))
      .finally(() => setLoadingWorkspaceActivity(false));
  };

  const populateWorkspaceDetails = async () => {
    const { ufid } = props.match.params;
    setLoadingWorkspace(true);

    vwbWorkspaceAdminApi()
      .getVwbWorkspaceAdminView(ufid)
      .then((resp) => {
        setWorkspaceDetails(resp);
        setResearchPurpose(
          (JSON.parse(resp.workspace.researchPurpose)?.[0]
            ?.form_data as ResearchPurposeItem) ?? {}
        );
        getWorkspaceActivity(resp.workspace.id);
      })
      .catch((error) => handleDataLoadError(error))
      .finally(() => setLoadingWorkspace(false));
  };

  const displayResearchPurposeItem = (item) => {
    switch (typeof researchPurpose[item.field]) {
      case 'boolean':
        return item.valueField ? researchPurpose[item.valueField] : item.value;
      case 'string':
        return researchPurpose[item.field];
      default:
        return '';
    }
  };

  const loadWorkspaceResources = async () => {
    const ws = workspaceDetails?.workspace;
    if (!ws) {
      return;
    }

    try {
      // Call backend endpoint which uses service account credentials
      const response = await vwbWorkspaceAdminApi().getVwbWorkspaceResources(
        ws.id
      );

      // Cast from Array<object> to WorkspaceResource[]
      const resources = (response.resources || []) as WorkspaceResource[];

      setWorkspaceResources(resources);
      setResourcesFetched(true);
    } catch (err) {
      // Don't set dataLoadError here - we want to fail silently if AoD isn't active
      setWorkspaceResources([]);
      setResourcesFetched(false);
    }
  };

  const fetchWorkspaceData = async () => {
    const ws = workspaceDetails?.workspace;
    if (!ws) {
      return;
    }

    setFetchingResources(true);

    try {
      // First, enable AoD as service
      await vwbWorkspaceAdminApi().enableAccessOnDemandByUserFacingId(
        ws.userFacingId,
        {
          reason: 'AoU Prod Support',
        }
      );

      // Wait a moment for AoD to propagate
      await new Promise((resolve) => setTimeout(resolve, 2000));

      // Then reload workspace resources
      await loadWorkspaceResources();
    } catch (err) {
      alert(`Failed to fetch workspace data: ${err}`);
      setDataLoadError(err as Response);
    } finally {
      setFetchingResources(false);
    }
  };

  const deleteResource = (
    resourceId: string,
    resourceType: string,
    resourceName: string
  ) => {
    setResourceToDelete({
      id: resourceId,
      type: resourceType,
      name: resourceName,
    });
    setConfirmDeleteResource(true);
  };

  const cancelDeleteResource = () => {
    setConfirmDeleteResource(false);
    setResourceToDelete(null);
  };

  const confirmDelete = async () => {
    if (!resourceToDelete) {
      return;
    }

    setConfirmDeleteResource(false);
    setDeletingResourceId(resourceToDelete.id);
    setDeleteError(null);

    const ws = workspaceDetails?.workspace;
    if (!ws) {
      return;
    }

    try {
      // Call backend API which uses service account credentials
      await vwbWorkspaceAdminApi().deleteVwbWorkspaceResource(
        ws.id,
        resourceToDelete.id,
        resourceToDelete.type
      );

      // Optimistically remove the resource from the local state
      // Note: Delete operations are async in workspace manager, so the resource
      // might still exist for a while, but we remove it from UI immediately
      setWorkspaceResources((prevResources) =>
        prevResources.filter(
          (r) => r.metadata.resourceId !== resourceToDelete.id
        )
      );

      // Clear state after successful deletion
      setDeletingResourceId(null);
      setResourceToDelete(null);
    } catch (err) {
      setDeleteError(
        `Failed to delete resource: ${err.message || err.toString()}`
      );
      // Clear state after error
      setDeletingResourceId(null);
      setResourceToDelete(null);
    }
  };

  useEffect(() => {
    props.hideSpinner();
  }, []);

  useEffect(() => {
    populateWorkspaceDetails();
  }, [props.match.params.ufid]);

  useEffect(() => {
    // Try to load workspace resources when workspace details are available
    // This will succeed if AoD is already active
    if (workspaceDetails?.workspace) {
      loadWorkspaceResources();
    }
  }, [workspaceDetails]);

  const { collaborators, workspace } = workspaceDetails || {};

  return (
    <div style={{ margin: '1.5rem' }}>
      {confirmDeleteResource && resourceToDelete && (
        <Modal onRequestClose={cancelDeleteResource}>
          <ModalTitle>Delete Resource</ModalTitle>
          <ModalBody>
            Are you sure you want to delete <b>{resourceToDelete.name}</b> (
            {resourceToDelete.type})?
            <br />
            <br />
            This action cannot be undone.
          </ModalBody>
          <ModalFooter>
            <Button type='secondary' onClick={cancelDeleteResource}>
              Cancel
            </Button>
            <Button style={{ marginLeft: '0.75rem' }} onClick={confirmDelete}>
              Delete
            </Button>
          </ModalFooter>
        </Modal>
      )}
      {deleteError && (
        <Modal onRequestClose={() => setDeleteError(null)}>
          <ModalTitle>Delete Failed</ModalTitle>
          <ModalBody>{deleteError}</ModalBody>
          <ModalFooter>
            <Button onClick={() => setDeleteError(null)}>Close</Button>
          </ModalFooter>
        </Modal>
      )}
      {dataLoadError && (
        <ErrorDiv>
          Error loading data. Please refresh the page or contact the development
          team.
        </ErrorDiv>
      )}
      {loadingWorkspace && <SpinnerOverlay />}
      {workspace && (
        <div>
          <h3>Basic Information</h3>
          <div className='basic-info' style={{ marginTop: '1.5rem' }}>
            <WorkspaceInfoField labelText='Billing Account Type'>
              {workspace.billingAccountId ===
              serverConfigStore.get().config.initialCreditsBillingAccountId
                ? `Initial credits (${workspace.createdBy})`
                : 'User provided'}
            </WorkspaceInfoField>
            <WorkspaceInfoField labelText='Google Project Id'>
              {workspace.googleProjectId}
            </WorkspaceInfoField>
            <WorkspaceInfoField labelText='Workspace ID'>
              {workspace.id}
            </WorkspaceInfoField>
            <WorkspaceInfoField labelText='User Facing ID'>
              {workspace.userFacingId}
            </WorkspaceInfoField>
            <WorkspaceInfoField labelText='Workspace Name'>
              {workspace.displayName}
            </WorkspaceInfoField>
            <WorkspaceInfoField labelText='Creation Time'>
              {new Date(workspace.creationTime).toDateString()}
            </WorkspaceInfoField>
            <WorkspaceInfoField labelText='Description'>
              {workspace.description}
            </WorkspaceInfoField>
            <WorkspaceInfoField labelText='Workspace Data'>
              <Button
                type='primary'
                style={{ height: '2.25rem' }}
                disabled={fetchingResources}
                onClick={fetchWorkspaceData}
              >
                Fetch Workspace Data
                {fetchingResources && (
                  <Spinner size={18} style={{ marginLeft: '0.75rem' }} />
                )}
              </Button>
            </WorkspaceInfoField>
            <Accordion style={{ marginTop: '0.5rem' }}>
              <AccordionTab header='Research Purpose'>
                {RESEARCH_PURPOSE_MAPPING.map((section, s) => (
                  <div key={s}>
                    <div style={styles.sectionHeader}>
                      {section.sectionHeader}
                    </div>
                    <div style={styles.sectionContentContainer}>
                      {section.items.some(
                        (item) => !!researchPurpose?.[item.field]
                      ) &&
                        section.items.map((item, i) =>
                          researchPurpose[item.field] ? (
                            <>
                              {item.title && (
                                <div style={styles.sectionSubHeader}>
                                  {item.title}
                                </div>
                              )}
                              <div key={i}>
                                <div
                                  style={{
                                    marginTop: i > 0 ? '1.5rem' : '0.45rem',
                                    marginLeft: '1.5rem',
                                  }}
                                >
                                  {displayResearchPurposeItem(item)}
                                </div>
                              </div>
                              {item.subItems?.some(
                                (subItem) => !!researchPurpose?.[subItem.field]
                              ) &&
                                item.subItems?.map((subItem, si) =>
                                  researchPurpose[subItem.field] ? (
                                    <div
                                      key={si}
                                      style={styles.sectionContentContainer}
                                    >
                                      {subItem.title && (
                                        <div style={styles.sectionSubHeader}>
                                          {subItem.title}
                                        </div>
                                      )}
                                      <div style={styles.subSectionText}>
                                        {displayResearchPurposeItem(subItem)}
                                      </div>
                                    </div>
                                  ) : null
                                )}
                            </>
                          ) : null
                        )}
                    </div>
                  </div>
                ))}
              </AccordionTab>
            </Accordion>
            <h3>Collaborators</h3>
            <div className='collaborators' style={{ marginTop: '1.5rem' }}>
              <div style={{ marginBottom: '0.5rem' }}>
                Creator and Owner: {workspace.createdBy}
              </div>
              <div style={{ marginBottom: '0.5rem' }}>
                Other Owners:{' '}
                {collabList(
                  collaborators.filter(
                    (c) =>
                      c.role === WorkspaceAccessLevel.OWNER &&
                      c.email !== workspace.createdBy
                  )
                )}
              </div>
              <div style={{ marginBottom: '0.5rem' }}>
                Writers:{' '}
                {collabList(
                  collaborators.filter(
                    (c) => c.role === WorkspaceAccessLevel.WRITER
                  )
                )}
              </div>
              <div style={{ marginBottom: '0.5rem' }}>
                Readers:{' '}
                {collabList(
                  collaborators.filter(
                    (c) => c.role === WorkspaceAccessLevel.READER
                  )
                )}
              </div>
            </div>
            <h3>Manual Access to Workspace (Optional)</h3>
            <div
              className='aod'
              style={{ marginTop: '1.5rem', width: '800px' }}
            >
              <div style={{ marginBottom: '1rem' }}>
                <strong>
                  Use this CLI command only if you need to access the workspace
                  in Verily Workbench UI to delete workspace, edit settings, or
                  manage collaborators.
                </strong>
                <FlexRow
                  style={{
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    backgroundColor: colorWithWhiteness(
                      colors.tableBorder,
                      0.75
                    ),
                    border: `1px solid ${colors.tableBorder}`,
                    borderRadius: '5px',
                    color: colors.dark,
                    marginTop: '.75rem',
                    padding: '.75rem 0px',
                  }}
                >
                  <div
                    style={{ paddingLeft: '0.75rem', paddingRight: '0.75rem' }}
                  >
                    {getCommandText(workspace.id)}
                  </div>
                  <FontAwesomeIcon
                    icon={faCopy}
                    style={{ marginRight: '0.6rem', cursor: 'pointer' }}
                    onClick={() =>
                      navigator.clipboard.writeText(
                        getCommandText(workspace.id)
                      )
                    }
                    title='Copy CLI Command'
                  />
                </FlexRow>
              </div>
              <Button
                type='primary'
                style={{ marginLeft: '0.5rem', height: '2.25rem' }}
                onClick={() => {
                  window.open(
                    `${environment.vwbUiUrl}/workspaces/${workspace.id}`,
                    '_blank',
                    'noopener noreferrer'
                  );
                }}
              >
                Open in Verily Workbench{' '}
                <NewWindowIcon style={{ marginLeft: '5px' }} />
              </Button>
            </div>
            {resourcesFetched && (
              <>
                <h3>Workspace Resources</h3>
                <div style={{ marginTop: '1.5rem' }}>
                  <TabView>
                    <TabPanel header='Cloud Environments'>
                      <DataTable
                        value={workspaceResources.filter(
                          (r) =>
                            r.metadata.resourceType === 'GCE_INSTANCE' ||
                            r.metadata.resourceType === 'DATAPROC_CLUSTER'
                        )}
                        emptyMessage='No Cloud Environments found'
                        paginator
                        rows={10}
                      >
                        <Column
                          field='metadata.resourceType'
                          header='Type'
                          headerStyle={{ width: '150px' }}
                          body={(rowData) =>
                            rowData.metadata.resourceType === 'GCE_INSTANCE'
                              ? 'GCE Instance'
                              : 'Dataproc Cluster'
                          }
                        />
                        <Column
                          field='metadata.name'
                          header='Name'
                          headerStyle={{ width: '200px' }}
                        />
                        <Column
                          header='ID / Cluster ID'
                          headerStyle={{ width: '200px' }}
                          body={(rowData) =>
                            rowData.resourceAttributes?.gcpGceInstance
                              ?.instanceId ||
                            rowData.resourceAttributes?.gcpDataprocCluster
                              ?.clusterId ||
                            '-'
                          }
                        />
                        <Column
                          header='Zone / Region'
                          headerStyle={{ width: '150px' }}
                          body={(rowData) =>
                            rowData.resourceAttributes?.gcpGceInstance?.zone ||
                            rowData.resourceAttributes?.gcpDataprocCluster
                              ?.region ||
                            '-'
                          }
                        />
                        <Column
                          header='Status'
                          headerStyle={{ width: '120px' }}
                          body={(rowData) =>
                            rowData.resourceAttributes?.gcpGceInstance
                              ?.status ||
                            rowData.metadata?.state ||
                            '-'
                          }
                        />
                        <Column
                          header='Creator'
                          headerStyle={{ width: '200px' }}
                          body={(rowData) =>
                            rowData.metadata?.createdBy ||
                            rowData.metadata?.controlledResourceMetadata
                              ?.privateResourceUser?.userName ||
                            '-'
                          }
                        />
                        <Column
                          header='Actions'
                          headerStyle={{ width: '100px' }}
                          body={(rowData) => (
                            <Button
                              type='primary'
                              style={{ height: '1.875rem' }}
                              disabled={
                                deletingResourceId ===
                                rowData.metadata.resourceId
                              }
                              onClick={() =>
                                deleteResource(
                                  rowData.metadata.resourceId,
                                  rowData.metadata.resourceType,
                                  rowData.metadata.name
                                )
                              }
                            >
                              {deletingResourceId ===
                              rowData.metadata.resourceId ? (
                                <Spinner size={14} />
                              ) : (
                                'Delete'
                              )}
                            </Button>
                          )}
                        />
                      </DataTable>
                    </TabPanel>
                    <TabPanel header='Cloud Storage'>
                      <DataTable
                        value={workspaceResources.filter(
                          (r) => r.metadata.resourceType === 'GCS_BUCKET'
                        )}
                        emptyMessage='No Cloud Storage buckets found'
                        paginator
                        rows={10}
                      >
                        <Column
                          field='metadata.name'
                          header='Name'
                          headerStyle={{ width: '200px' }}
                        />
                        <Column
                          field='resourceAttributes.gcpGcsBucket.bucketName'
                          header='Bucket Name'
                          headerStyle={{ width: '300px' }}
                          body={(rowData) =>
                            rowData.resourceAttributes?.gcpGcsBucket
                              ?.bucketName || '-'
                          }
                        />
                        <Column
                          header='Creator'
                          headerStyle={{ width: '200px' }}
                          body={(rowData) =>
                            rowData.metadata?.createdBy ||
                            rowData.metadata?.controlledResourceMetadata
                              ?.privateResourceUser?.userName ||
                            '-'
                          }
                        />
                        <Column
                          header='Actions'
                          headerStyle={{ width: '100px' }}
                          body={(rowData) => (
                            <Button
                              type='primary'
                              style={{ height: '1.875rem' }}
                              disabled={
                                deletingResourceId ===
                                rowData.metadata.resourceId
                              }
                              onClick={() =>
                                deleteResource(
                                  rowData.metadata.resourceId,
                                  rowData.metadata.resourceType,
                                  rowData.metadata.name
                                )
                              }
                            >
                              {deletingResourceId ===
                              rowData.metadata.resourceId ? (
                                <Spinner size={14} />
                              ) : (
                                'Delete'
                              )}
                            </Button>
                          )}
                        />
                      </DataTable>
                    </TabPanel>
                    <TabPanel header='BigQuery Dataset'>
                      <DataTable
                        value={workspaceResources.filter(
                          (r) => r.metadata.resourceType === 'BIG_QUERY_DATASET'
                        )}
                        emptyMessage='No BigQuery datasets found'
                        paginator
                        rows={10}
                      >
                        <Column
                          field='metadata.name'
                          header='Name'
                          headerStyle={{ width: '250px' }}
                        />
                        <Column
                          field='metadata.resourceId'
                          header='Resource ID'
                          headerStyle={{ width: '300px' }}
                        />
                        <Column
                          field='resourceAttributes.gcpBigQueryDataset.datasetId'
                          header='Dataset ID'
                          body={(rowData) =>
                            rowData.resourceAttributes?.gcpBigQueryDataset
                              ?.datasetId || '-'
                          }
                        />
                        <Column
                          header='Creator'
                          headerStyle={{ width: '200px' }}
                          body={(rowData) =>
                            rowData.metadata?.createdBy ||
                            rowData.metadata?.controlledResourceMetadata
                              ?.privateResourceUser?.userName ||
                            '-'
                          }
                        />
                        <Column
                          header='Actions'
                          headerStyle={{ width: '120px' }}
                          body={(rowData) => (
                            <Button
                              type='primary'
                              style={{ height: '1.875rem' }}
                              disabled={
                                deletingResourceId ===
                                rowData.metadata.resourceId
                              }
                              onClick={() =>
                                deleteResource(
                                  rowData.metadata.resourceId,
                                  rowData.metadata.resourceType,
                                  rowData.metadata.name
                                )
                              }
                            >
                              {deletingResourceId ===
                              rowData.metadata.resourceId ? (
                                <Spinner size={14} />
                              ) : (
                                'Delete'
                              )}
                            </Button>
                          )}
                        />
                      </DataTable>
                    </TabPanel>
                  </TabView>
                </div>
              </>
            )}
            <h3>Egress event history</h3>
            <div style={{ marginTop: '1.5rem' }}>
              {renderIfAuthorized(
                profileStore.get().profile,
                AuthorityGuardedAction.EGRESS_EVENTS,
                () => (
                  <EgressEventsTable
                    displayPageSize={10}
                    sourceWorkspaceNamespace={workspace.id}
                  />
                )
              )}
            </div>
            <h3>Workspace Activity</h3>
            <div className='collaborators' style={{ marginTop: '1.5rem' }}>
              {loadingWorkspaceActivity ? (
                <Spinner style={{ marginLeft: '40%' }} />
              ) : (
                <DataTable
                  paginator
                  rows={10}
                  emptyMessage='No workspace activity found'
                  loading={loadingWorkspaceActivity}
                  value={workspaceActivity}
                >
                  <Column
                    field='changeType'
                    header='Change Type'
                    headerStyle={{ width: '250px' }}
                  />
                  <Column
                    field='actorEmail'
                    header='Change By'
                    headerStyle={{ width: '150px' }}
                  />
                  <Column
                    field='changeTime'
                    header='Change Time'
                    headerStyle={{ width: '150px' }}
                    body={({ changeTime }) =>
                      new Date(changeTime).toLocaleString()
                    }
                  />
                </DataTable>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
});
