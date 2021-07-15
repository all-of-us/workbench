import * as fp from 'lodash/fp';
import * as React from 'react';

import {currentWorkspaceStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';

import {profileApi, workspacesApi} from 'app/services/swagger-fetch-clients';

import {Button} from 'app/components/buttons';
import {FlexColumn} from 'app/components/flex';
import {InfoIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import {WithSpinnerOverlayProps} from 'app/components/with-spinner-overlay';
import {ResearchPurpose} from 'app/pages/workspace/research-purpose';
import {WorkspaceShare} from 'app/pages/workspace/workspace-share';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, withCdrVersions, withUrlParams, withUserProfile} from 'app/utils';
import {AuthorityGuardedAction, hasAuthorityForAction} from 'app/utils/authorities';
import {getCdrVersion} from 'app/utils/cdr-versions';
import {WorkspacePermissionsUtil} from 'app/utils/workspace-permissions';
import {
  BillingAccountType,
  CdrVersionTiersResponse,
  Profile,
  UserRole,
  WorkspaceAccessLevel
} from 'generated/fetch';

interface WorkspaceProps extends WithSpinnerOverlayProps {
  profileState: {profile: Profile, reload: Function, updateCache: Function};
  cdrVersionTiersResponse: CdrVersionTiersResponse;
}

interface WorkspaceState {
  sharing: boolean;
  workspace: WorkspaceData;
  workspaceFreeTierUsage: number;
  workspaceUserRoles: UserRole[];
  publishing: boolean;
}

const styles = reactStyles({
  mainPage: {
    display: 'flex', justifyContent: 'space-between', alignItems: 'stretch',
    height: 'calc(100% - 60px)'
  },
  rightSidebar: {
    backgroundColor: colorWithWhiteness(colors.primary, 0.85), marginRight: '-0.6rem',
    paddingLeft: '0.5rem', paddingTop: '1rem', width: '22%', display: 'flex',
    flexDirection: 'column'
  },
  shareHeader: {
    display: 'flex', flexDirection: 'row', justifyContent: 'space-between', paddingRight: '0.5rem',
    paddingBottom: '0.5rem'
  },
  infoBox: {
    backgroundColor: colorWithWhiteness(colors.primary, 0.75),
    width: '6rem',
    borderRadius: '5px', padding: '0.4rem', marginRight: '0.5rem', marginBottom: '0.5rem',
    color: colors.primary, lineHeight: '14px'
  },
  infoBoxHeader: {
    textTransform: 'uppercase', fontSize: '0.4rem'
  }
});

const pageId = 'workspace';

const ShareTooltipText = () => {
  return <div>
    Here you can add and see collaborators with whom you share your workspace.
    <ul>
      <li>A <u>Reader</u> can view your notebooks, but not make edits,
        deletes or share contents of the Workspace.</li>
      <li>A <u>Writer</u> can view, edit and delete content in the Workspace
        but not share the Workspace with others.</li>
      <li>An <u>Owner</u> can view, edit, delete and share contents in the Workspace.</li>
    </ul>
  </div>;
};

const WorkspaceInfoTooltipText = () => {
  return <div>
    <u>Dataset</u>
    <br/>The version of the dataset used by this workspace is displayed here<br/>
    <u>Creation date</u>
    <br/>The date you created your workspace<br/>
    <u>Last updated</u>
    <br/>The date this workspace was last updated<br/>
    <u>Data Access Tier</u>
    <br/>To make sure data is accessed only by authorized users, users can request
      and be granted access to data access tiers within the <i>All of Us</i> Research Program.
      Currently there are 3 tiers  - “Public”, “Registered” and “Controlled”.<br/>
  </div>;
};

export const WorkspaceAbout = fp.flow(withUserProfile(), withUrlParams(), withCdrVersions())
(class extends React.Component<WorkspaceProps, WorkspaceState> {

  constructor(props) {
    super(props);
    this.state = {
      sharing: false,
      workspace: undefined,
      workspaceFreeTierUsage: undefined,
      workspaceUserRoles: [],
      publishing: false,
    };
  }

  async componentDidMount() {
    this.props.hideSpinner();
    this.setVisits();
    await this.reloadWorkspace(currentWorkspaceStore.getValue());
    this.loadFreeTierUsage();
    this.loadUserRoles();
  }

  async loadFreeTierUsage() {
    const freeTierUsage = await workspacesApi().getBillingUsage(
      this.state.workspace.namespace, this.state.workspace.id);
    this.setState({workspaceFreeTierUsage: freeTierUsage.cost});
  }

  async setVisits() {
    const {profileState: {profile}} = this.props;
    if (!profile.pageVisits.some(v => v.page === pageId)) {
      await profileApi().updatePageVisits({ page: pageId});
    }
  }

  async reloadWorkspace(workspace: WorkspaceData) {
    this.setState({workspace: workspace});
  }

  async loadUserRoles() {
    const {workspace} = this.state;
    this.setState({workspaceUserRoles: []});
    workspacesApi().getFirecloudWorkspaceUserRoles(workspace.namespace, workspace.id).then(
      resp => {
        this.setState({
          workspaceUserRoles: fp.sortBy('familyName', resp.items)});
      }
    ).catch(error => {
      console.error(error);
    });
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

  workspaceRuntimeBillingProjectId(): string {
    const {workspace} = this.state;
    if (workspace === undefined) {
      return null;
    }
    if ([WorkspaceAccessLevel.WRITER, WorkspaceAccessLevel.OWNER].includes(workspace.accessLevel)) {
      return workspace.namespace;
    }
    return null;
  }

  async publishUnpublishWorkspace(publish: boolean) {
    this.setState({publishing: true});
    try {
      if (publish) {
        await workspacesApi()
          .publishWorkspace(this.state.workspace.namespace, this.state.workspace.id);
      } else {
        await workspacesApi()
          .unpublishWorkspace(this.state.workspace.namespace, this.state.workspace.id);
      }
    } catch (error) {
      console.error(error);
    } finally {
      this.setState({publishing: false});
    }
  }

  async onShare() {
    this.setState({sharing: false});
    await this.reloadWorkspace(currentWorkspaceStore.getValue());
    this.loadUserRoles();
  }

  render() {
    const {profileState: {profile}, cdrVersionTiersResponse} = this.props;
    const {workspace, workspaceUserRoles, sharing, publishing} = this.state;
    return <div style={styles.mainPage}>
      <FlexColumn style={{margin: '1rem', width: '98%'}}>
        <ResearchPurpose data-test-id='researchPurpose'/>
        {hasAuthorityForAction(profile, AuthorityGuardedAction.PUBLISH_WORKSPACE) &&
          <div style={{display: 'flex', justifyContent: 'flex-end'}}>
              <Button data-test-id='unpublish-button'
                      disabled={publishing}
                      type='secondary'
                      onClick={() => this.publishUnpublishWorkspace(false)}>Unpublish</Button>
              <Button data-test-id='publish-button'
                      onClick={() => this.publishUnpublishWorkspace(true)}
                      disabled={publishing}
                      style={{marginLeft: '0.5rem'}}>Publish</Button>
        </div>}
      </FlexColumn>
      <div style={styles.rightSidebar}>
        <div style={styles.shareHeader}>
          <h3 style={{marginTop: 0}}>Collaborators:</h3>
          <TooltipTrigger content={ShareTooltipText()}>
            <InfoIcon style={{margin: '0 0.3rem'}}/>
          </TooltipTrigger>
          <Button style={{height: '22px', fontSize: 12, marginRight: '0.5rem',
            maxWidth: '13px'}} disabled={workspaceUserRoles.length === 0}
                  data-test-id='workspaceShareButton'
                  onClick={() => this.setState({sharing: true})}>Share</Button>
        </div>
        {workspaceUserRoles.length > 0 ?
          <React.Fragment>
            {workspaceUserRoles.map((user, i) =>
              <div key={i} data-test-id={'workspaceUser-' + i}>
                {user.role + ' : ' + user.email}
              </div>)}
          </React.Fragment> :
          <Spinner size={50} style={{display: 'flex', alignSelf: 'center'}}/>}
        <div>
          <h3 style={{marginBottom: '0.5rem'}}>Workspace Information:
            <TooltipTrigger content={WorkspaceInfoTooltipText()}>
              <InfoIcon style={{margin: '0 0.3rem'}}/>
            </TooltipTrigger>
          </h3>
          <div style={styles.infoBox} data-test-id='cdrVersion'>
            <div style={styles.infoBoxHeader}>Dataset</div>
            <div style={{fontSize: '0.5rem'}}>
              {workspace ? getCdrVersion(workspace, cdrVersionTiersResponse).name : 'Loading...'}
            </div>
          </div>
          <div style={styles.infoBox} data-test-id='creationDate'>
            <div style={styles.infoBoxHeader}>Creation Date</div>
            <div style={{fontSize: '0.5rem'}}>{this.workspaceCreationTime}</div>
          </div>
          <div style={styles.infoBox} data-test-id='lastUpdated'>
            <div style={styles.infoBoxHeader}>Last Updated</div>
            <div style={{fontSize: '0.5rem'}}>{this.workspaceLastModifiedTime}</div>
          </div>
          <div style={styles.infoBox} data-test-id='accessTierShortName'>
            <div style={styles.infoBoxHeader}>Data Access Tier</div>
            <div style={{fontSize: '0.5rem'}}>{workspace ?
              fp.capitalize(workspace.accessTierShortName) : 'Loading...'}</div>
          </div>
          {workspace && WorkspacePermissionsUtil.canWrite(workspace.accessLevel)
            && workspace.billingAccountType === BillingAccountType.FREETIER &&
              <div style={{...styles.infoBox, height: '2.5rem'}}>
                <div style={styles.infoBoxHeader}>Workspace Free Credit Usage</div>
                <div style={{fontSize: '0.5rem'}}>{this.state.workspaceFreeTierUsage !== undefined ?
                  '$' + this.state.workspaceFreeTierUsage.toFixed(2) :
                  <Spinner style={{height: 16, width: 16}}/>
                }</div>
              </div>}
        </div>
      </div>
      {sharing && <WorkspaceShare workspace={workspace}
                                  accessLevel={workspace.accessLevel}
                                  userEmail={profile.username}
                                  onClose={() => this.onShare()}
                                  userRoles={workspaceUserRoles}
                                  data-test-id='workspaceShareModal'/>}
    </div>;
  }
});
