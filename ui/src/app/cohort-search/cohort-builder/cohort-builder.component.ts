import {Component, OnInit, Inject} from '@angular/core';
import {DOCUMENT} from '@angular/platform-browser';
import {NgRedux, select} from '@angular-redux/store';
import {Router, ActivatedRoute} from '@angular/router';
import {Observable} from 'rxjs/Observable';

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
  styleUrls: ['cohort-builder.component.css']
})
export class CohortBuilderComponent implements OnInit {

  @select(inclusionGroups) includeGroups$;
  @select(exclusionGroups) excludeGroups$;
  @select(subjects) subjects$;
  @select(wizardOpen) readonly open$: Observable<boolean>;

  private adding = false;

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions,
              private router: Router,
              private route: ActivatedRoute,
              @Inject(DOCUMENT) private document: any) {}

  ngOnInit() {
    if (this.route.snapshot.url[5] === undefined) {
      this.adding = true;
    }
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
    const scrollableDiv = window.document.getElementById('scrollable-groups');
    scrollableDiv.scrollTop = scrollableDiv.scrollHeight;
  }
}
