// Based on the URL mapping in "routes" below, the RouterModule attaches
// UI Components to the <router-outlet> element in the main AppComponent.

// TODO: Remove the lint-disable comment once we can selectively ignore import lines.
// https://github.com/palantir/tslint/pull/3099
// tslint:disable:max-line-length

import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {CohortEditComponent} from 'app/views/cohort-edit/component';
import {HomePageComponent} from 'app/views/home-page/component';
import {ReviewComponent} from 'app/views/review/component';
import {WorkspaceComponent} from 'app/views/workspace/component';
import {WorkspaceEditComponent} from 'app/views/workspace-edit/component';

// tslint:enable:max-line-length


const routes: Routes = [
  {path: '', component: HomePageComponent, data: {title: 'View Workspaces'}},
  {path: 'workspace/:ns/:wsid',
          component: WorkspaceComponent,
          data: {title: 'View Workspace Details'}},
  {path: 'workspace/:ns/:wsid/cohorts/:cid/edit',
          component: CohortEditComponent,
          data: {title: 'Edit Cohort', adding: false}},
  {path: 'workspace/:ns/:wsid/cohorts/create',
          component: CohortEditComponent,
          data: {title: 'Create Cohort', adding: true}},
  {path: 'workspace/build',
          component: WorkspaceEditComponent,
          data: {title: 'Create Workspace', adding: true}},
  {path: 'workspace/:ns/:wsid/edit',
          component: WorkspaceEditComponent,
          data: {title: 'Edit Workspace', adding: false}},
  {path: 'review',
          component: ReviewComponent,
          data: {title: 'Review Research Purposes'}}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
