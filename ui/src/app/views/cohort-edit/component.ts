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
  constructor(
      private router: Router,
      private route: ActivatedRoute,
      private cohortsService: CohortsService,
  ) {}

  ngOnInit(): void {
    this.cohortId = this.route.snapshot.url[4].path;
    if (this.route.snapshot.url[5] === undefined) {
      this.adding = true;
    } else {
      this.cohortsService
          .getCohort(
              WorkspaceComponent.DEFAULT_WORKSPACE_NS,
              WorkspaceComponent.DEFAULT_WORKSPACE_ID,
              this.cohortId)
          .retry(2)
          .subscribe(cohort => { this.cohort = cohort; });
    }
  }

  saveCohort(): void {
    this.cohortsService
        .updateCohort(
            WorkspaceComponent.DEFAULT_WORKSPACE_NS,
            WorkspaceComponent.DEFAULT_WORKSPACE_ID,
            this.cohort.id,
            this.cohort)
        .retry(2)
        .subscribe(cohorts => this.router.navigate(['../../..'], {relativeTo : this.route}));
  }

  addCohort(): void {
    this.cohortsService
        .createCohort(
            WorkspaceComponent.DEFAULT_WORKSPACE_NS,
            WorkspaceComponent.DEFAULT_WORKSPACE_ID,
            this.cohort)
        .retry(2)
        .subscribe(cohorts => this.router.navigate(['../..'], {relativeTo : this.route}));
  }

  cancelAdd(): void {
    this.router.navigate(['../..'], {relativeTo : this.route});
  }

  cancelSave(): void {
    this.router.navigate(['../../..'], {relativeTo : this.route});
  }


}
