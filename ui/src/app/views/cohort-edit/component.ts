import {Component, OnInit} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';

import {ErrorHandlingService} from 'app/services/error-handling.service';
import {WorkspaceComponent} from 'app/views/workspace/component';

import {Cohort} from 'generated';
import {CohortsService} from 'generated';

@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class CohortEditComponent implements OnInit {
  cohort: Cohort = {id: '', name: '', description: '', criteria: '', type: ''};
  cohortId: string;
  adding = false;
  workspaceNamespace: string;
  workspaceId: string;
  buttonClicked = false;
  constructor(
      private router: Router,
      private route: ActivatedRoute,
      private cohortsService: CohortsService,
      private errorHandlingService: ErrorHandlingService,
  ) {}

  ngOnInit(): void {
    this.cohortId = this.route.snapshot.params['cid'];
    this.workspaceId = this.route.snapshot.params['wsid'];
    this.workspaceNamespace = this.route.snapshot.params['ns'];
    if (this.route.routeConfig.data.adding) {
      this.adding = true;
    } else {
      this.errorHandlingService.retryApi(this.cohortsService
          .getCohort(
              this.workspaceNamespace,
              this.workspaceId,
              this.cohortId))
          .subscribe(cohort => this.cohort = cohort);
    }
  }

  saveCohort(): void {
    if (!this.buttonClicked) {
      this.buttonClicked = true;
      this.errorHandlingService.retryApi(this.cohortsService
          .updateCohort(
              this.workspaceNamespace,
              this.workspaceId,
              this.cohort.id,
              this.cohort))
          .subscribe(cohorts => this.router.navigate(['../../..'], {relativeTo : this.route}));
    }
  }

  addCohort(): void {
    if (!this.buttonClicked) {
      this.buttonClicked = true;
      this.errorHandlingService.retryApi(this.cohortsService
          .createCohort(
              this.workspaceNamespace,
              this.workspaceId,
              this.cohort))
          .subscribe(cohorts => this.router.navigate(['../..'], {relativeTo : this.route}));
    }
  }

  cancelAdd(): void {
    this.router.navigate(['../..'], {relativeTo : this.route});
  }

  cancelSave(): void {
    this.router.navigate(['../../..'], {relativeTo : this.route});
  }
}
