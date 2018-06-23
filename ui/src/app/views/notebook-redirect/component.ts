import {Component, OnDestroy, OnInit} from '@angular/core';
import {Headers, Http, Response} from '@angular/http';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {SignInService} from 'app/services/sign-in.service';

import {
  Cluster,
  ClusterService,
  ClusterStatus,
} from 'generated';
import {
  JupyterService,
  NotebooksService,
} from 'notebooks-generated';

enum Progress {
  Unknown,
  Initializing,
  Configuring,
  Redirecting
}

@Component({
  styleUrls: ['../../styles/buttons.css',
              '../../styles/cards.css',
              '../../styles/headers.css',
              '../../styles/inputs.css',
              './component.css'],
  templateUrl: './component.html',
})
export class NotebookRedirectComponent implements OnInit, OnDestroy {
  Progress = Progress;

  progress = Progress.Unknown;
  notebookName: string;

  private wsId: string;
  private wsNamespace: string;
  private loadingSub: Subscription;
  private cluster: Cluster;

  constructor(
    private route: ActivatedRoute,
    private clusterService: ClusterService,
    private signInService: SignInService,
    private leoNotebooksService: NotebooksService,
    private jupyterService: JupyterService,
    private http: Http) {}

  ngOnInit(): void {
    this.wsNamespace = this.route.snapshot.params['ns'];
    this.wsId = this.route.snapshot.params['wsid'];
    this.notebookName = this.route.snapshot.params['nbName'];

    this.loadingSub = this.clusterService.listClusters()
      .flatMap((resp) => {
        const c = resp.defaultCluster;
        // TODO:
        //  - Resume here if suspended. Throw an error from the resume Obs.
        //  - Fail if the cluster enters permanent error mode.
        if (c.status === ClusterStatus.Running) {
          return Observable.from([c]);
        }
        this.progress = Progress.Initializing;
        throw Error(`cluster has status ${c.status}`);
      })
      .retryWhen(errs => errs.delay(10000))
      .do((c) => {
        this.cluster = c;
        this.progress = Progress.Configuring;
      })
      .flatMap(c => this.initializeNotebookCookies(c))
      .flatMap(c => {
        // This will contain the Jupyter-local path to the localized notebook.
        if (this.notebookName) {
          return this.localizeNotebooks([this.notebookName])
            .map(localDir => `${localDir}/${this.notebookName}`);
        }
        return this.newNotebook();
      })
      .subscribe((nbName) => {
        this.progress = Progress.Redirecting;
        window.location.href = `${this.clusterUrl(this.cluster)}/notebooks/${nbName}`;
      });
  }

  ngOnDestroy(): void {
    if (this.loadingSub) {
      this.loadingSub.unsubscribe();
    }
  }

  private clusterUrl(cluster: Cluster): string {
    return environment.leoApiUrl + '/notebooks/'
      + cluster.clusterNamespace + '/'
      + cluster.clusterName;
  }

  private initializeNotebookCookies(cluster: Cluster): Observable<Cluster> {
    return this.leoNotebooksService.setCookieWithHttpInfo(c.clusterNamespace, c.clusterName, {
      withCredentials: true
    }).map(_ => cluster);
  }

  private newNotebook(): Observable<string> {
    return this.localizeNotebooks([]).flatMap((localDir) => {
      // Use the Jupyter Server API directly to create a new notebook. This
      // API handles notebook name collisions and matches the behavior of
      // clicking "new notebook" in the Jupyter UI.
      const workspaceDir = this.clusterLocalDirectory.replace(/^workspaces\//, '');
      return this.jupyterService.postContents(
        this.cluster.clusterNamespace, this.cluster.clusterName,
        workspaceDir, {
          'type': 'notebook'
        }).map(resp => `${localDir}/${resp.json().name}`);
    });
  }

  private localizeNotebooks(notebookNames: Array<string>): Observable<string> {
    return this.clusterService
      .localize(this.cluster.clusterNamespace, this.cluster.clusterName, {
        workspaceNamespace: this.wsNamespace,
        workspaceId: this.wsId,
        notebookNames: notebookNames
      })
      .map(resp => resp.clusterLocalDirectory);
  }
}
