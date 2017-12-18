import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';

import {HomeComponent} from './data-browser/home/home.component';
import {SearchComponent} from './data-browser/search/search.component';

import {CohortResolver} from './guards/cohort-resolver.guard';

import {CohortEditComponent} from './views/cohort-edit/component';
import {HomePageComponent} from './views/home-page/component';
import {ReviewComponent} from './views/review/component';
import {WorkspaceEditComponent} from './views/workspace-edit/component';
import {WorkspaceShareComponent} from './views/workspace-share/component';
import {WorkspaceComponent} from './views/workspace/component';

/*
 * Both of these symbols (the resolver and the route list), which have no
 * relevance outside this module, MUST be exported. See
 * https://github.com/angular/angular-cli/issues/3707#issuecomment-332498738
 */
const routes: Routes = [
  {
    path: '',
    component: HomePageComponent,
    data: {title: 'View Workspaces'}
  }, {
    path: 'workspace/:ns/:wsid',
    component: WorkspaceComponent,
    data: {title: 'View Workspace Details'}
  }, {
    path: 'workspace/:ns/:wsid/cohorts/:cid/edit',
    component: CohortEditComponent,
    data: {title: 'Edit Cohort'},
    resolve: {
      cohort: CohortResolver
    }
  }, {
    path: 'workspace/build',
    component: WorkspaceEditComponent,
    data: {title: 'Create Workspace', adding: true}
  }, {
    path: 'workspace/:ns/:wsid/edit',
    component: WorkspaceEditComponent,
    data: {title: 'Edit Workspace', adding: false}
  }, {
    path: 'review',
    component: ReviewComponent,
    data: {title: 'Review Research Purposes'}
  }, {
    path: 'workspace/:ns/:wsid/share',
    component: WorkspaceShareComponent,
    data: {title: 'Share Workspace'}
  }, {
    path: 'data-browser/home',
    component: HomeComponent,
    data: {title: 'Data Browser'}
  }, {
    path: 'data-browser/browse',
    component: SearchComponent,
    data: {title: 'Browse'}
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
  providers: [CohortResolver],
})
export class AppRoutingModule {}
