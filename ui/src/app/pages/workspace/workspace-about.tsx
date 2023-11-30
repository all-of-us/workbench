import * as React from 'react';
import * as fp from 'lodash/fp';
import { faLock } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import {
  CdrVersionTiersResponse,
  Profile,
  UserRole,
  WorkspaceBillingUsageResponse,
  WorkspaceUserRolesResponse,
} from 'generated/fetch';

import {
  Button,
  StyledExternalLink,
  StyledRouterLink,
} from 'app/components/buttons';
import { FlexColumn, FlexRow } from 'app/components/flex';
import { InfoIcon } from 'app/components/icons';
import { TooltipTrigger } from 'app/components/popups';
import { Spinner } from 'app/components/spinners';
import { AoU, AouTitle } from 'app/components/text-wrappers';
import { WithSpinnerOverlayProps } from 'app/components/with-spinner-overlay';
import { ResearchPurpose } from 'app/pages/workspace/research-purpose';
import { WorkspaceShare } from 'app/pages/workspace/workspace-share';
import {
  profileApi,
  workspaceAdminApi,
  workspacesApi,
} from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles, withCdrVersions, withUserProfile } from 'app/utils';
import {
  AuthorityGuardedAction,
  hasAuthorityForAction,
} from 'app/utils/authorities';
import { getCdrVersion } from 'app/utils/cdr-versions';
import { fetchWithErrorModal } from 'app/utils/errors';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { WorkspaceData } from 'app/utils/workspace-data';
import { WorkspacePermissionsUtil } from 'app/utils/workspace-permissions';
import { isUsingFreeTierBillingAccount } from 'app/utils/workspace-utils';
import { supportUrls } from 'app/utils/zendesk';

interface WorkspaceProps extends WithSpinnerOverlayProps {
  profileState: { profile: Profile; reload: Function; updateCache: Function };
  cdrVersionTiersResponse: CdrVersionTiersResponse;
}

interface WorkspaceState {
  sharing: boolean;
  workspace: WorkspaceData;
  workspaceInitialCreditsUsage: number;
  workspaceUserRoles: UserRole[];
  publishing: boolean;
}

const styles = reactStyles({
  mainPage: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'stretch',
    height: 'calc(100% - 60px)',
  },
  rightSidebar: {
    backgroundColor: colorWithWhiteness(colors.primary, 0.85),
    marginRight: '-0.9rem',
    paddingLeft: '0.75rem',
    paddingTop: '1.5rem',
    width: '22%',
    display: 'flex',
    flexDirection: 'column',
  },
  shareHeader: {
    display: 'flex',
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingRight: '0.75rem',
    paddingBottom: '0.75rem',
  },
  infoBox: {
    backgroundColor: colorWithWhiteness(colors.primary, 0.75),
    width: '15rem',
    borderRadius: '5px',
    padding: '0.6rem',
    marginRight: '0.75rem',
    marginBottom: '0.75rem',
    color: colors.primary,
    lineHeight: '14px',
  },
  infoBoxHeader: {
    textTransform: 'uppercase',
    fontSize: '0.6rem',
  },
  lockMessage: {
    padding: '16px',
    boxSizing: 'border-box',
    borderWidth: '1px',
    borderStyle: 'solid',
    borderRadius: '5px',
    color: colors.primary,
    fontFamily: 'Montserrat',
    letterSpacing: 0,
    lineHeight: '22px',
    borderColor: colors.warning,
    backgroundColor: colorWithWhiteness(colors.warning, 0.65),
    maxWidth: 'fit-content',
    marginBottom: '1.5rem',
  },
});

const pageId = 'workspace';

const ShareTooltipText = () => {
  return (
    <div>
      Here you can add and see collaborators with whom you share your workspace.
      <ul>
        <li>
          A <u>Reader</u> can view your notebooks, but not make edits, deletes
          or share contents of the Workspace.
        </li>
        <li>
          A <u>Writer</u> can view, edit and delete content in the Workspace but
          not share the Workspace with others.
        </li>
        <li>
          An <u>Owner</u> can view, edit, delete and share contents in the
          Workspace.
        </li>
      </ul>
    </div>
  );
};

