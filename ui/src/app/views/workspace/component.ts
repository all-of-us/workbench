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
  }
  // TODO: edit/add cohort link - blrubenstein
  goToCohort(id: number): void {
    this.router.navigate(['/cohort', id]);
  }
  // TODO: open notebook link - blrubenstein
  goToNotebook(id: number): void {

  }
}
