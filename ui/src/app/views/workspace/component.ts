import {Component, OnInit} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';

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
  cohortList: Cohort[] = [];
  constructor(
      private router: Router,
      private route: ActivatedRoute,
      private userService: UserService,
      private repositoryService: RepositoryService,
      private cohortsService: CohortsService
  ) {}
  // TODO: Replace arguments to getCohortsInWorkspace to read from URL.
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