const WorkspaceInfoTooltipText = () => {
  return (
    <div>
      <u>Dataset</u>
      <br />
      The version of the dataset used by this workspace is displayed here
      <br />
      <u>Creation date</u>
      <br />
      The date you created your workspace
      <br />
      <u>Last updated</u>
      <br />
      The date this workspace was last updated
      <br />
      <u>Data Access Tier</u>
      <br />
      To make sure data is accessed only by authorized users, users can request
      and be granted access to data access tiers within the <AouTitle />.
      Currently there are 3 tiers - “Public”, “Registered” and “Controlled”.
      <br />
    </div>
  );
};

export const WorkspaceAbout = fp.flow(
  withUserProfile(),
  withCdrVersions()
)(
  class extends React.Component<WorkspaceProps, WorkspaceState> {
    constructor(props) {
      super(props);
      this.state = {
        sharing: false,
        workspace: undefined,
        workspaceInitialCreditsUsage: undefined,
        workspaceUserRoles: [],
        publishing: false,
      };
    }

    componentDidMount() {
      this.props.hideSpinner();
      this.setVisits();
      const workspace = this.reloadWorkspace(currentWorkspaceStore.getValue());
      if (WorkspacePermissionsUtil.canWrite(workspace.accessLevel)) {
        this.loadInitialCreditsUsage(workspace);
      }
      this.loadUserRoles(workspace);
    }

    loadInitialCreditsUsage(workspace: WorkspaceData) {
      fetchWithErrorModal(() =>
        workspacesApi().getBillingUsage(workspace.namespace, workspace.id)
      ).then((usage: WorkspaceBillingUsageResponse) =>
        this.setState({ workspaceInitialCreditsUsage: usage.cost })
      );
    }

    setVisits() {
      const {
        profileState: { profile },
      } = this.props;
      if (!profile.pageVisits.some((v) => v.page === pageId)) {
        fetchWithErrorModal(() =>
          profileApi().updatePageVisits({ page: pageId })
        );
      }
    }

    reloadWorkspace(workspace: WorkspaceData): WorkspaceData {
      this.setState({ workspace });
      return workspace;
    }

    // update the component state AND the store with the new workspace object
    updateWorkspaceState(workspace: WorkspaceData): void {
      currentWorkspaceStore.next(workspace);
      this.setState({ workspace });
    }

    loadUserRoles(workspace: WorkspaceData) {
      this.setState({ workspaceUserRoles: [] });
      fetchWithErrorModal(() =>
        workspacesApi().getFirecloudWorkspaceUserRoles(
          workspace.namespace,
          workspace.id
        )
      ).then((resp: WorkspaceUserRolesResponse) =>
        this.setState({
          workspaceUserRoles: fp.sortBy('familyName', resp.items),
        })
      );
    }

    get workspaceCreationTime(): string {
      if (this.state.workspace) {
        const asDate = new Date(this.state.workspace.creationTime);
        return asDate.toDateString();
      } else {
        return 'Loading...';
      }
    }

    get workspaceLastModifiedTime(): string {
      if (this.state.workspace) {
        const asDate = new Date(this.state.workspace.lastModifiedTime);
        return asDate.toDateString();
      } else {
        return 'Loading...';
      }
    }

    get workspaceGcpBillingSpendUrl(): string {
      return this.state.workspace
        ? 'https://console.cloud.google.com/billing/' +
            this.state.workspace.billingAccountName.replace(
              'billingAccounts/',
              ''
            ) +
            '/reports;grouping=GROUP_BY_SKU?project=' +
            this.state.workspace.googleProject +
            '&authuser=' +
            this.props.profileState.profile.username
        : '';
    }

    get workspaceBucketUrl(): string {
      return this.state.workspace
        ? 'https://console.cloud.google.com/storage/browser/' +
            this.state.workspace.googleBucketName +
            '?project=' +
            this.state.workspace.googleProject +
            '&authuser=' +
            this.props.profileState.profile.username
        : '';
    }

    publishUnpublishWorkspace(publish: boolean) {
      const { workspace } = this.state;
      const { namespace, id } = workspace;
      this.setState({ publishing: true });
      fetchWithErrorModal(() =>
        publish
          ? workspaceAdminApi().publishWorkspace(namespace, id)
          : workspaceAdminApi().unpublishWorkspace(namespace, id)
      )
        .then(() =>
          this.updateWorkspaceState({ ...workspace, published: publish })
        )
        .finally(() => this.setState({ publishing: false }));
    }

    onShare() {
      this.setState({ sharing: false });
      const workspace = this.reloadWorkspace(currentWorkspaceStore.getValue());
      this.loadUserRoles(workspace);
    }

    render() {
      const {
        profileState: { profile },
        cdrVersionTiersResponse,
      } = this.props;
      const { workspace, workspaceUserRoles, sharing, publishing } = this.state;
      const published = workspace?.published;
      return (
        <div style={styles.mainPage}>
          <FlexColumn style={{ margin: '1.5rem', width: '98%' }}>
            {workspace?.adminLocked && (
              <div data-test-id='lock-workspace-msg' style={styles.lockMessage}>
                <FlexRow>
                  <div style={{ marginRight: '1.5rem', color: colors.warning }}>
                    <FontAwesomeIcon size={'2x'} icon={faLock} />
                  </div>
                  <div>
                    <b>
                      This workspace has been locked due to a compliance
                      violation of the
                      <StyledRouterLink path='/data-code-of-conduct'>
                        {' '}
                        <AoU /> Researcher Workbench Data User Code of Conduct.
                      </StyledRouterLink>
                    </b>
                    <p></p>
                    {/* This is a temp solution in case admin have locked some workspaces already before workbench introduced the ability to save locking reason */}
                    {workspace.adminLockedReason && (
                      <div
                        style={{
                          fontSize: '0.9rem',
                          paddingLeft: '1em',
                          paddingBottom: '1em',
                        }}
                      >
                        <b>REASON: {workspace.adminLockedReason}</b>
                      </div>
                    )}
                    The project team should work with the workspace owner to
                    address areas of non-compliance by updating the workspace
                    description (e.g. “About” page) and corresponding with the{' '}
                    <AoU /> Resources Access Board. For questions, please
                    contact the{' '}
                    <StyledExternalLink
                      href={supportUrls.helpCenter}
                      target='_blank'
                    >
                      Researcher Workbench support team.
                    </StyledExternalLink>
                  </div>
                </FlexRow>
              </div>
            )}
            <ResearchPurpose data-test-id='researchPurpose' />
            {hasAuthorityForAction(
              profile,
              AuthorityGuardedAction.PUBLISH_WORKSPACE
            ) && (
              <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                <Button
                  data-test-id='unpublish-button'
                  onClick={() => this.publishUnpublishWorkspace(false)}
                  disabled={publishing || !published}
                  type={published ? 'primary' : 'secondary'}
                >
                  Unpublish
                </Button>
                <Button
                  data-test-id='publish-button'
                  onClick={() => this.publishUnpublishWorkspace(true)}
                  disabled={publishing || published}
                  type={published ? 'secondary' : 'primary'}
                  style={{ marginLeft: '0.75rem' }}
                >
                  Publish
                </Button>
              </div>
            )}
          </FlexColumn>
          <div style={styles.rightSidebar}>
            <div style={styles.shareHeader}>
              <h3 style={{ marginTop: 0 }}>Collaborators:</h3>
              <TooltipTrigger content={ShareTooltipText()}>
                <InfoIcon style={{ margin: '0 0.45rem' }} />
              </TooltipTrigger>
              <TooltipTrigger
                content={<div>Workspace compliance action is required</div>}
                disabled={!workspace?.adminLocked}
              >
                <Button
                  style={{
                    height: '22px',
                    fontSize: 12,
                    marginRight: '0.75rem',
                    maxWidth: '13px',
                  }}
                  disabled={
                    workspaceUserRoles.length === 0 || workspace?.adminLocked
                  }
                  data-test-id='workspaceShareButton'
                  onClick={() => this.setState({ sharing: true })}
                >
                  Share
                </Button>
              </TooltipTrigger>
            </div>
            {workspaceUserRoles.length > 0 ? (
              <React.Fragment>
                {workspaceUserRoles.map((user, i) => (
                  <div key={i} data-test-id={'workspaceUser-' + i}>
                    {user.role + ' : ' + user.email}
                  </div>
                ))}
              </React.Fragment>
            ) : (
              <Spinner
                size={50}
                style={{ display: 'flex', alignSelf: 'center' }}
              />
            )}
            <div>
              <h3 style={{ marginBottom: '0.75rem' }}>
                Workspace Information:
                <TooltipTrigger content={WorkspaceInfoTooltipText()}>
                  <InfoIcon style={{ margin: '0 0.45rem' }} />
                </TooltipTrigger>
              </h3>
              <div style={styles.infoBox} data-test-id='workspaceNamespace'>
                <div style={styles.infoBoxHeader}>Workspace Namespace</div>
                <div style={{ fontSize: '0.75rem' }}>
                  {workspace ? workspace.namespace : 'Loading...'}
                </div>
              </div>
              <div style={styles.infoBox} data-test-id='cdrVersion'>
                <div style={styles.infoBoxHeader}>Dataset</div>
                <div style={{ fontSize: '0.75rem' }}>
                  {workspace
                    ? getCdrVersion(workspace, cdrVersionTiersResponse).name
                    : 'Loading...'}
                </div>
              </div>
              <div style={styles.infoBox} data-test-id='creationDate'>
                <div style={styles.infoBoxHeader}>Creation Date</div>
                <div style={{ fontSize: '0.75rem' }}>
                  {this.workspaceCreationTime}
                </div>
              </div>
              <div style={styles.infoBox} data-test-id='lastUpdated'>
                <div style={styles.infoBoxHeader}>Last Updated</div>
                <div style={{ fontSize: '0.75rem' }}>
                  {this.workspaceLastModifiedTime}
                </div>
              </div>
              <div style={styles.infoBox} data-test-id='accessTierShortName'>
                <div style={styles.infoBoxHeader}>Data Access Tier</div>
                <div style={{ fontSize: '0.75rem' }}>
                  {workspace
                    ? fp.capitalize(workspace.accessTierShortName)
                    : 'Loading...'}
                </div>
              </div>
              {workspace &&
                WorkspacePermissionsUtil.canWrite(workspace.accessLevel) &&
                isUsingFreeTierBillingAccount(workspace) && (
                  <div style={{ ...styles.infoBox, height: '3.75rem' }}>
                    <div style={styles.infoBoxHeader}>
                      Workspace Initial Credit Usage
                    </div>
                    <div style={{ fontSize: '0.75rem' }}>
                      {this.state.workspaceInitialCreditsUsage !== undefined ? (
                        '$' + this.state.workspaceInitialCreditsUsage.toFixed(2)
                      ) : (
                        <Spinner style={{ height: 16, width: 16 }} />
                      )}
                    </div>
                  </div>
                )}
            </div>
            <div>
              <h3 style={{ marginBottom: '0.75rem' }}>Cloud Information:</h3>
              <div style={styles.infoBox} data-test-id='googleProject'>
                <div style={styles.infoBoxHeader}>Google Project Id</div>
                <div style={{ fontSize: '0.75rem' }}>
                  {workspace ? workspace.googleProject : 'Loading...'}
                </div>
              </div>
              <div style={styles.infoBox} data-test-id='bucketName'>
                <div style={styles.infoBoxHeader}>Bucket Name</div>
                <div style={{ fontSize: '0.75rem' }}>
                  {workspace ? workspace.googleBucketName : 'Loading...'}
                </div>
              </div>
            </div>

            <TooltipTrigger
              content='Only workspaces owners can view the billing report'
              disabled={
                workspace &&
                WorkspacePermissionsUtil.isOwner(workspace.accessLevel)
              }
            >
              <div>
                <h3 style={{ marginBottom: '0.75rem' }}>Billing</h3>
                <StyledExternalLink
                  data-test-id='workspace-billing-report'
                  href={this.workspaceGcpBillingSpendUrl}
                  target='_blank'
                  disabled={
                    !(
                      workspace &&
                      WorkspacePermissionsUtil.isOwner(workspace.accessLevel)
                    )
                  }
                >
                  View detailed spend report
                </StyledExternalLink>
              </div>
            </TooltipTrigger>
            <div>
              <h3 style={{ marginBottom: '0.75rem' }}>File Management</h3>
              <StyledExternalLink
                data-test-id='workspace-bucket-url'
                href={this.workspaceBucketUrl}
                target='_blank'
              >
                Browse files in Google Cloud Platform
              </StyledExternalLink>
            </div>
          </div>
          {sharing && (
            <WorkspaceShare
              workspace={workspace}
              accessLevel={workspace.accessLevel}
              userEmail={profile.username}
              onClose={() => this.onShare()}
              userRoles={workspaceUserRoles}
              data-test-id='workspaceShareModal'
            />
          )}
        </div>
      );
    }
  }
);
