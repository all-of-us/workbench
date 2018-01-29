import {NgModule} from '@angular/core';
import {RouterModule} from '@angular/router';

import {CohortSearchComponent} from './cohort-search/cohort-search.component';

import {WorkspacesService} from 'generated';

export const workspaceProvider = (api) => (route) => api
  .getWorkspace(route.params.ns, route.params.wsid)
  .map(({workspace, accessLevel}) => ({...workspace, accessLevel}));

@NgModule({
  imports: [RouterModule.forChild([{
    path: 'workspace/:ns/:wsid/cohorts/build',
    component: CohortSearchComponent,
    data: {title: 'Build Cohort Criteria'},
    resolve: {workspace: 'workspace'},
  }])],
  providers: [{
    provide: 'workspace',
    deps: [WorkspacesService],
    useFactory: workspaceProvider,
  }],
  exports: [RouterModule],
})
export class CohortSearchRouter {}
