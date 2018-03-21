import {Component, Inject, OnDestroy, OnInit} from '@angular/core';
import {Headers, Http, Response} from '@angular/http';
import {DOCUMENT} from '@angular/platform-browser';
import {ActivatedRoute, Router} from '@angular/router';
import {Comparator, StringFilter} from '@clr/angular';
import {Observable} from 'rxjs/Observable';

import {WorkspaceData} from 'app/resolvers/workspace';
import {SignInService} from 'app/services/sign-in.service';

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


@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class WorkspaceComponent implements OnInit, OnDestroy {
  // Keep in sync with api/src/main/resources/notebooks.yaml.
  private static readonly leoBaseUrl = 'https://notebooks.firecloud.org';

  private cohortNameFilter = new CohortNameFilter();
  private cohortDescriptionFilter = new CohortDescriptionFilter();
  private notebookNameFilter = new NotebookNameFilter();
  private cohortNameComparator = new CohortNameComparator();
  private cohortDescriptionComparator = new CohortDescriptionComparator();
  private notebookNameComparator = new NotebookNameComparator();

  workspace: Workspace;
  wsId: string;
  wsNamespace: string;
  cohortsLoading = true;
  cohortsError = false;
  notebookError = false;
  notebooksLoading = false;
  cohortList: Cohort[] = [];
  cluster: Cluster;
  clusterLoading = true;
  clusterPulled = false;
  private clusterLocalDirectory: string;
  private launchedNotebookName: string;
  private accessLevel: WorkspaceAccessLevel;
  deleting = false;
  showAlerts = false;
  notebookList: FileDetail[] = [];
  editHover = false;
  shareHover = false;
  trashHover = false;
  listenerAdded = false;
  notebookAuthListener: EventListenerOrEventListenerObject;
  alertCategory: string;
  alertMsg: string;

  constructor(
      private route: ActivatedRoute,
      private cohortsService: CohortsService,
      private clusterService: ClusterService,
      private http: Http,
      private router: Router,
      private signInService: SignInService,
      private workspacesService: WorkspacesService,
      @Inject(DOCUMENT) private document: any
  ) {
    const wsData: WorkspaceData = this.route.snapshot.data.workspace;
    this.workspace = wsData;
    this.accessLevel = wsData.accessLevel;
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
    this.workspacesService.getNoteBookList(this.wsNamespace, this.wsId)
      .subscribe(
        fileList => {
          this.notebookList = fileList;
        },
        error => {
          this.notebooksLoading = false;
          this.notebookError = false;
        });
    this.initCluster();
  }

  ngOnDestroy(): void {
    window.removeEventListener('message', this.notebookAuthListener);
  }

  launchNotebook(notebook): void {
    if (this.clusterLoading) {
      return;
    }
    this.localizeNotebooks([notebook]).subscribe(() => {
      this.launchedNotebookName = notebook.name;
      this.clusterPulled = true;
    });
  }

  launchCluster(): void {
    if (this.clusterLoading) {
      return;
    }
    this.localizeNotebooks(this.notebookList).subscribe(() => {
      // TODO(calbach): Going through this modal is a temporary hack to avoid
      // triggering pop-up blockers. Likely we'll want to switch notebook
      // cluster opening to go through a redirect URL to make the localize and
      // redirect look more atomic to the browser. Once this is in place, rm the
      // modal and this hacky passing of the launched notebook name.
      this.launchedNotebookName = '';
      this.clusterPulled = true;
    });
  }

  private initCluster(): void {
    this.pollCluster().subscribe((c) => {
      this.initializeNotebookCookies(c).subscribe(() => {
        this.clusterLoading = false;
        this.cluster = c;
      });
    });
  }

  private initializeNotebookCookies(cluster: Cluster): Observable<Response> {
    // TODO(calbach): Generate the FC notebook Typescript client and call here.
    const leoNotebookUrl = WorkspaceComponent.leoBaseUrl + '/notebooks/'
        + cluster.clusterNamespace + '/'
      + cluster.clusterName;
    const leoSetCookieUrl = leoNotebookUrl + '/setCookie';

    const headers = new Headers();
    headers.append('Authorization', 'Bearer ' + this.signInService.currentAccessToken);
    return this.http.get(leoSetCookieUrl, {
      headers: headers,
      withCredentials: true
    });
  }

  private pollCluster(): Observable<Cluster> {
    // Polls for cluster startup every 10s.
    return new Observable<Cluster>(observer => {
      this.clusterService.listClusters()
        .subscribe((resp) => {
          const cluster = resp.defaultCluster;
          if (cluster.status !== ClusterStatus.RUNNING) {
            setTimeout(() => {
              this.pollCluster().subscribe(newCluster => {
                this.cluster = newCluster;
                observer.next(newCluster);
                observer.complete();
              });
            }, 10000);
          } else {
            observer.next(cluster);
            observer.complete();
          }
      });
    });
  }

  cancelCluster(): void {
    this.launchedNotebookName = '';
    this.clusterPulled = false;
  }

  openCluster(notebookName?: string): void {
    let leoNotebookUrl = WorkspaceComponent.leoBaseUrl + '/notebooks/'
      + this.cluster.clusterNamespace + '/'
      + this.cluster.clusterName;
    if (notebookName) {
      leoNotebookUrl = [
        leoNotebookUrl, 'edit', this.clusterLocalDirectory, notebookName
      ].join('/');
    } else {
      // TODO(calbach): If lacking a notebook name, should create a new notebook instead.
      leoNotebookUrl = [
        leoNotebookUrl, 'tree', this.clusterLocalDirectory
      ].join('/');
    }

    const notebook = window.open(leoNotebookUrl, '_blank');
    this.launchedNotebookName = '';
    this.clusterPulled = false;
    // TODO (blrubenstein): Make the notebook page a list of pages, and
    //    move this to component scope.
    if (!this.listenerAdded) {
      this.notebookAuthListener = (e: MessageEvent) => {
        if (e.source !== notebook) {
          return;
        }
        if (e.origin !== WorkspaceComponent.leoBaseUrl) {
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
        }, WorkspaceComponent.leoBaseUrl);
      };
      window.addEventListener('message', this.notebookAuthListener);
      this.listenerAdded = true;
    }
  }

  edit(): void {
    this.router.navigate(['edit'], {relativeTo : this.route});
  }

  clone(): void {
    this.router.navigate(['clone'], {relativeTo : this.route});
  }

  share(): void {
    this.router.navigate(['share'], {relativeTo : this.route});
  }

  delete(): void {
    this.deleting = true;
    this.workspacesService.deleteWorkspace(
        this.workspace.namespace, this.workspace.id).subscribe(() => {
          this.router.navigate(['/']);
        });
  }

  get writePermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER
        || this.accessLevel === WorkspaceAccessLevel.WRITER;
  }

  get ownerPermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER;
  }

  private localizeNotebooks(notebooks): Observable<void> {
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
}
