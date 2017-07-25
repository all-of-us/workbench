import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';

import {Cohort} from 'generated';
import {CohortsService} from 'generated';
import {Repository} from 'app/models/repository';
import {RepositoryService} from 'app/services/repository.service';
import {User} from 'app/models/user';
import {UserService} from 'app/services/user.service';


@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class WorkspaceComponent implements OnInit {
  public static DEFAULT_WORKSPACE_NS = 'defaultNamespace';
  public static DEFAULT_WORKSPACE_NAME = 'defaultWorkspace';
  public static DEFAULT_WORKSPACE_ID = '1';

  repositories: Repository[] = [];
  user: User;  // to detect if logged in
  currentUrl: string;
  cohortList = [];
  constructor(
      private router: Router,
      private userService: UserService,
      private repositoryService: RepositoryService,
      private cohortsService: CohortsService
  ) {}

  ngOnInit(): void {
    this.userService.getLoggedInUser().then(user => this.user = user);
    this.cohortsService
        .getCohortsInWorkspace(
            WorkspaceComponent.DEFAULT_WORKSPACE_NS, WorkspaceComponent.DEFAULT_WORKSPACE_ID)
        .retry(2)
        .subscribe(
            cohortsReceived => {
              for (const coho of cohortsReceived.items) {
                this.cohortList.push(coho);
              }
            });
    this.currentUrl = this.router.url;
  }

  addCohort(): void {
    this.router.navigate([this.currentUrl + '/cohorts/build']);
  }

  goToCohortEdit(id: string): void {
    this.router.navigate([this.currentUrl + '/cohorts/' + id + '/build']);
  }
}
