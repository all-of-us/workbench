import {Location} from '@angular/common';
import {Component, OnDestroy, OnInit} from '@angular/core';
import {DomSanitizer, SafeResourceUrl} from '@angular/platform-browser';
import {Observable} from 'rxjs/Observable';
import {timer} from 'rxjs/observable/timer';
import {mapTo} from 'rxjs/operators';
import {Subscription} from 'rxjs/Subscription';

import {
  queryParamsStore,
  urlParamsStore
} from 'app/utils/navigation';
import {Kernels} from 'app/utils/notebook-kernels';
import {environment} from 'environments/environment';

import {appendNotebookFileSuffix, dropNotebookFileSuffix} from 'app/pages/analysis/util';
import {
  Cluster,
  ClusterService,
  ClusterStatus
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
  Redirecting,
  Loaded
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
};

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
  styleUrls: ['../../../styles/buttons.css',
    '../../../styles/cards.css',
    '../../../styles/headers.css',
    '../../../styles/inputs.css',
    './component.css'],
  templateUrl: './component.html',
})
export class NotebookRedirectComponent implements OnInit, OnDestroy {

  Progress = Progress;

  progress = Progress.Unknown;
  notebookName: string;
  fullNotebookName: string;

  creating: boolean;

  leoUrl: SafeResourceUrl;

  private wsId: string;
  private wsNamespace: string;
  private loadingSub: Subscription;
  private cluster: Cluster;
  private progressComplete = new Map<Progress, boolean>();
  private playground = false;

  constructor(
    private locationService: Location,
    private clusterService: ClusterService,
    private leoClusterService: LeoClusterService,
    private leoNotebooksService: NotebooksService,
    private jupyterService: JupyterService,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit(): void {
    const {ns, wsid} = urlParamsStore.getValue();
    const {creating, playgroundMode} = queryParamsStore.getValue();
    this.wsNamespace = ns;
    this.wsId = wsid;
    this.creating = creating || false;
    this.playground = playgroundMode === 'true';
    this.setNotebookNames();

    let initializedProgress = false;
    this.loadingSub = this.clusterService.listClusters(this.wsNamespace, this.wsId)
      .do((resp) => {
        if (initializedProgress) {
          // Only initialize progress once. This callback will re-execute as we
          // poll the cluster's status.
          return;
        }
        const c = resp.defaultCluster;
        if (c.status === ClusterStatus.Running) {
          this.incrementProgress(Progress.Unknown);
        } else if (c.status === ClusterStatus.Starting ||
            c.status === ClusterStatus.Stopping ||
            c.status === ClusterStatus.Stopped) {
          this.incrementProgress(Progress.Resuming);
        } else {
          this.incrementProgress(Progress.Initializing);
        }
        initializedProgress = true;
      })
      .flatMap((resp) => {
        const c = resp.defaultCluster;
        if (c.status === ClusterStatus.Running) {
          return Observable.from([c]);
        } else if (c.status === ClusterStatus.Stopped) {
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
        this.incrementProgress(Progress.Authenticating);
      })
      .flatMap(c => this.initializeNotebookCookies(c))
      .flatMap(c => {
        let localizeObs: Observable<string>;
        // This will contain the Jupyter-local path to the localized notebook.
        if (!this.creating) {
          this.incrementProgress(Progress.Copying);
          localizeObs = this.localizeNotebooks([this.fullNotebookName],
            this.playground)
            .map(localDir => `${localDir}/${this.fullNotebookName}`);
        } else {
          this.incrementProgress(Progress.Creating);
          localizeObs = this.newNotebook();
        }
        // The cluster may be running, but we've observed some 504s on localize
        // right after it comes up. Retry here to mitigate that. The retry must
        // be on this inner observable to prevent resubscribing to upstream
        // observables (we just want to retry localization).
        return localizeObs.retry(3);
      })
      .subscribe((nbName) => {
        this.incrementProgress(Progress.Redirecting);
        if (this.creating) {
          window.history.replaceState({}, 'Notebook', 'workspaces/' + this.wsNamespace +
          '/' + this.wsId + '/notebooks/' + encodeURIComponent(this.fullNotebookName));
        }
        this.leoUrl = this.sanitizer
          .bypassSecurityTrustResourceUrl(this.notebookUrl(this.cluster, nbName));

        // Angular 2 only provides a load hook for iFrames
        // the load hook triggers on url definition, not on completion of url load
        // so instead just giving it a sec to "redirect"
        setTimeout(() => {
          this.incrementProgress(Progress.Loaded);
        }, 1000);
      });
  }

  ngOnDestroy(): void {
    if (this.loadingSub) {
      this.loadingSub.unsubscribe();
    }
  }

  // this maybe overkill, but should handle all situations
  setNotebookNames(): void {
    const {nbName} = urlParamsStore.getValue();
    this.notebookName = dropNotebookFileSuffix(decodeURIComponent(nbName));
    this.fullNotebookName = appendNotebookFileSuffix(this.notebookName);
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
    const fileContent = commonNotebookFormat;
    const {kernelType} = queryParamsStore.getValue();
    if (kernelType === Kernels.R.toString()) {
      fileContent.metadata = rNotebookMetadata;
    } else {
      fileContent.metadata = pyNotebookMetadata;
    }
    return this.localizeNotebooks([], false).flatMap((localDir) => {
      // Use the Jupyter Server API directly to create a new notebook. This
      // API handles notebook name collisions and matches the behavior of
      // clicking 'new notebook' in the Jupyter UI.
      const workspaceDir = localDir.replace(/^workspaces\//, '');
      return this.jupyterService.putContents(
        this.cluster.clusterNamespace, this.cluster.clusterName,
        workspaceDir, appendNotebookFileSuffix(this.notebookName), {
          'type': 'file',
          'format': 'text',
          'content': JSON.stringify(fileContent)
        }).map(resp => `${localDir}/${resp.name}`);
    });
  }

  private localizeNotebooks(notebookNames: Array<string>,
    playgroundMode: boolean): Observable<string> {
    return this.clusterService
      .localize(this.cluster.clusterNamespace, this.cluster.clusterName, {
        workspaceNamespace: this.wsNamespace,
        workspaceId: this.wsId,
        notebookNames: notebookNames,
        playgroundMode: playgroundMode
      })
      .map(resp => resp.clusterLocalDirectory);
  }

  navigateBack(): void {
    this.locationService.back();
  }

  private incrementProgress(p: Progress): void {
    this.progressComplete[p] = true;
    this.progress = p;
  }
}
