import {Component, OnInit} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';

import {Cohort} from 'generated';
import {CohortsService} from 'generated';
import {User} from 'app/models/user';
import {UserService} from 'app/services/user.service';
import {CohortEditService} from 'app/services/cohort-edit.service';

@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class CohortEditComponent implements OnInit {
  user: User;
  cohort: Cohort = {id: '', name: '', description: '', criteria: '', type: ''};
  cohortId: string;
  adding = false;
  constructor(
      private router: Router,
      private route: ActivatedRoute,
      private userService: UserService,
      private cohortsService: CohortsService,
      private CohortEditService: CohortEditService
  ) {}

  ngOnInit(): void {
    this.userService.getLoggedInUser()
        .then(user => this.user = user);
    this.cohortId = this.route.snapshot.url[4].path;
    if (this.route.snapshot.url[5] === undefined) {
      this.adding = true;
    } else {
      this.CohortEditService.get(this.cohortId).then(cohort => {
        this.cohort = cohort;
      }).catch(error => {
        alert('All of Us Researcher Workbench does not currently \
support editing cohorts from the API.');
        this.router.navigate(['../../..'], {relativeTo : this.route});
      });
    }
  }

  saveCohort(): void {
    this.CohortEditService.edit(this.cohort.id, this.cohort).then(
      cohorts => this.router.navigate(['../../..'], {relativeTo : this.route}));
  }

  addCohort(): void {
    this.CohortEditService.add(this.cohort).then(
      cohorts => this.router.navigate(['../..'], {relativeTo : this.route}));
  }

  cancelAdd(): void {
    this.router.navigate(['../..'], {relativeTo : this.route});
  }

  cancelSave(): void {
    this.router.navigate(['../../..'], {relativeTo : this.route});
  }


}
