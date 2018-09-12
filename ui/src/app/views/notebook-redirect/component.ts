import {Location} from '@angular/common';
import {Component, Inject, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {timer} from 'rxjs/observable/timer';
import {mapTo} from 'rxjs/operators';
import {Subscription} from 'rxjs/Subscription';

import {WINDOW_REF} from 'app/utils';
import {Kernels} from 'app/utils/notebook-kernels';
import {environment} from 'environments/environment';

import {
  Cluster,
  ClusterService,
  ClusterStatus,
} from 'generated';
import {
  ClusterService as LeoClusterService,
  JupyterService,
  NotebooksService,
} from 'notebooks-generated';

enum Progress {
  Unknown,
  Initializing,
  Resuming,
  Authenticating,
  Copying,
  Creating,
  Redirecting
}

const commonNotebookFormat = {
  'cells': [
    {
      'cell_type': 'code',
      'execution_count': null,
      'metadata': {},
      'outputs': [],
      'source': []
    }
  ],
  metadata: {},
  'nbformat': 4,
  'nbformat_minor': 2
}

const rNotebookMetadata = {
  'kernelspec': {
    'display_name': 'R',
    'language': 'R',
    'name': 'ir'
  },
  'language_info': {
    'codemirror_mode': 'r',
    'file_extension': '.r',
    'mimetype': 'text/x-r-source',
    'name': 'R',
    'pygments_lexer': 'r',
    'version': '3.4.4'
  }
};

const pyNotebookMetadata = {
  'kernelspec': {
    'display_name': 'Python 3',
    'language': 'python',
    'name': 'python3'
  },
  'language_info': {
    'codemirror_mode': {
      'name': 'ipython',
      'version': 3
    },
    'file_extension': '.py',
    'mimetype': 'text/x-python',
    'name': 'python',
    'nbconvert_exporter': 'python',
    'pygments_lexer': 'ipython3',
    'version': '3.4.2'
  }
};

@Component({
  styleUrls: ['../../styles/buttons.css',
              '../../styles/cards.css',
              '../../styles/headers.css',
              '../../styles/inputs.css',
              './component.css'],
  templateUrl: './component.html',
})
export class NotebookRedirectComponent implements OnInit, OnDestroy {


  fileContent: any;

  Progress = Progress;

  progress = Progress.Unknown;
  notebookName: string;

  creating: boolean;

  private wsId: string;
  private wsNamespace: string;
  private loadingSub: Subscription;
  private cluster: Cluster;

  constructor(
    @Inject(WINDOW_REF) private window: Window,
    private route: ActivatedRoute,
    private clusterService: ClusterService,
    private leoClusterService: LeoClusterService,
    private leoNotebooksService: NotebooksService,
    private jupyterService: JupyterService,
  ) {}

  ngOnInit(): void {
    this.wsNamespace = this.route.snapshot.params['ns'];
    this.wsId = this.route.snapshot.params['wsid'];
    this.creating = this.route.snapshot.data.creating;

    if (this.creating) {
      this.notebookName = this.route.snapshot.queryParamMap.get('notebook-name');
      this.fileContent = commonNotebookFormat;
      if (this.route.snapshot.queryParamMap.get('kernel-type') === Kernels.R.toString()) {
        this.fileContent.metadata = rNotebookMetadata;
      } else {
        this.fileContent.metadata = pyNotebookMetadata;
      }
    } else {
      this.notebookName = this.route.snapshot.params['nbName'];
    }

    this.loadingSub = this.clusterService.listClusters()
      .flatMap((resp) => {
        const c = resp.defaultCluster;
        if (c.status === ClusterStatus.Starting ||
            c.status === ClusterStatus.Stopping ||
            c.status === ClusterStatus.Stopped) {
          this.progress = Progress.Resuming;
        } else {
          this.progress = Progress.Initializing;
        }

        if (c.status === ClusterStatus.Running) {
          return Observable.from([c]);
        }
        if (c.status === ClusterStatus.Stopped) {
          // Resume the cluster and continue polling.
          return <Observable<Cluster>> this.leoClusterService
            .startCluster(c.clusterNamespace, c.clusterName)
            .flatMap(_ => {
              throw Error('resuming');
            });
        }
        throw Error(`cluster has status ${c.status}`);
      })
      .retryWhen(errs => this.clusterRetryDelay(errs))
      .do((c) => {
        this.cluster = c;
        this.progress = Progress.Authenticating;
      })
      .flatMap(c => this.initializeNotebookCookies(c))
      .flatMap(c => {
        let localizeObs: Observable<string>;
        // This will contain the Jupyter-local path to the localized notebook.
        if (!this.creating) {
          this.progress = Progress.Copying;
          localizeObs = this.localizeNotebooks([this.notebookName])
            .map(localDir => `${localDir}/${this.notebookName}`);
        } else {
          this.progress = Progress.Creating;
          localizeObs = this.newNotebook();
        }
        // The cluster may be running, but we've observed some 504s on localize
        // right after it comes up. Retry here to mitigate that. The retry must
        // be on this inner observable to prevent resubscribing to upstream
        // observables (we just want to retry localization).
        return localizeObs.retry(3);
      })
      .subscribe((nbName) => {
        this.progress = Progress.Redirecting;
        this.window.location.href = this.notebookUrl(this.cluster, nbName);
      });
  }

  ngOnDestroy(): void {
    if (this.loadingSub) {
      this.loadingSub.unsubscribe();
    }
  }

  private clusterRetryDelay(errs: Observable<Error>) {
    // Ideally we'd just call .delay(10000), but that doesn't work in
    // combination with fakeAsync(). This is a workaround for
    // https://github.com/angular/angular/issues/10127
    return errs.switchMap(v => timer(10000).pipe(mapTo(v)));
  }

  private notebookUrl(cluster: Cluster, nbName: string): string {
    return encodeURI(
      environment.leoApiUrl + '/notebooks/'
        + cluster.clusterNamespace + '/'
        + cluster.clusterName + '/notebooks/' + nbName);
  }

  private initializeNotebookCookies(c: Cluster): Observable<Cluster> {
    return this.leoNotebooksService.setCookieWithHttpInfo(c.clusterNamespace, c.clusterName, {
      withCredentials: true
    }).map(_ => c);
  }

  private newNotebook(): Observable<string> {
    return this.localizeNotebooks([]).flatMap((localDir) => {
      // Use the Jupyter Server API directly to create a new notebook. This
      // API handles notebook name collisions and matches the behavior of
      // clicking 'new notebook' in the Jupyter UI.
      const workspaceDir = localDir.replace(/^workspaces\//, '');
      return this.jupyterService.putContents(
        this.cluster.clusterNamespace, this.cluster.clusterName,
        workspaceDir, this.notebookName + '.ipynb', {
          'type': 'file',
          'format': 'text',
          'content': JSON.stringify(this.fileContent)
        }).map(resp => `${localDir}/${resp.name}`);
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
