import React, { useEffect, useState } from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import { faCopy } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import * as fp from 'lodash/fp';
import { Accordion, AccordionTab } from 'primereact/accordion';
import { Column } from 'primereact/column';
import { DataTable } from 'primereact/datatable';

import {
  UserRole,
  VwbWorkspaceAdminView,
  VwbWorkspaceAuditLog,
  WorkspaceAccessLevel,
} from 'generated/fetch';

import { environment } from 'environments/environment';
import { Button, StyledExternalLink } from 'app/components/buttons';
import { FlexRow } from 'app/components/flex';
import { NewWindowIcon } from 'app/components/icons';
import { Error as ErrorDiv } from 'app/components/inputs';
import { Spinner, SpinnerOverlay } from 'app/components/spinners';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { RESEARCH_PURPOSE_MAPPING } from 'app/pages/admin/vwb/vwb-research-purpose-text';
import { WorkspaceInfoField } from 'app/pages/admin/workspace/workspace-info-field';
import { vwbWorkspaceAdminApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { MatchParams, serverConfigStore } from 'app/utils/stores';

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

  const handleDataLoadError = async (error) => {
    if (error instanceof Response) {
      console.log('error', error, await error.json());
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

  useEffect(() => {
    props.hideSpinner();
  }, []);

  useEffect(() => {
    populateWorkspaceDetails();
  }, [props.match.params.ufid]);

  const { collaborators, workspace } = workspaceDetails || {};

  return (
    <div style={{ margin: '1.5rem' }}>
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
            <h3>Access On Demand</h3>
            <div
              className='aod'
              style={{ marginTop: '1.5rem', width: '800px' }}
            >
              <div style={{ marginBottom: '1rem' }}>
                To enable Access On Demand for this workspace, run the following
                command using Verily's{' '}
                <StyledExternalLink
                  href='https://support.workbench.verily.com/docs/guides/cli/cli_install_and_run/'
                  target='_blank'
                >
                  Workbench CLI
                </StyledExternalLink>
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
            <h3>Collaborators</h3>
            <div className='collaborators' style={{ marginTop: '1.5rem' }}>
              <div style={{ marginBottom: '1rem' }}>
                Creator and Owner: {workspace.createdBy}
              </div>
              <div style={{ marginBottom: '1rem' }}>
                Other Owners:{' '}
                {collabList(
                  collaborators.filter(
                    (c) =>
                      c.role === WorkspaceAccessLevel.OWNER &&
                      c.email !== workspace.createdBy
                  )
                )}
              </div>
              <div style={{ marginBottom: '1rem' }}>
                Writers:{' '}
                {collabList(
                  collaborators.filter(
                    (c) => c.role === WorkspaceAccessLevel.WRITER
                  )
                )}
              </div>
              <div style={{ marginBottom: '1rem' }}>
                Readers:{' '}
                {collabList(
                  collaborators.filter(
                    (c) => c.role === WorkspaceAccessLevel.READER
                  )
                )}
              </div>
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
