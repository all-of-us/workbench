import {Component, Inject, OnInit} from '@angular/core';
import {DOCUMENT} from '@angular/platform-browser';
import {ActivatedRoute, Router} from '@angular/router';
import {Comparator, StringFilter} from 'clarity-angular';
import {Observable} from 'rxjs/Observable';

import {ErrorHandlingService} from 'app/services/error-handling.service';

import {
  Cluster,
  ClusterService,
  Cohort,
  CohortsService,
  Workspace,
  WorkspaceAccessLevel,
  WorkspacesService,
} from 'generated';

// TODO: use a real swagger generated class for this.
class Notebook {
  constructor(public name: string, public description: string, public url: string) {}
}
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
class NotebookNameFilter implements StringFilter<Notebook> {
  accepts(notebook: Notebook, search: string): boolean {
    return notebook.name.toLowerCase().indexOf(search) >= 0;
  }
}
class NotebookDescriptionFilter implements StringFilter<Notebook> {
  accepts(notebook: Notebook, search: string): boolean {
    return notebook.description.toLowerCase().indexOf(search) >= 0;
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
class NotebookNameComparator implements Comparator<Notebook> {
  compare(a: Notebook, b: Notebook) {
    return a.name.localeCompare(b.name);
  }
}
class NotebookDescriptionComparator implements Comparator<Notebook> {
  compare(a: Notebook, b: Notebook) {
    return a.description.localeCompare(b.description);
  }
}


@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class WorkspaceComponent implements OnInit {

  /* tslint:disable:no-unused-variable */
  /* All these are used in the template, not the class */
  private cohortNameFilter = new CohortNameFilter();
  private cohortDescriptionFilter = new CohortDescriptionFilter();
  private notebookNameFilter = new NotebookNameFilter();
  private notebookDescriptionFilter = new NotebookDescriptionFilter();
  private cohortNameComparator = new CohortNameComparator();
  private cohortDescriptionComparator = new CohortDescriptionComparator();
  private notebookNameComparator = new NotebookNameComparator();
  private notebookDescriptionComparator = new NotebookDescriptionComparator();
  /* tslint:enable:no-unused-variable */

  workspace: Workspace;
  wsId: string;
  wsNamespace: string;
  workspaceLoading = true;
  cohortsLoading = true;
  cohortsError = false;
  notebooksLoading = false;
  cohortList: Cohort[] = [];
  cluster: Cluster;
  clusterPulled = false;
  clusterLoading = false;
  notFound = false;
  accessLevel: WorkspaceAccessLevel;
  // TODO: Replace with real data/notebooks read in from GCS
  notebookList: Notebook[] = [];
  constructor(
      private router: Router,
      private route: ActivatedRoute,
      private cohortsService: CohortsService,
      private clusterService: ClusterService,
      private errorHandlingService: ErrorHandlingService,
      private workspacesService: WorkspacesService,
      /* tslint:disable-next-line:no-unused-variable */
      @Inject(DOCUMENT) private document: any
  ) {}
  ngOnInit(): void {
    this.workspaceLoading = true;
    this.wsNamespace = this.route.snapshot.params['ns'];
    this.wsId = this.route.snapshot.params['wsid'];

    this.errorHandlingService.retryApi(this.workspacesService
      .getWorkspace(this.wsNamespace, this.wsId))
        .subscribe(
          workspaceResponse => {
            this.workspace = workspaceResponse.workspace;
            this.accessLevel = workspaceResponse.accessLevel;
            this.workspaceLoading = false;
            this.errorHandlingService.retryApi(this.cohortsService
                .getCohortsInWorkspace(this.wsNamespace, this.wsId))
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
          },
          error => {
            if (error.status === 404) {
              this.notFound = true;
            } else {
              this.workspaceLoading = false;
            }
          });
  }

  launchNotebook(): void {
    this.errorHandlingService.retryApi(this.pollCluster()).subscribe(cluster => {
      this.cluster = cluster;
      this.clusterPulled = true;
    });
  }

  pollCluster(): Observable<Cluster> {
    // Polls for cluster startup every minute.
    const observable = new Observable(observer => {
      this.errorHandlingService.retryApi(this.clusterService.getCluster(
          this.workspace.namespace, this.workspace.id)).subscribe((cluster) => {
        if (cluster.status !== 'Running' && cluster.status !== 'Deleting') {
          setTimeout(() => {
              this.errorHandlingService.retryApi(this.pollCluster()).subscribe(newCluster => {
                this.cluster = newCluster;
                observer.next(newCluster);
                observer.complete();
              });
            }, 60000
          );
        } else {
          this.cluster = cluster;
          observer.next(cluster);
          observer.complete();
        }
      });
    });
    return observable;
  }

  cancelCluster(): void {
    this.clusterPulled = false;
  }

  openCluster(): void {
    const url = 'https://leonardo.dsde-dev.broadinstitute.org/notebooks/'
        + this.cluster.clusterNamespace + '/'
        + this.cluster.clusterName;
    window.location.href = url;
  }

  createAndLaunchNotebook(): void {
    this.clusterLoading = true;
    this.errorHandlingService.retryApi(this.clusterService
        .createCluster(this.workspace.namespace, this.workspace.id)).subscribe(() => {
      this.errorHandlingService.retryApi(this.pollCluster()).subscribe(polledCluster => {
        this.clusterLoading = false;
        this.cluster = polledCluster;
        this.clusterPulled = true;
      });
    });
  }

  killNotebook(): void {
    this.errorHandlingService.retryApi(this.clusterService.deleteCluster(
        this.workspace.namespace, this.workspace.id)).subscribe(() => {});
  }

  edit(): void {
    this.router.navigate(['edit'], {relativeTo : this.route});
  }

  share(): void {
    this.router.navigate(['share'], {relativeTo : this.route});
  }

  hasPermission(): boolean {
    return this.accessLevel === WorkspaceAccessLevel.OWNER
        || this.accessLevel === WorkspaceAccessLevel.WRITER;
  }
}
