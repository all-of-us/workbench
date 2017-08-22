import {Component, OnInit, Inject} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';
import {StringFilter, Comparator} from 'clarity-angular';
import {DOCUMENT} from '@angular/platform-browser';

import {Cohort} from 'generated';
import {CohortsService} from 'generated';
import {resetDateObject} from 'helper-functions';
import {Repository} from 'app/models/repository';
import {RepositoryService} from 'app/services/repository.service';
import {User} from 'app/models/user';
import {UserService} from 'app/services/user.service';
import {Workspace} from 'generated';
import {WorkspacesService} from 'generated';
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
  public static DEFAULT_WORKSPACE_NS = 'defaultNamespace';
  public static DEFAULT_WORKSPACE_NAME = 'defaultWorkspace';
  public static DEFAULT_WORKSPACE_ID = '1';
  private cohortNameFilter = new CohortNameFilter();
  private cohortDescriptionFilter = new CohortDescriptionFilter();
  private notebookNameFilter = new NotebookNameFilter();
  private notebookDescriptionFilter = new NotebookDescriptionFilter();
  private cohortNameComparator = new CohortNameComparator();
  private cohortDescriptionComparator = new CohortDescriptionComparator();
  private notebookNameComparator = new NotebookNameComparator();
  private notebookDescriptionComparator = new NotebookDescriptionComparator();
  workspace: Workspace;
  wsId: string;
  wsNamespace: string;
  workspaceLoading = true;
  cohortsLoading = true;
  cohortsError = false;
  notebooksLoading = false;
  repositories: Repository[] = [];
  user: User;  // to detect if logged in
  cohortList: Cohort[] = [];
  // TODO: Replace with real data/notebooks
  notebookList = [new Notebook('Notebook 1',
                    'This is the user defined description for notebook 1',
                    '/cohort/notebook1'),
                  new Notebook('Notebook 2',
                    'This is the user defined description for notebook 2',
                    '/cohort/notebook2')];
  constructor(
      private router: Router,
      private route: ActivatedRoute,
      private userService: UserService,
      private repositoryService: RepositoryService,
      private cohortsService: CohortsService,
      private workspacesService: WorkspacesService,
      @Inject(DOCUMENT) private document: any
  ) {}
  ngOnInit(): void {
    this.userService.getLoggedInUser().then(user => this.user = user);
    this.workspaceLoading = true;
    this.wsNamespace = this.route.snapshot.url[1].path;
    this.wsId = this.route.snapshot.url[2].path;
    this.cohortsService
        .getCohortsInWorkspace(
            this.wsNamespace, this.wsId)
        .retry(2)
        .subscribe(
            cohortsReceived => {
              for (const coho of cohortsReceived.items) {
                coho.creationTime = resetDateObject(coho.creationTime);
                coho.lastModifiedTime = resetDateObject(coho.lastModifiedTime);
                this.cohortList.push(coho);
              }
              this.cohortsLoading = false;
            },
            error => {
              this.cohortsLoading = false;
              this.cohortsError = true;
            });
    this.workspacesService
      .getWorkspace(
        this.wsNamespace, this.wsId)
        .retry(2)
        .subscribe(
          workspaceReceived => {
            this.workspace = workspaceReceived;
            this.workspaceLoading = false;
          },
          error => {
            this.workspaceLoading = false;
          });
  }
}
