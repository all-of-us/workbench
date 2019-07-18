import {Component} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {currentWorkspaceStore, serverConfigStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';

import {profileApi, workspacesApi} from 'app/services/swagger-fetch-clients';

import {Button} from 'app/components/buttons';
import {InfoIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import {reactStyles, ReactWrapperBase, withUrlParams, withUserProfile} from 'app/utils';
import {ResearchPurpose} from 'app/views/research-purpose';
import {CdrVersion, Cohort, Profile, UserRole} from 'generated/fetch';
import {Spinner} from "../../components/spinners";


interface WorkspaceState {
  sharing: boolean;
  showTip: boolean;
  cdrVersion: CdrVersion;
  useBillingProjectBuffer: boolean;
  freeTierBillingProject: string;
  cohortsLoading: boolean;
  cohortsError: boolean;
  cohortList: Cohort[];
  workspace: WorkspaceData;
  workspaceUserRoles: UserRole[];
}

// export class WorkspaceComponent implements OnInit, OnDestroy {
//   private static PAGE_ID = 'workspace';
//
//   sharing = false;
//   showTip: boolean;
//   workspace: Workspace;
//   cdrVersion: CdrVersion;
//   wsId: string;
//   wsNamespace: string;
//   useBillingProjectBuffer: boolean;
//   freeTierBillingProject: string;
//   cohortsLoading = true;
//   cohortsError = false;
//   cohortList: Cohort[] = [];
//   accessLevel: WorkspaceAccessLevel;
//   notebooksLoading = true;
//   notebookError = false;
//   notebookList: FileDetail[] = [];
//   notebookAuthListeners: EventListenerOrEventListenerObject[] = [];
//   newPageVisit: PageVisit = { page: WorkspaceComponent.PAGE_ID};
//   firstVisit = true;
//   username = '';
//   creatingNotebook = false;
//   workspaceUserRoles = [];
//
//   bugReportOpen: boolean;
//   bugReportDescription = '';
//   googleBucketModal = false;
//
//   // The updated Workspace About page will be released with the dataset builder
//   //  because workspace recent work will be moved to the Data tab.
//   showUpdatedResearchPurpose = environment.enableDatasetBuilder;
//
//   private subscriptions = [];
//
//   constructor(
//     private serverConfigService: ServerConfigService,
//     private cdrVersionStorageService: CdrVersionStorageService
//   ) {
//     this.closeNotebookModal = this.closeNotebookModal.bind(this);
//     this.closeBugReport = this.closeBugReport.bind(this);
//   }
//
//
//   private loadUserRoles() {
//     workspacesApi().getFirecloudWorkspaceUserRoles(this.wsNamespace, this.wsId)
//       .then(
//         resp => {
//           this.workspaceUserRoles = fp.sortBy('familyName', resp.items);
//         }
//       ).catch(
//         error => {
//           console.log(error);
//         }
//     );
//   }
//   ngOnDestroy(): void {
//     this.notebookAuthListeners.forEach(f => window.removeEventListener('message', f));
//     for (const s of this.subscriptions) {
//       s.unsubscribe();
//     }
//   }
//   buildCohort(): void {
//     navigate(['/workspaces', this.wsNamespace, this.wsId, 'cohorts', 'build']);
//   }
//
//   get workspaceCreationTime(): string {
//     const asDate = new Date(this.workspace.creationTime);
//     return asDate.toDateString();
//   }
//
//   get workspaceLastModifiedTime(): string {
//     const asDate = new Date(this.workspace.lastModifiedTime);
//     return asDate.toDateString();
//   }
//
//   get writePermission(): boolean {
//     return this.accessLevel === WorkspaceAccessLevel.OWNER
//       || this.accessLevel === WorkspaceAccessLevel.WRITER;
//   }
//
//   get ownerPermission(): boolean {
//     return this.accessLevel === WorkspaceAccessLevel.OWNER;
//   }
//
//   openGoogleBucket() {
//     this.googleBucketModal = false;
//     const googleBucketUrl = 'https://console.cloud.google.com/storage/browser/' +
//         this.workspace.googleBucketName + '?authuser=' + this.username;
//     window.open(googleBucketUrl, '_blank');
//   }
//   share(): void {
//     this.sharing = true;
//   }
//
//   closeShare(): void {
//     this.sharing = false;
//     this.loadUserRoles();
//   }
//
//   submitNotebooksLoadBugReport(): void {
//     this.notebookError = false;
//     this.bugReportDescription = 'Could not load notebooks';
//     this.bugReportOpen = true;
//   }
//
//   workspaceClusterBillingProjectId(): string {
//     if (this.useBillingProjectBuffer === undefined) {
//       // The server config hasn't loaded yet, we don't yet know which billing
//       // project should be used for clusters.
//       return null;
//     }
//     if (!this.useBillingProjectBuffer) {
//       return this.freeTierBillingProject;
//     }
//
//     if ([WorkspaceAccessLevel.WRITER, WorkspaceAccessLevel.OWNER].includes(this.accessLevel)) {
//       return this.workspace.namespace;
//     }
//     return null;
//   }
//
//   closeBugReport(): void {
//     this.bugReportOpen = false;
//   }
// }

const styles = reactStyles({
  mainPage: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'stretch',
    height: 'calc(100% - 60px)'
  },
  rightSidebar: {
    backgroundColor: '#E2E2EA',
    marginRight: '-0.6rem',
    paddingLeft: '0.5rem',
    paddingTop: '1rem',
    width: '22%',
    display: 'flex'
  },
  shareHeader: {
    display: 'flex', flexDirection: 'row', justifyContent: 'space-between', padding: '0.5rem'
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

export const WorkspaceAbout = fp.flow(withUserProfile(), withUrlParams())
(class extends React.Component<
  {profileState: {profile: Profile, reload: Function, updateCache: Function}}, WorkspaceState> {

  constructor(props) {
    super(props);
    this.state = {
      sharing: false,
      showTip: false,
      cdrVersion: undefined,
      useBillingProjectBuffer: false,
      freeTierBillingProject: undefined,
      cohortsLoading: true,
      cohortsError: false,
      cohortList: [],
      workspace: undefined,
      workspaceUserRoles: []
    };
  }

  async componentDidMount() {
    const {profileState: {profile}} = this.props;
    this.setState({
      useBillingProjectBuffer: serverConfigStore.getValue().useBillingProjectBuffer,
      freeTierBillingProject: profile.freeTierBillingProjectName,
    });
    this.setVisits();
    await this.reloadWorkspace(currentWorkspaceStore.getValue());
    this.loadUserRoles();
  }

  async setVisits() {
    const {profileState: {profile}} = this.props;
    if (!profile.pageVisits.some(v => v.page === pageId)) {
      this.setState({showTip: true});
      await profileApi().updatePageVisits({ page: pageId});
    }
  }

  async reloadWorkspace(workspace: WorkspaceData) {
    this.setState({workspace: workspace});
  }

  async loadUserRoles() {
    const {workspace} = this.state;
    workspacesApi().getFirecloudWorkspaceUserRoles(workspace.namespace, workspace.id).then(
      resp => {
        this.setState({workspaceUserRoles: fp.sortBy('familyName', resp.items)});
      }
    ).catch(error => {
      console.error(error);
    });
  }

  render() {
    const {workspaceUserRoles} = this.state;
    return <div style={styles.mainPage}>
      <ResearchPurpose/>
      <div style={styles.rightSidebar}>
        <div style={styles.shareHeader}>
          <h3 style={{marginTop: 0}}>Collaborators:</h3>
          <TooltipTrigger content={ShareTooltipText()}>
            <InfoIcon style={{margin: '0 0.3rem'}}/>
          </TooltipTrigger>
          <Button style={{height: '22px', fontSize: 12, marginRight: '0.5rem',
            maxWidth: '13px'}}>Share</Button>
        </div>
        {workspaceUserRoles.length > 0 ?
          <React.Fragment>
            {workspaceUserRoles.map((user, i) =>
              <div key={i}>{user.role + ' : ' + user.email}</div>)}
          </React.Fragment> :
          <Spinner size={50} style={{display: 'flex', alignSelf: 'center'}}/>}
      </div>
    </div>;
  }
});

@Component({
  template: '<div #root></div>'
})
export class WorkspaceComponent extends ReactWrapperBase {
  constructor() {
    super(WorkspaceAbout, []);
  }
}
