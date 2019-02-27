import {NgModule} from '@angular/core';
import {NavigationEnd, Router, RouterModule, Routes} from '@angular/router';

import {AccessTasksGuard} from './guards/access-tasks-guard.service';
import {RegistrationGuard} from './guards/registration-guard.service';
import {SignInGuard} from './guards/sign-in-guard.service';

import {AdminReviewWorkspaceComponent} from './views/admin-review-workspace/component';
import {AdminUserComponent} from './views/admin-user/component';
import {CohortListComponent} from './views/cohort-list/component';
import {ConceptHomepageComponent} from './views/concept-homepage/component';
import {ConceptSetDetailsComponent} from './views/concept-set-details/component';
import {ConceptSetListComponent} from './views/concept-set-list/component';
import {HomepageComponent} from './views/homepage/component';
import {NotebookListComponent} from './views/notebook-list/component';
import {NotebookRedirectComponent} from './views/notebook-redirect/component';
import {ProfilePageComponent} from './views/profile-page/component';
import {SettingsComponent} from './views/settings/component';
import {SignedInComponent} from './views/signed-in/component';
import {StigmatizationPageComponent} from './views/stigmatization-page/component';
import {UnregisteredComponent} from './views/unregistered/component';
import {WorkspaceEditComponent, WorkspaceEditMode} from './views/workspace-edit/component';
import {WorkspaceListComponent} from './views/workspace-list/component';
import {WorkspaceWrapperComponent} from './views/workspace-wrapper/component';
import {WorkspaceComponent} from './views/workspace/component';

import {CohortResolver} from './resolvers/cohort';
import {ConceptSetResolver} from './resolvers/concept-set';
import {WorkspaceResolver} from './resolvers/workspace';

import {DataPageComponent} from 'app/views/data-page/component';
import {DataSetComponent} from 'app/views/dataset/component';
import {environment} from 'environments/environment';
import {NavStore} from './utils/navigation';
import {SignInComponent} from './views/sign-in/component';

declare let gtag: Function;

const routes: Routes = [
  {
    path: 'login',
    component: SignInComponent,
    data: {title: 'Sign In'}
  }, {
    path: '',
    component: SignedInComponent,
    canActivate: [SignInGuard],
    canActivateChild: [SignInGuard, RegistrationGuard, AccessTasksGuard],
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
        path: 'nih-callback',
        component: HomepageComponent,
        data: {title: 'Homepage'},
      }, {
        path: 'workspaces',
        children: [
          {
            path: '',
            component: WorkspaceListComponent,
            data: {
              title: 'View Workspaces',
              breadcrumb: 'workspaces'
            }
          },
          {
            /* TODO The children under ./views need refactoring to use the data
             * provided by the route rather than double-requesting it.
             */
            path: ':ns/:wsid',
            component: WorkspaceWrapperComponent,
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
                  breadcrumb: 'workspace'
                }
              }, {
                path: 'edit',
                component: WorkspaceEditComponent,
                data: {
                  title: 'Edit Workspace',
                  mode: WorkspaceEditMode.Edit,
                  breadcrumb: 'workspaceEdit'
                }
              }, {
                path: 'clone',
                component: WorkspaceEditComponent,
                data: {
                  title: 'Clone Workspace',
                  mode: WorkspaceEditMode.Clone,
                  breadcrumb: 'workspaceClone'
                }
              },
              {
                path: 'notebooks',
                children: [
                  {
                    path: '',
                    component: NotebookListComponent,
                    data: {
                      title: 'View Notebooks',
                      breadcrumb: 'workspace'
                    }
                  }, {
                    path: ':nbName',
                    component: NotebookRedirectComponent,
                    data: {
                      title: 'Notebook',
                      breadcrumb: 'notebook',
                      minimizeChrome: true
                    }
                  }
                ]
              }, {
                path: 'cohorts',
                children: [
                  {
                    path: '',
                    component: CohortListComponent,
                    data: {
                      title: 'View Cohorts',
                      breadcrumb: 'workspace'
                    },
                  },
                  {
                    path: 'build',
                    loadChildren: './cohort-search/cohort-search.module#CohortSearchModule',
                  },
                  {
                    path: ':cid/review',
                    loadChildren: './cohort-review/cohort-review.module#CohortReviewModule',
                    data: {
                      title: 'Cohort',
                    },
                    resolve: {
                      cohort: CohortResolver,
                    }
                  }
                ]
              },
              {
                path: 'concepts',
                component: ConceptHomepageComponent,
                data: {
                  title: 'Search Concepts',
                  breadcrumb: 'workspace'
                }
              },
              {
                path: 'data',
                component: DataPageComponent,
                data: {
                  title: 'Data Page',
                  breadcrumb: 'workspace'
                }
              },
              {
                path: 'data/datasets',
                component: DataSetComponent,
                data: {
                  title: 'Dataset Page',
                  breadcrumb: 'dataset'
                }
              },
              {
                path: 'concepts/sets',
                children: [{
                  path: '',
                  component: ConceptSetListComponent,
                  data: {
                    title: 'View Concept Sets',
                    breadcrumb: 'workspace'
                  }
                }, {
                  path: ':csid',
                  component: ConceptSetDetailsComponent,
                  data: {
                    title: 'Concept Set',
                    breadcrumb: 'conceptSet'
                  },
                  resolve: {
                    conceptSet: ConceptSetResolver,
                  }
                }]
              }]
          }]
      },
      {
        path: 'admin/review-workspace',
        component: AdminReviewWorkspaceComponent,
        data: {title: 'Review Workspaces'}
      }, {
        path: 'admin/user',
        component: AdminUserComponent,
        data: {title: 'User Admin Table'}
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
      }
    ]
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes,
    {onSameUrlNavigation: 'reload', paramsInheritanceStrategy: 'always'})],
  exports: [RouterModule],
  providers: [
    ConceptSetResolver,
    AccessTasksGuard,
    RegistrationGuard,
    SignInGuard,
    WorkspaceResolver,
  ]
})
export class AppRoutingModule {

  constructor(public router: Router) {
    NavStore.navigate = (commands, extras) => this.router.navigate(commands, extras);
    NavStore.navigateByUrl = (url, extras) => this.router.navigateByUrl(url, extras);
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        gtag('config', environment.gaId, { 'page_path': event.urlAfterRedirects });
      }
    });
  }
}
