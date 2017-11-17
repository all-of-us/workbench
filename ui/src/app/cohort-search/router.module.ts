import {NgModule} from '@angular/core';
import {RouterModule} from '@angular/router';

import {CohortSearchComponent} from './cohort-search/cohort-search.component';

@NgModule({
  imports: [RouterModule.forChild([
    /* Define routes here */
    {
      path: 'cohort/:id',
      component: CohortSearchComponent,
      data: {title: 'Build Cohort'}
    }, {
      path: 'workspace/:ns/:wsid/cohorts/build',
      component: CohortSearchComponent,
      data: {title: 'Build Cohort Criteria'}
    }
  ])],
  exports: [RouterModule]
})
export class CohortSearchRouter {}
