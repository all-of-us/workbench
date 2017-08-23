// Based on the URL mapping in "routes" below, the RouterModule attaches
// UI Components to the <router-outlet> element in the main AppComponent.

import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {CohortBuilderComponent} from 'app/views/cohort-builder/component';
import {CohortBuilderPlaceholderComponent} from 'app/views/cohort-builder-placeholder/component';
import {CohortEditComponent} from 'app/views/cohort-edit/component';
import {HomePageComponent} from 'app/views/home-page/component';
import {WorkspaceComponent} from 'app/views/workspace/component';
import {WorkspaceEditComponent} from 'app/views/workspace-edit/component';


const routes: Routes = [
  {path: '', component: HomePageComponent, data: {title: 'View Workspaces'}},
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
          component: CohortBuilderPlaceholderComponent,
          data: {title: 'Build Cohort Criteria'}},
  {path: 'workspace/:ns/:wsid/cohorts/:cid/build',
          component: CohortBuilderPlaceholderComponent,
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
