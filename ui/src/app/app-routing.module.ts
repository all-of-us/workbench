import {NgModule} from '@angular/core';
import {NavigationEnd, Router, RouterModule, Routes} from '@angular/router';

import {RegistrationGuard} from './guards/registration-guard.service';
import {SignInGuard} from './guards/sign-in-guard.service';

import {AdminReviewIdVerificationComponent} from './views/admin-review-id-verification/component';
import {AdminReviewWorkspaceComponent} from './views/admin-review-workspace/component';
import {CohortEditComponent} from './views/cohort-edit/component';
import {CohortListComponent} from './views/cohort-list/component';
import {HomepageComponent} from './views/homepage/component';
import {LoginComponent} from './views/login/component';
import {NotebookListComponent} from './views/notebook-list/component';
import {NotebookRedirectComponent} from './views/notebook-redirect/component';
import {ProfilePageComponent} from './views/profile-page/component';
import {SettingsComponent} from './views/settings/component';
import {SignedInComponent} from './views/signed-in/component';
import {StigmatizationPageComponent} from './views/stigmatization-page/component';
import {UnregisteredComponent} from './views/unregistered/component';
import {WorkspaceEditComponent, WorkspaceEditMode} from './views/workspace-edit/component';
import {WorkspaceListComponent} from './views/workspace-list/component';
import {WorkspaceNavBarComponent} from './views/workspace-nav-bar/component';
import {WorkspaceComponent} from './views/workspace/component';

import {CohortResolver} from './resolvers/cohort';
import {WorkspaceResolver} from './resolvers/workspace';

declare let gtag: Function;
declare let ga_tracking_id: string;

const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent,
    data: {title: 'Sign In'}
  }, {
    path: '',
    component: SignedInComponent,
    canActivate: [SignInGuard],
    canActivateChild: [SignInGuard, RegistrationGuard],
    runGuardsAndResolvers: 'always',
    children: [
      {
        path: '',
        component: HomepageComponent,
        data: {title: 'Homepage'},
      }, {
        path: 'unregistered',
        component: UnregisteredComponent,
        data: {
          title: 'Awaiting ID Verification'
        }
      }, {
        path: 'definitions/stigmatization',
        component: StigmatizationPageComponent,
        data: {
          title: 'Stigmatization Definition'
        }
      }, {
      path: 'workspaces',
      data: {breadcrumb: 'Workspaces'},
      children: [
        {
          path: '',
          component: WorkspaceListComponent,
          data: {title: 'View Workspaces'}
        },
        {
          /* TODO The children under ./views need refactoring to use the data
           * provided by the route rather than double-requesting it.
           */
          path: ':ns/:wsid',
          component: WorkspaceNavBarComponent,
          data: {
            title: 'View Workspace Details',
            breadcrumb: 'Param: Workspace Name'
          },
          runGuardsAndResolvers: 'always',
          resolve: {
            workspace: WorkspaceResolver,
          },
          children: [
            {
              path: '',
              component: WorkspaceComponent,
              data: {
                title: 'View Workspace Details',
                breadcrumb: 'View Workspace Details'
              }
            }, {
              path: 'edit',
              component: WorkspaceEditComponent,
              data: {
                title: 'Edit Workspace',
                mode: WorkspaceEditMode.Edit,
                breadcrumb: 'Edit Workspace'
              }
            }, {
              path: 'clone',
              component: WorkspaceEditComponent,
              data: {
                title: 'Clone Workspace',
                mode: WorkspaceEditMode.Clone,
                breadcrumb: 'Clone Workspace'
              }
            },
            {
              path: 'notebooks',
              component: NotebookListComponent,
              data: {
                title: 'View Notebooks',
                breadcrumb: 'Notebooks'
              }
            },
            {
              path: 'cohorts',
              data: { breadcrumb: 'Cohorts' },
              children: [
                {
                  path: '',
                  component: CohortListComponent,
                  data: {
                    title: 'View Cohorts',
                  },
                },
                {
                  path: 'build',
                  loadChildren: './cohort-search/cohort-search.module#CohortSearchModule',
                  data: {
                    breadcrumb: 'Add a Cohort'
                  }
                },
                {
                  path: ':cid/review',
                  loadChildren: './cohort-review/cohort-review.module#CohortReviewModule',
                  data: {
                    title: 'Cohort',
                    breadcrumb: 'Param: Cohort Name'
                  },
                  resolve: {
                    cohort: CohortResolver,
                  }
                },
                {
                  path: ':cid/edit',
                  component: CohortEditComponent,
                  data: {
                    title: 'Edit Cohort',
                    breadcrumb: 'Param: Cohort Name'
                  },
                  resolve: {
                    cohort: CohortResolver,
                  }
                }
              ]
            }]
          }
        ]
      },
      {
        path: 'admin/review-workspace',
        component: AdminReviewWorkspaceComponent,
        data: {title: 'Review Workspaces'}
      }, {
        path: 'admin/review-id-verification',
        component: AdminReviewIdVerificationComponent,
        data: {title: 'Review ID Verifications'}
      }, {
        path: 'profile',
        component: ProfilePageComponent,
        data: {title: 'Profile'}
      }, {
        path: 'settings',
        component: SettingsComponent,
        data: {title: 'Settings'}
      }, {
        path: 'workspaces/build',
        component: WorkspaceEditComponent,
        data: {title: 'Create Workspace', mode: WorkspaceEditMode.Create}
      }, {
        // The notebook redirect pages are interstitial pages, so we want to
        // give them special chrome treatment - we therefore put them outside
        // the normal /workspaces hierarchy.
        path: 'workspaces/:ns/:wsid/notebooks/create',
        component: NotebookRedirectComponent,
        data: {
          title: 'Creating a new Notebook'
        }
      }, {
        path: 'workspaces/:ns/:wsid/notebooks/:nbName',
        component: NotebookRedirectComponent,
        data: {
          title: 'Opening a Notebook'
        }
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes,
    {onSameUrlNavigation: 'reload', paramsInheritanceStrategy: 'always'})],
  exports: [RouterModule],
  providers: [
    RegistrationGuard,
    SignInGuard,
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
