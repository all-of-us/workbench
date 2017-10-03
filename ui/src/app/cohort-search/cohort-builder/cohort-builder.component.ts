import {Component} from '@angular/core';
import {NgRedux, select} from '@angular-redux/store';
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
export class CohortBuilderComponent {

  @select(inclusionGroups) includeGroups$;
  @select(exclusionGroups) excludeGroups$;
  @select(subjects) subjects$;
  @select(wizardOpen) readonly open$: Observable<boolean>;

  constructor(private ngRedux: NgRedux<CohortSearchState>,
              private actions: CohortSearchActions) {}
}
