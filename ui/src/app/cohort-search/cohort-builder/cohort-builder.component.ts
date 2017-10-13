import {
  Component,
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Inject,
  OnInit,
  OnDestroy,
} from '@angular/core';
import {DOCUMENT} from '@angular/platform-browser';
import {NgRedux, select} from '@angular-redux/store';
import {Router, ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';
import {Subscription} from 'rxjs/Subscription';

import {
  CohortSearchActions,
  CohortSearchState,
} from '../redux';

@Component({
  selector: 'app-cohort-builder',
  templateUrl: './cohort-builder.component.html',
  styleUrls: ['./cohort-builder.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CohortBuilderComponent implements OnInit, OnDestroy {

  @select(s => s) state$;
  private includeGroups;
  private excludeGroups;
  private open;
  private total;

  private adding = false;
  private subscription: Subscription;

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private cd: ChangeDetectorRef,
              private actions: CohortSearchActions,
              private router: Router,
              private route: ActivatedRoute,
              @Inject(DOCUMENT) private document: any) {}

  ngOnInit() {
    if (this.route.snapshot.url[5] === undefined) {
      this.adding = true;
    }

    this.subscription = this.state$.subscribe(state => {
      this.includeGroups = state.getIn(['search', 'includes'], []);
      this.excludeGroups = state.getIn(['search', 'excludes'], []);
      this.open = state.getIn(['context', 'wizardOpen'], false);
      this.total = state.getIn(['counts', 'total'], 0);
      this.cd.markForCheck();
    });
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
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

    // FIXME: this doesn't appear to actually do anything
    const scrollableDiv = document.getElementById('scrollable-groups');
    scrollableDiv.scrollTop = scrollableDiv.scrollHeight;
  }
}
