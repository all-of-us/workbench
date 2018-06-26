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
import {
  JupyterService,
  NotebooksService,
} from 'notebooks-generated';


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

  workspace: Workspace;
  wsId: string;
  wsNamespace: string;
  awaitingReview = false;
  cohortsLoading = true;
  cohortsError = false;
  notebookError = false;
  notebooksLoading = true;
  cohortList: Cohort[] = [];
  private pollClusterTimer: NodeJS.Timer;
  cluster: Cluster;
  clusterLoading = true;
  clusterLongWait = false;
  clusterPulled = false;
  launchedNotebookName: string;
  private clusterLocalDirectory: string;
  private accessLevel: WorkspaceAccessLevel;
  deleting = false;
  showAlerts = false;
  notebookList: FileDetail[] = [];
  notebookAuthListeners: EventListenerOrEventListenerObject[] = [];
  alertCategory: string;
  alertMsg: string;
  tabOpen = Tabs.Notebooks;
  localizeNotebooksError = false;

  @ViewChild(BugReportComponent)
  bugReportComponent: BugReportComponent;

  constructor(
    private route: ActivatedRoute,
    private cohortsService: CohortsService,
    private clusterService: ClusterService,
    private http: Http,
    private router: Router,
    private signInService: SignInService,
    private workspacesService: WorkspacesService,
    private leoNotebooksService: NotebooksService,
    private jupyterService: JupyterService,
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
    this.initCluster();
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
          this.notebookError = false;
        });
  }

  ngOnDestroy(): void {
    this.notebookAuthListeners.forEach(f => window.removeEventListener('message', f));
    if (this.pollClusterTimer) {
      clearTimeout(this.pollClusterTimer);
    }
  }

  openNotebook(notebook: any): void {
    if (this.clusterLoading) {
      return;
    }
    this.localizeNotebooks([notebook]).subscribe(() => {
      this.launchedNotebookName = notebook.name;
      this.clusterPulled = true;
    }, () => {
      this.localizeNotebooksError = true;
    });
  }

  newNotebook(): void {
    if (this.clusterLoading) {
      return;
    }
    this.localizeNotebooks([]).subscribe(() => {
      // Use the Jupyter Server API directly to create a new notebook. This
      // API handles notebook name collisions and matches the behavior of
      // clicking "new notebook" in the Jupyter UI.

      // The Jupyter Swagger already contains the "/workspaces/" infix.
      const workspaceDir = this.clusterLocalDirectory.replace(/^workspaces\//, '');
      this.jupyterService.postContents(
        this.cluster.clusterNamespace, this.cluster.clusterName,
        workspaceDir, {
          'type': 'notebook'
        }).subscribe((resp) => {
          // TODO(calbach): Going through this modal is a temporary hack to avoid
          // triggering pop-up blockers. Likely we'll want to switch notebook
          // cluster opening to go through a redirect URL to make the localize and
          // redirect look more atomic to the browser. Once this is in place, rm the
          // modal and this hacky passing of the launched notebook name.
          this.launchedNotebookName = resp.name;
          this.clusterPulled = true;
          // Reload the notebook list to get the newly created notebook.
          this.loadNotebookList();
        });
    }, () => {
      this.localizeNotebooksError = true;
    });
  }

  private initCluster(): void {
    this.pollCluster().subscribe((c) => {
      // Use lower level *withHttpInfo() method to work around
      // https://github.com/DataBiosphere/leonardo/issues/444
      this.leoNotebooksService.setCookieWithHttpInfo(c.clusterNamespace, c.clusterName, {
        withCredentials: true
      }).subscribe(() => {
          this.clusterLoading = false;
        });
    });
  }

  private pollCluster(): Observable<Cluster> {
    return new Observable<Cluster>(observer => {
      // Repoll every 15s if not ready.
      const repoll = () => {
        this.pollClusterTimer = setTimeout(() => {
          this.pollCluster().subscribe(c => {
            observer.next(c);
            observer.complete();
          });
        }, 15000);
      };
      this.clusterService.listClusters()
        .subscribe((resp) => {
          this.cluster = resp.defaultCluster;
          if (this.cluster.status !== ClusterStatus.Running) {
            // Once cluster creation has started, it may take ~5 minutes.
            this.clusterLongWait = true;
            repoll();
          } else {
            this.clusterLongWait = false;
            observer.next(this.cluster);
            observer.complete();
          }
          // Repoll on errors.
        }, repoll);
    });
  }

  cancelCluster(): void {
    this.launchedNotebookName = '';
    this.clusterPulled = false;
  }

  openCluster(notebookName?: string): void {
    let leoNotebookUrl = environment.leoApiUrl + '/notebooks/'
      + this.cluster.clusterNamespace + '/'
      + this.cluster.clusterName;
    if (notebookName) {
      leoNotebookUrl = [
        leoNotebookUrl, 'notebooks', this.clusterLocalDirectory, notebookName
      ].join('/');
    } else {
      leoNotebookUrl = [
        leoNotebookUrl, 'tree', this.clusterLocalDirectory
      ].join('/');
    }

    const notebook = window.open(leoNotebookUrl, '_blank');
    this.launchedNotebookName = '';
    this.clusterPulled = false;

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

  private localizeNotebooks(notebooks: any): Observable<void> {
    const names: Array<string> = notebooks.map(n => n.name);
    return new Observable<void>(obs => {
      this.clusterService
        .localize(this.cluster.clusterNamespace, this.cluster.clusterName, {
          workspaceNamespace: this.workspace.namespace,
          workspaceId: this.workspace.id,
          notebookNames: names
        })
        .subscribe((resp) => {
          this.clusterLocalDirectory = resp.clusterLocalDirectory;
          obs.next();
          obs.complete();
        }, (err) => {
          this.handleLocalizeError();
          setTimeout(() => {
            this.resetAlerts();
          }, 5000);
          obs.error(err);
        });
    });
  }

  handleLocalizeError(): void {
    this.alertCategory = 'alert-danger';
    this.alertMsg = 'There was an issue while saving file(s) please try again later';
    this.showAlerts = true;
  }

  resetAlerts(): void {
    this.alertCategory = '';
    this.alertMsg = '';
    this.showAlerts = false;
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

  share(): void {
    this.shareModal.open();
  }

  submitNotebooksLoadBugReport(): void {
    this.notebookError = false;
    this.bugReportComponent.reportBug();
    this.bugReportComponent.bugReport.shortDescription = 'Could not load notebooks';
  }

  submitNotebookLocalizeBugReport(): void {
    this.localizeNotebooksError = false;
    this.bugReportComponent.reportBug();
    this.bugReportComponent.bugReport.shortDescription = 'Could not localize notebook.';
  }
}
