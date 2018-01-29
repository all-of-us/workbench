import {NgModule} from '@angular/core';
import {NavigationEnd, Router, RouterModule, Routes} from '@angular/router';

import {HomeComponent} from './data-browser/home/home.component';
import {SearchComponent} from './data-browser/search/search.component';

import {CohortEditComponent} from './views/cohort-edit/component';
import {HomePageComponent} from './views/home-page/component';
import {IdVerificationPageComponent} from './views/id-verification-page/component';
import {ProfileEditComponent} from './views/profile-edit/component';
import {ProfilePageComponent} from './views/profile-page/component';
import {ReviewComponent} from './views/review/component';
import {WorkspaceEditComponent, WorkspaceEditMode} from './views/workspace-edit/component';
import {WorkspaceShareComponent} from './views/workspace-share/component';
import {WorkspaceComponent} from './views/workspace/component';

import {CohortResolver} from './resolvers/cohort';
import {WorkspaceResolver} from './resolvers/workspace';

declare let gtag: Function;
declare let ga_tracking_id: string;

const cohortRoutes: Routes = [
];

const workspaceRoutes: Routes = [
  {
    path: '',
    component: WorkspaceComponent,
    data: {title: 'View Workspace Details'}
  }, {
    path: 'edit',
    component: WorkspaceEditComponent,
    data: {title: 'Edit Workspace', mode: WorkspaceEditMode.Edit}
  }, {
    path: 'clone',
    component: WorkspaceEditComponent,
    data: {title: 'Clone Workspace', mode: WorkspaceEditMode.Clone}
  }, {
    path: 'share',
    component: WorkspaceShareComponent,
    data: {title: 'Share Workspace'}
  }, {
    path: 'cohorts/:cid/edit',
    component: CohortEditComponent,
    data: {title: 'Edit Cohort'},
    resolve: {cohort: CohortResolver},
  }
];

const routes: Routes = [
  {
    path: '',
    component: HomePageComponent,
    data: {title: 'View Workspaces'}
  }, {
    path: 'workspace/:ns/:wsid',
    children: workspaceRoutes,
    resolve: { workspace: WorkspaceResolver },
  }, {
    path: 'data-browser/home',
    component: HomeComponent,
    data: {title: 'Data Browser'}
  }, {
    path: 'data-browser/browse',
    component: SearchComponent,
    data: {title: 'Browse'}
  }, {
    path: 'profile/id-verification',
    component: IdVerificationPageComponent,
    data: {title: 'ID Verification'}
  }, {
    path: 'profile',
    component: ProfilePageComponent,
    data: {title: 'Profile'}
  }, {
    path: 'profile/edit',
    component: ProfileEditComponent,
    data: {title: 'Profile'}
  }, {
    path: 'workspace/build',
    component: WorkspaceEditComponent,
    data: {title: 'Create Workspace', mode: WorkspaceEditMode.Create}
  }, {
    path: 'review',
    component: ReviewComponent,
    data: {title: 'Review Research Purposes'}
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
  providers: [
    CohortResolver,
    WorkspaceResolver,
  ]
})
export class AppRoutingModule {

 constructor(public router: Router) {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        gtag('config', ga_tracking_id, { 'page_path': event.urlAfterRedirects });
      }
    });
  }
}
