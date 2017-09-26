import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {CohortBuilderComponent} from './cohort-builder/cohort-builder.component';

@NgModule({
  imports: [RouterModule.forChild([
    /* Define routes here */
    {
      path: 'cohort/:id',
      component: CohortBuilderComponent,
      data: {title: 'Build Cohort'}
    }, {
      path: 'workspace/:ns/:wsid/cohorts/build',
      component: CohortBuilderComponent,
      data: {title: 'Build Cohort Criteria'}
    }
  ])],
  exports: [RouterModule]
})
export class CohortSearchRouter {}
