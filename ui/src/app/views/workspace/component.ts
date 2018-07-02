import {Component, OnDestroy, OnInit, ViewChild} from '@angular/core';
import {Headers, Http, Response} from '@angular/http';
import {ActivatedRoute, Router} from '@angular/router';
import {Comparator, StringFilter} from '@clr/angular';
import {Observable} from 'rxjs/Observable';

import {WorkspaceData} from 'app/resolvers/workspace';
import {SignInService} from 'app/services/sign-in.service';
import {BugReportComponent} from 'app/views/bug-report/component';
import {WorkspaceShareComponent} from 'app/views/workspace-share/component';
import {environment} from 'environments/environment';

import {
  Cluster,
  ClusterService,
  ClusterStatus,
  Cohort,
  CohortsService,
  FileDetail,
  Workspace,
  WorkspaceAccessLevel,
  WorkspacesService,
} from 'generated';


/*
 * Search filters used by the cohort and notebook data tables to
 * determine which of the cohorts loaded into client side memory
 * are displayed.
 */
class CohortNameFilter implements StringFilter<Cohort> {
  accepts(cohort: Cohort, search: string): boolean {
    return cohort.name.toLowerCase().indexOf(search) >= 0;
  }
}
class CohortDescriptionFilter implements StringFilter<Cohort> {
  accepts(cohort: Cohort, search: string): boolean {
    return cohort.description.toLowerCase().indexOf(search) >= 0;
  }
}
class NotebookNameFilter implements StringFilter<FileDetail> {
  accepts(notebook: FileDetail, search: string): boolean {
    return notebook.name.toLowerCase().indexOf(search) >= 0;
  }
}

/*
 * Sort comparators used by the cohort and notebook data tables to
 * determine the order that the cohorts loaded into client side memory
 * are displayed.
 */
class CohortNameComparator implements Comparator<Cohort> {
  compare(a: Cohort, b: Cohort) {
    return a.name.localeCompare(b.name);
  }
}
class CohortDescriptionComparator implements Comparator<Cohort> {
  compare(a: Cohort, b: Cohort) {
    return a.description.localeCompare(b.description);
  }
}
class NotebookNameComparator implements Comparator<FileDetail> {
  compare(a: FileDetail, b: FileDetail) {
    return a.name.localeCompare(b.name);
  }
}

enum Tabs {
  Cohorts,
  Notebooks,
}

@Component({
  styleUrls: ['../../styles/buttons.css',
    '../../styles/headers.css',
    '../../styles/cards.css',
    './component.css'],
  templateUrl: './component.html',
})
export class WorkspaceComponent implements OnInit, OnDestroy {
  @ViewChild(WorkspaceShareComponent)
  shareModal: WorkspaceShareComponent;


  Tabs = Tabs;

  cohortNameFilter = new CohortNameFilter();
  cohortDescriptionFilter = new CohortDescriptionFilter();
  notebookNameFilter = new NotebookNameFilter();
  cohortNameComparator = new CohortNameComparator();
  cohortDescriptionComparator = new CohortDescriptionComparator();
  notebookNameComparator = new NotebookNameComparator();

  greeting: string;
  workspace: Workspace;
  wsId: string;
  wsNamespace: string;
  awaitingReview = false;
  cohortsLoading = true;
  cohortsError = false;
  cohortList: Cohort[] = [];
  private accessLevel: WorkspaceAccessLevel;
  notebooksLoading = true;
  notebookError = false;
  notebookList: FileDetail[] = [];
  notebookAuthListeners: EventListenerOrEventListenerObject[] = [];
  tabOpen = Tabs.Notebooks;

  @ViewChild(BugReportComponent)
  bugReportComponent: BugReportComponent;

  constructor(
    private route: ActivatedRoute,
    private cohortsService: CohortsService,
    private http: Http,
    private router: Router,
    private signInService: SignInService,
    private workspacesService: WorkspacesService,
  ) {
    const wsData: WorkspaceData = this.route.snapshot.data.workspace;
    this.workspace = wsData;
    this.accessLevel = wsData.accessLevel;
    const {approved, reviewRequested} = this.workspace.researchPurpose;
    this.awaitingReview = reviewRequested && !approved;
  }

  ngOnInit(): void {
    this.wsNamespace = this.route.snapshot.params['ns'];
    this.wsId = this.route.snapshot.params['wsid'];
    this.cohortsService.getCohortsInWorkspace(this.wsNamespace, this.wsId)
      .subscribe(
        cohortsReceived => {
          for (const coho of cohortsReceived.items) {
            this.cohortList.push(coho);
          }
          this.cohortsLoading = false;
        },
        error => {
          this.cohortsLoading = false;
          this.cohortsError = true;
        });
    this.loadNotebookList();

    if (this.newWorkspace) {
      this.greeting = 'Get Started';
    } else {
      this.greeting = 'Recent Work';
    }
  }

  private loadNotebookList() {
    this.workspacesService.getNoteBookList(this.wsNamespace, this.wsId)
      .subscribe(
        fileList => {
          this.notebookList = fileList;
          this.notebooksLoading = false;
        },
        error => {
          this.notebooksLoading = false;
          this.notebookError = true;
        });
  }

  ngOnDestroy(): void {
    this.notebookAuthListeners.forEach(f => window.removeEventListener('message', f));
  }

  newNotebook(): void {
    this.openNotebook();
  }

  openNotebook(nb?: FileDetail): void {
    let nbUrl = `/workspaces/${this.workspace.namespace}/${this.workspace.id}/notebooks/`;
    if (nb) {
      nbUrl += encodeURIComponent(nb.name);
    } else {
      nbUrl += 'create';
    }
    const notebook = window.open(nbUrl, '_blank');

    // TODO(RW-474): Remove the authHandler integration. This is messy,
    // non-standard, and currently will break in the following situation:
    // - User opens a new notebook tab.
    // - While that tab is loading, user immediately navigates away from this
    //   page.
    // This is not easily fixed without leaking listeners outside the lifespan
    // of the workspace component.
    const authHandler = (e: MessageEvent) => {
      if (e.source !== notebook) {
        return;
      }
      if (e.origin !== environment.leoApiUrl) {
        return;
      }
      if (e.data.type !== 'bootstrap-auth.request') {
        return;
      }
      notebook.postMessage({
        'type': 'bootstrap-auth.response',
        'body': {
          'googleClientId': this.signInService.clientId
        }
      }, environment.leoApiUrl);
    };
    window.addEventListener('message', authHandler);
    this.notebookAuthListeners.push(authHandler);
  }

  buildCohort(): void {
    if (!this.awaitingReview) {
      this.router.navigate(['cohorts', 'build'], {relativeTo: this.route});
    }
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

  get newWorkspace(): boolean {
    console.log("In new workspace query");
    console.log("cohort list length" + this.cohortList.length);
    console.log("notebookList length" + this.notebookList.length);
    return this.cohortList.length === 0 && this.notebookList.length === 0);
  }

  share(): void {
    this.shareModal.open();
  }

  submitNotebooksLoadBugReport(): void {
    this.notebookError = false;
    this.bugReportComponent.reportBug();
    this.bugReportComponent.bugReport.shortDescription = 'Could not load notebooks';
  }
}
