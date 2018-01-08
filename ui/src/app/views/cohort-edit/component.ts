import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Subscription} from 'rxjs/Subscription';

import {ErrorHandlingService} from '../../services/error-handling.service';

import {Cohort, CohortsService} from 'generated';

@Component({
  styleUrls: ['./component.css'],
  templateUrl: './component.html',
})
export class CohortEditComponent implements OnInit, OnDestroy {
  cohort: Cohort;
  workspaceNamespace: string;
  workspaceId: string;
  private sub: Subscription;
  private loading = false;

  constructor(
      private router: Router,
      private route: ActivatedRoute,
      private cohortsService: CohortsService,
      private errorHandlingService: ErrorHandlingService,
  ) {}

  ngOnInit(): void {
    this.sub = this.route.params
      .do(({ns, wsid}) => {
        this.workspaceNamespace = ns;
        this.workspaceId = wsid;
      })
      .switchMap(({ns, wsid, cid}) => this.cohortsService.getCohort(ns, wsid, +cid))
      .subscribe(cohort => this.cohort = cohort);
  }

  ngOnDestroy() {
    this.sub.unsubscribe();
  }

  save(): void {
    this.loading = true;

    const call = this.cohortsService.updateCohort(
      this.workspaceNamespace,
      this.workspaceId,
      this.cohort.id,
      this.cohort
    );

    const success = ({cohort}) => {
      this.cohort = cohort;
      this.loading = false;
      this.router.navigate(['../../..'], {relativeTo: this.route});
    };

    this.errorHandlingService.retryApi(call).subscribe(success);
  }

  cancel(): void {
    this.router.navigate(['../../..'], {relativeTo : this.route});
  }
}
