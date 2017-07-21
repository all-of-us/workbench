// Based on the URL mapping in "routes" below, the RouterModule attaches
// UI Components to the <router-outlet> element in the main AppComponent.

import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {CohortBuilderComponent} from 'app/views/cohort-builder/component';
import {LoginComponent} from 'app/views/login/component';
import {WorkspaceComponent} from 'app/views/workspace/component';
import {SelectRepositoryComponent} from 'app/views/select-repository/component';

const routes: Routes = [
  {path: '', redirectTo: '/login', pathMatch: 'full', data: {title: 'Redirecting...'}},
  {path: 'login', component: LoginComponent, data: {title: 'Log In'}},
  {path: 'repository', component: SelectRepositoryComponent, data: {title: 'Select Repository'}},
  {path: 'workspace/:ns/:wsid', component: WorkspaceComponent, data: {title: 'View Workspace'}},
  {path: 'cohort/:id', component: CohortBuilderComponent, data: {title: 'Build Cohort'}}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}
