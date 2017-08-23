// Based on the URL mapping in "routes" below, the RouterModule attaches
// UI Components to the <router-outlet> element in the main AppComponent.

import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {CohortBuilderComponent} from 'app/views/cohort-builder/search/cohort-builder/cohort-builder.component';
import {CohortEditComponent} from 'app/views/cohort-edit/component';
import {HomePageComponent} from 'app/views/home-page/component';
import {LoginComponent} from 'app/views/login/component';
import {WorkspaceComponent} from 'app/views/workspace/component';
import {WorkspaceEditComponent} from 'app/views/workspace-edit/component';
import {SelectRepositoryComponent} from 'app/views/select-repository/component';
import { CohortReviewComponent } from 'app/views/cohort-builder/review/cohort-review/cohort-review.component';


const routes: Routes = [
  {path: '', component: HomePageComponent, data: {title: 'View Workspaces'}},
  {path: 'login', component: LoginComponent, data: {title: 'Log In'}},
  {path: 'repository', component: SelectRepositoryComponent, data: {title: 'Select Repository'}},
  {path: 'workspace/:ns/:wsid',
          component: WorkspaceComponent,
          data: {title: 'View Workspace Details'}},
  {path: 'cohort/:id', component: CohortBuilderComponent, data: {title: 'Build Cohort'}},
  {path: 'workspace/:ns/:wsid/cohorts/:cid/edit',
          component: CohortEditComponent,
          data: {title: 'Edit Cohort'}},
  {path: 'workspace/:ns/:wsid/cohorts/create',
          component: CohortEditComponent,
          data: {title: 'Create Cohort'}},
  {path: 'workspace/:ns/:wsid/cohorts/build',
          component: CohortBuilderComponent,
          data: {title: 'Build Cohort Criteria'}},
  {path: 'workspace/:ns/:wsid/cohorts/:cid/build',
          component: CohortReviewComponent,
          data: {title: 'Edit Cohort Criteria'}},
  {path: 'workspace/build',
          component: WorkspaceEditComponent,
          data: {title: 'Create Workspace'}}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
