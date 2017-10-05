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

import {CohortSearchActions} from '../actions';
import {
  CohortSearchState,
  inclusionGroups,
  exclusionGroups,
  wizardOpen,
  subjects,
} from '../store';

@Component({
  selector: 'app-search',
  templateUrl: 'cohort-builder.component.html',
  styleUrls: ['cohort-builder.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CohortBuilderComponent implements OnInit {

  @select(s => s) state$;

  private includeGroups;
  private excludeGroups;
  private subjects;
  private open;

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
      this.includeGroups = state.getIn(inclusionGroups, []);
      this.excludeGroups = state.getIn(exclusionGroups, []);
      this.subjects = state.getIn(subjects, []);
      this.open = state.getIn(wizardOpen, false);
      this.cd.markForCheck();
    });
  }

  save(): void {
    if (this.adding) {
      this.router.navigate(['../create'], {relativeTo : this.route});
    } else {
      this.router.navigate(['../edit'], {relativeTo : this.route});
    }
  }

  initGroup(kind) {
    this.actions.initGroup(kind);
    const scrollableDiv = document.getElementById('scrollable-groups');
    scrollableDiv.scrollTop = scrollableDiv.scrollHeight;
  }

  ngOnDestroy() {
    this.subscription.unsubscribe();
  }
}
