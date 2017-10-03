import {Component} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';
import {DOCUMENT} from '@angular/platform-browser';
import {NgRedux, select, dispatch} from '@angular-redux/store';
import {intersection, complement, union} from 'set-manipulator';
import {Observable} from 'rxjs/Observable';
import 'rxjs/add/operator/takeWhile';

import {CohortSearchActions} from '../actions';
import {inclusionGroups, exclusionGroups, wizardOpen} from '../store';
import {CohortSearchState} from '../store.interfaces';

import {SearchGroup, SubjectListResponse as SubjectList} from 'generated';


@Component({
  selector: 'app-search',
  templateUrl: 'cohort-builder.component.html',
  styleUrls: ['cohort-builder.component.css']
})
export class CohortBuilderComponent {

  @select(inclusionGroups) includeGroups$: Observable<SearchGroup[]>;
  @select(exclusionGroups) excludeGroups$: Observable<SearchGroup[]>;
  @select('subjects') subjects$: Observable<SubjectList>;
  @select(wizardOpen) readonly open$: Observable<boolean>;

  constructor(private router: Router,
              private route: ActivatedRoute,
              private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}
}
