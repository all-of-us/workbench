import {
  Component,
  OnInit,
  OnDestroy,
} from '@angular/core';
import {NgRedux, select} from '@angular-redux/store';
import {Router, ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';
import {List} from 'immutable';

import {
  CohortSearchActions,
  CohortSearchState,
  activeCriteriaType,
  includeGroups,
  excludeGroups,
  wizardOpen,
  totalCount,
  isRequstingTotal
} from '../redux';

@Component({
  selector: 'app-cohort-builder',
  templateUrl: './cohort-builder.component.html',
  styleUrls: ['./cohort-builder.component.css'],
})
export class CohortBuilderComponent implements OnInit, OnDestroy {

  @select(includeGroups) includeGroups$: Observable<List<any>>;
  @select(excludeGroups) excludeGroups$: Observable<List<any>>;
  @select(wizardOpen) open$: Observable<boolean>;
  @select(totalCount) total$: Observable<number>;
  @select(isRequstingTotal) isRequesting$: Observable<boolean>;
  @select(activeCriteriaType) criteriaType$: Observable<string>;

  private adding = false;
  private subscriptions: Subscription[];

  private includes: List<any>;
  private excludes: List<any>;
  private isRequesting = false;

  constructor(private actions: CohortSearchActions,
              private router: Router,
              private route: ActivatedRoute) {}

  ngOnInit() {
    if (this.route.snapshot.url[5] === undefined) {
      this.adding = true;
    }
    this.subscriptions = [
      this.includeGroups$.subscribe(groups => this.includes = groups),
      this.excludeGroups$.subscribe(groups => this.excludes = groups),
      this.isRequesting$.subscribe(val => this.isRequesting = val),
    ];
  }

  ngOnDestroy() {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  save(): void {
    if (this.adding) {
      this.router.navigate(['../create'], {relativeTo : this.route});
    } else {
      this.router.navigate(['../edit'], {relativeTo : this.route});
    }
  }

  initGroup(role) {
    this.actions.initGroup(role);
  }
}
