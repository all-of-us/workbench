import {NgModule} from '@angular/core';
import {NavigationEnd, Router, RouterModule, Routes} from '@angular/router';

import {HomeComponent} from './data-browser/home/home.component';
import {SearchComponent} from './data-browser/search/search.component';

import {AdminReviewIdVerificationComponent} from './views/admin-review-id-verification/component';
import {AdminReviewWorkspaceComponent} from './views/admin-review-workspace/component';
import {CohortEditComponent} from './views/cohort-edit/component';
import {HomePageComponent} from './views/home-page/component';
import {IdVerificationPageComponent} from './views/id-verification-page/component';
import {ProfileEditComponent} from './views/profile-edit/component';
import {ProfilePageComponent} from './views/profile-page/component';
import {WorkspaceEditComponent, WorkspaceEditMode} from './views/workspace-edit/component';
import {WorkspaceShareComponent} from './views/workspace-share/component';
import {WorkspaceComponent} from './views/workspace/component';

import {AnnotationDefinitionsResolver} from './resolvers/annotation-definitions';
import {CohortResolver} from './resolvers/cohort';
import {ReviewResolver} from './resolvers/review';
import {WorkspaceResolver} from './resolvers/workspace';

declare let gtag: Function;
declare let ga_tracking_id: string;

const routes: Routes = [
  {
    path: '',
    component: HomePageComponent,
    data: {title: 'View Workspaces'}
  }, {
    /* TODO The children under ./views need refactoring to use the data
     * provided by the route rather than double-requesting it.
     */
    path: 'workspace/:ns/:wsid',
    resolve: {
      workspace: WorkspaceResolver,
    },
    children: [{
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
        path: 'cohorts/build',
        loadChildren: './cohort-search/cohort-search.module#CohortSearchModule',
      }, {
        path: 'cohorts/:cid/review',
        loadChildren: './cohort-review/cohort-review.module#CohortReviewModule',
        resolve: {
          annotationDefinitions: AnnotationDefinitionsResolver,
          cohort: CohortResolver,
          review: ReviewResolver,
        },
      }, {
        path: 'cohorts/:cid/edit',
        component: CohortEditComponent,
        data: {title: 'Edit Cohort'},
        resolve: {
          cohort: CohortResolver,
        },
    }],
  }, {
    path: 'admin/review-workspace',
    component: AdminReviewWorkspaceComponent,
    data: {title: 'Review Workspaces'}
  }, {
    path: 'admin/review-id-verification',
    component: AdminReviewIdVerificationComponent,
    data: {title: 'Review ID Verifications'}
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
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
  providers: [
    AnnotationDefinitionsResolver,
    CohortResolver,
    ReviewResolver,
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
