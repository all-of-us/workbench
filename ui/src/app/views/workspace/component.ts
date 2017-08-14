import {Component, OnInit} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';
import {StringFilter} from 'clarity-angular';

import {Cohort} from 'generated';
import {CohortsService} from 'generated';
import {Repository} from 'app/models/repository';
import {RepositoryService} from 'app/services/repository.service';
import {User} from 'app/models/user';
import {UserService} from 'app/services/user.service';
//TODO: use a real swagger generated class for this.
class Notebook {
  constructor(public name: string, public description: string, public url: string) {}
}
/*
* Search filters used by the cohort and notebook tables to
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

  repositories: Repository[] = [];
  user: User;  // to detect if logged in
  cohortList: Cohort[] = [];
  //TODO: Replace with real data/notebooks
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
      private cohortsService: CohortsService
  ) {}
  ngOnInit(): void {
    this.userService.getLoggedInUser().then(user => this.user = user);
    this.cohortsService
        .getCohortsInWorkspace(
            this.route.snapshot.url[1].path, this.route.snapshot.url[2].path)
        .retry(2)
        .subscribe(
            cohortsReceived => {
              for (const coho of cohortsReceived.items) {
                this.cohortList.push(coho);
              }
            });

  }

  addCohort(): void {
    this.router.navigate(['cohorts/build'], {relativeTo : this.route});
  }

  goToCohortEdit(id: string): void {
    this.router.navigate(['cohorts/' + id + '/build'], {relativeTo : this.route});
  }
}
