import {Component, OnDestroy, OnInit} from '@angular/core';
import * as fp from 'lodash/fp';

import {CdrVersionStorageService} from 'app/services/cdr-version-storage.service';
import {ServerConfigService} from 'app/services/server-config.service';
import {currentWorkspaceStore, navigate, urlParamsStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {ResearchPurposeItems} from 'app/views/workspace-edit';

import {cohortsApi, profileApi, workspacesApi} from 'app/services/swagger-fetch-clients';

import {CdrVersion} from 'generated';

import {environment} from 'environments/environment';
import {Cohort, FileDetail, PageVisit, Workspace, WorkspaceAccessLevel} from 'generated/fetch';


@Component({
  styleUrls: ['../../styles/buttons.css',
    '../../styles/headers.css',
    '../../styles/cards.css',
    '../../styles/tooltip.css',
    './component.css'],
  templateUrl: './component.html',
})
export class WorkspaceComponent implements OnInit, OnDestroy {
  private static PAGE_ID = 'workspace';

  sharing = false;
  showTip: boolean;
  workspace: Workspace;
  cdrVersion: CdrVersion;
  wsId: string;
  wsNamespace: string;
  useBillingProjectBuffer: boolean;
  freeTierBillingProject: string;
  cohortsLoading = true;
  cohortsError = false;
  cohortList: Cohort[] = [];
  accessLevel: WorkspaceAccessLevel;
  notebooksLoading = true;
  notebookError = false;
  notebookList: FileDetail[] = [];
  notebookAuthListeners: EventListenerOrEventListenerObject[] = [];
  researchPurposeArray: String[] = [];
  leftResearchPurposes: String[];
  rightResearchPurposes: String[];
  newPageVisit: PageVisit = { page: WorkspaceComponent.PAGE_ID};
  firstVisit = true;
  username = '';
  creatingNotebook = false;
  workspaceUserRoles = [];

  bugReportOpen: boolean;
  bugReportDescription = '';
  googleBucketModal = false;

  // The updated Workspace About page will be released with the dataset builder
  //  because workspace recent work will be moved to the Data tab.
  showUpdatedResearchPurpose = environment.enableDatasetBuilder;

  private subscriptions = [];

  constructor(
    private serverConfigService: ServerConfigService,
    private cdrVersionStorageService: CdrVersionStorageService
  ) {
    this.closeNotebookModal = this.closeNotebookModal.bind(this);
    this.closeBugReport = this.closeBugReport.bind(this);
  }

  ngOnInit(): void {
    this.reloadWorkspace(currentWorkspaceStore.getValue());
    this.subscriptions.push(currentWorkspaceStore.subscribe((workspace) => {
      if (workspace) {
        this.reloadWorkspace(workspace);
      }
    }));
    this.serverConfigService.getConfig().subscribe(({useBillingProjectBuffer}) => {
      this.useBillingProjectBuffer = useBillingProjectBuffer;
    });

    // TODO: RW-1057
    profileApi().getMe().then(
      profile => {
        this.username = profile.username;
        this.freeTierBillingProject = profile.freeTierBillingProjectName;
        if (profile.pageVisits) {
          this.firstVisit = !profile.pageVisits.some(v =>
            v.page === WorkspaceComponent.PAGE_ID);
        }
        if (this.firstVisit) {
          this.showTip = true;
        }
        profileApi().updatePageVisits(this.newPageVisit);
      }).catch(
        error => console.error(error));
    cohortsApi().getCohortsInWorkspace(this.wsNamespace, this.wsId)
      .then(
        cohortsReceived => {
          for (const coho of cohortsReceived.items) {
            this.cohortList.push(coho);
          }
          this.cohortsLoading = false;
        }).catch(
          error => {
            this.cohortsLoading = false;
            this.cohortsError = true;
          });
    this.loadNotebookList();
    this.loadUserRoles();
    this.cdrVersionStorageService.cdrVersions$.subscribe(resp => {
      this.cdrVersion = resp.items.find(v => v.cdrVersionId === this.workspace.cdrVersionId);
    });
  }

  private reloadWorkspace(workspace: WorkspaceData) {
    this.workspace = workspace;
    this.accessLevel = workspace.accessLevel;
    this.researchPurposeArray = [];
    ResearchPurposeItems.forEach((item) => {
      if (this.workspace.researchPurpose[item.shortName]) {
        let shortDescription = item.shortDescription;
        if (item.shortName === 'diseaseFocusedResearch') {
          shortDescription += ': ' + this.workspace.researchPurpose.diseaseOfFocus;
        }
        if (item.shortName === 'otherPurpose') {
          shortDescription += ': ' + this.workspace.researchPurpose.otherPurposeDetails;
        }
        this.researchPurposeArray.push(shortDescription);
      }
    });
    this.leftResearchPurposes =
      this.researchPurposeArray.slice(0, Math.ceil(this.researchPurposeArray.length / 2));
    this.rightResearchPurposes =
      this.researchPurposeArray.slice(
        this.leftResearchPurposes.length,
        this.researchPurposeArray.length);
    const {ns, wsid} = urlParamsStore.getValue();
    this.wsNamespace = ns;
    this.wsId = wsid;
  }

  private loadUserRoles() {
    workspacesApi().getWorkspaceUserRoles(this.wsNamespace, this.wsId)
      .then(
        resp => {
          this.workspaceUserRoles = fp.sortBy('familyName', resp.items);
        }
      ).catch(
        error => {
          console.log(error);
        }
    );
  }

  private loadNotebookList() {
    workspacesApi().getNoteBookList(this.wsNamespace, this.wsId)
      .then(
        fileList => {
          this.notebookList = fileList;
          this.notebooksLoading = false;
        }).catch(
          error => {
            this.notebooksLoading = false;
            this.notebookError = true;
          });
  }

  ngOnDestroy(): void {
    this.notebookAuthListeners.forEach(f => window.removeEventListener('message', f));
    for (const s of this.subscriptions) {
      s.unsubscribe();
    }
  }

  newNotebook(): void {
    this.creatingNotebook = true;
  }

  closeNotebookModal() {
    this.creatingNotebook = false;
  }

  buildCohort(): void {
    navigate(['/workspaces', this.wsNamespace, this.wsId, 'cohorts', 'build']);
  }

  get workspaceCreationTime(): string {
    const asDate = new Date(this.workspace.creationTime);
    return asDate.toDateString();
  }

  get workspaceLastModifiedTime(): string {
    const asDate = new Date(this.workspace.lastModifiedTime);
    return asDate.toDateString();
  }

  get writePermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER
      || this.accessLevel === WorkspaceAccessLevel.WRITER;
  }

  get ownerPermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER;
  }

  openGoogleBucket() {
    this.googleBucketModal = false;
    const googleBucketUrl = 'https://console.cloud.google.com/storage/browser/' +
        this.workspace.googleBucketName + '?authuser=' + this.username;
    window.open(googleBucketUrl, '_blank');
  }
  share(): void {
    this.sharing = true;
  }

  closeShare(): void {
    this.sharing = false;
  }

  submitNotebooksLoadBugReport(): void {
    this.notebookError = false;
    this.bugReportDescription = 'Could not load notebooks';
    this.bugReportOpen = true;
  }

  workspaceClusterBillingProjectId(): string {
    if (this.useBillingProjectBuffer === undefined) {
      // The server config hasn't loaded yet, we don't yet know which billing
      // project should be used for clusters.
      return null;
    }
    if (!this.useBillingProjectBuffer) {
      return this.freeTierBillingProject;
    }

    if ([WorkspaceAccessLevel.WRITER, WorkspaceAccessLevel.OWNER].includes(this.accessLevel)) {
      return this.workspace.namespace;
    }
    return null;
  }

  closeBugReport(): void {
    this.bugReportOpen = false;
  }
}
