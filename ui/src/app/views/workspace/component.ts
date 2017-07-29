import {Component, OnInit} from '@angular/core';
import {Router} from '@angular/router';

import {Cohort} from 'generated';
import {CohortsService} from 'generated';
import {CohortEditService} from 'app/services/cohort-edit.service';
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
  cohortList: Cohort[] = [];
  constructor(
      private router: Router,
      private userService: UserService,
      private repositoryService: RepositoryService,
      private cohortsService: CohortsService,
      private CohortEditService: CohortEditService
  ) {}

  ngOnInit(): void {
    this.userService.getLoggedInUser()
        .then(user => this.user = user);
    this.cohortList = [];
    document.body.style.cursor = 'wait';
    this.CohortEditService.list().then(
      cohorts => {
        this.cohortList = cohorts.slice();
        this.cohortsService.getCohortsInWorkspace('123', '123').retry(2).subscribe(
          cohortsReceived => {
            document.body.style.cursor = 'auto';
            for (const coho of cohortsReceived.items) {
              this.cohortList.push(coho);
            }
          },
          error => {
            document.body.style.cursor = 'auto';
            console.log('Failed to fetch cohorts data.');
          }
        );
      }
    );
    this.currentUrl = this.router.url;
  }

  addCohort(): void {
    this.router.navigate([this.currentUrl + '/cohorts/build']);
  }

  goToCohortEdit(id: string): void {
    this.router.navigate([this.currentUrl + '/cohorts/' + id + '/build']);
  }
}
