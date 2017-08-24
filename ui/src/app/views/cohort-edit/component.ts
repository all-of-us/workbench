import {Component, OnInit} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';

import {Cohort} from 'generated';
import {CohortsService} from 'generated';
import {WorkspaceComponent} from 'app/views/workspace/component';

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
  ) {}

  ngOnInit(): void {
    this.cohortId = this.route.snapshot.url[4].path;
    this.workspaceId = this.route.snapshot.url[2].path;
    this.workspaceNamespace = this.route.snapshot.url[1].path;
    if (this.route.snapshot.url[5] === undefined) {
      this.adding = true;
    } else {
      this.cohortsService
          .getCohort(
              this.workspaceNamespace,
              this.workspaceId,
              this.cohortId)
          .retry(2)
          .subscribe(this.handleCohortFetched);
    }
  }

  /**
   * Copy in only the client-used fields of the Cohort.
   * TODO: Remove this and copy the Cohort wholesale once deserialization of all fields
   * works server-side. (Currently creationTime deserialization causes errors.)
   */
  handleCohortFetched = (cohort: Cohort) => {
    console.log(cohort);
    console.log(this.cohort);
    this.cohort.id = cohort.id;
    this.cohort.name = cohort.name;
    this.cohort.description = cohort.description;
    this.cohort.criteria = cohort.criteria;
    this.cohort.type = cohort.type;
  }

  saveCohort(): void {
    if (!this.buttonClicked) {
      this.buttonClicked = true;
      this.cohortsService
          .updateCohort(
              this.workspaceNamespace,
              this.workspaceId,
              this.cohort.id,
              this.cohort)
          .retry(2)
          .subscribe(cohorts => this.router.navigate(['../../..'], {relativeTo : this.route}));
    }
  }

  addCohort(): void {
    if (!this.buttonClicked) {
      this.buttonClicked = true;
      this.cohortsService
          .createCohort(
              this.workspaceNamespace,
              this.workspaceId,
              this.cohort)
          .retry(2)
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
