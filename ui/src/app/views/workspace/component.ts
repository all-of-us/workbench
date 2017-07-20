import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';

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
  repositories: Repository[] = [];
  user: User;
  currentUrl: string;
  // TODO: Pull cohortList from external source
  cohortList = [];
  constructor(
      private router: Router,
      private userService: UserService,
      private repositoryService: RepositoryService,
      private cohortsService: CohortsService
  ) {}

  ngOnInit(): void {
    this.userService.getLoggedInUser()
        .then(user => this.user = user);
    this.cohortsService.getCohortsInWorkspace('123', '123').subscribe(
          cohortsReceived => {
            this.cohortList = cohortsReceived;
          });
    this.currentUrl = this.router.url;
  }

  goToCohortEdit(): void {
    this.router.navigate([this.currentUrl + '/cohort/cohort-id']);
  }
}
