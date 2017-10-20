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
  includeGroups,
  excludeGroups,
  wizardOpen,
  totalCount
} from '../redux';

@Component({
  selector: 'app-cohort-builder',
  templateUrl: './cohort-builder.component.html',
  styleUrls: ['./cohort-builder.component.css'],
})
export class CohortBuilderComponent implements OnInit, OnDestroy {

  @select(includeGroups) includeGroups$;
  @select(excludeGroups) excludeGroups$;
  @select(wizardOpen) open$;
  @select(totalCount) total$;

  private adding = false;
  private subscriptions: Subscription[];

  private includes;
  private excludes;

  constructor(private actions: CohortSearchActions,
              private router: Router,
              private route: ActivatedRoute) {}

  ngOnInit() {
    if (this.route.snapshot.url[5] === undefined) {
      this.adding = true;
    }
    this.subscriptions = [
      this.includeGroups$.subscribe(groups => this.includes = groups),
      this.excludeGroups$.subscribe(groups => this.excludes = groups)
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
