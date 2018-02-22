import {select} from '@angular-redux/store';
import {Component, Input} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import {ActivatedRoute, Router} from '@angular/router';
import {List} from 'immutable';
import {Observable} from 'rxjs/Observable';

import {CohortSearchActions} from '../redux';

import {Cohort, CohortsService, Workspace} from 'generated';

const COHORT_TYPE = 'AoU_Discover';

@Component({
  selector: 'app-overview',
  templateUrl: './overview.component.html',
  styleUrls: ['./overview.component.css']
})
export class OverviewComponent {
  @Input() chartData$: Observable<List<any>>;
  @Input() total$: Observable<number>;
  @Input() isRequesting$: Observable<boolean>;
  @select(s => s.get('initShowChart')) initShowChart$: Observable<boolean>;

  private cohortForm = new FormGroup({
    name: new FormControl('', [Validators.required]),
    description: new FormControl()
  });

  constructor(
    private actions: CohortSearchActions,
    private cohortApi: CohortsService,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  get name() {
    return this.cohortForm.get('name');
  }

  submit() {
    const ns: Workspace['namespace'] = this.route.snapshot.params.ns;
    const wsid: Workspace['id'] = this.route.snapshot.params.wsid;

    const name = this.cohortForm.get('name').value;
    const description = this.cohortForm.get('description').value;
    const criteria = JSON.stringify(this.actions.mapAll());
    const cohort = <Cohort>{name, description, criteria, type: COHORT_TYPE};
    const goBack = (_) => this.router.navigate(['workspace', ns, wsid]);
    this.cohortApi.createCohort(ns, wsid, cohort).subscribe(goBack);
  }
}
