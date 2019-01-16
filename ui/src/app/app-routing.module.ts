import {NgModule} from '@angular/core';
import {NavigationEnd, Router, RouterModule, Routes} from '@angular/router';

import {RegistrationGuard} from './guards/registration-guard.service';
import {SignInGuard} from './guards/sign-in-guard.service';

import {AdminReviewIdVerificationComponent} from './views/admin-review-id-verification/component';
import {AdminReviewWorkspaceComponent} from './views/admin-review-workspace/component';
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
import {WorkspaceNavBarComponent} from './views/workspace-nav-bar/component';
import {WorkspaceComponent} from './views/workspace/component';

import {CohortResolver} from './resolvers/cohort';
import {ConceptSetResolver} from './resolvers/concept-set';
import {WorkspaceResolver} from './resolvers/workspace';

import {environment} from 'environments/environment';
import {SignInTemplateComponent} from './views/sign-in-flow-template/component';

declare let gtag: Function;

const routes: Routes = [
  {
    path: 'login',
    component: SignInTemplateComponent,
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
      data: {
        breadcrumb: {
          value: 'Workspaces',
          intermediate: true
        }
      },
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
            breadcrumb: {
              value: 'Param: Workspace Name',
            }
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
              }
            }, {
              path: 'edit',
              component: WorkspaceEditComponent,
              data: {
                title: 'Edit Workspace',
                mode: WorkspaceEditMode.Edit,
                breadcrumb: {
                  value: 'Edit Workspace',
                }
              }
            }, {
              path: 'clone',
              component: WorkspaceEditComponent,
              data: {
                title: 'Clone Workspace',
                mode: WorkspaceEditMode.Clone,
                breadcrumb: {
                  value: 'Clone Workspace',
                }
              }
            },
            {
              path: 'notebooks',
              data: {
                breadcrumb: {
                  value: 'Notebooks',
                  intermediate: true
                }
              },
              children: [
                {
                  path: '',
                  component: NotebookListComponent,
                  data: {
                    title: 'View Notebooks'
                  }
                }, {
                  path: ':nbName',
                  component: NotebookRedirectComponent,
                  data: {
                    title: 'Notebook',
                    breadcrumb: {
                        value: 'Param: Notebook Name'
                    },
                    minimizeChrome: true
                  }
                }
              ]
            }, {
              path: 'cohorts',
              data: {
                breadcrumb: {
                  value: 'Cohorts',
                  intermediate: true
                }
              },
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
                    breadcrumb: {
                      value: 'Add a Cohort',
                    }
                  }
                },
                {
                  path: ':cid/review',
                  loadChildren: './cohort-review/cohort-review.module#CohortReviewModule',
                  data: {
                    title: 'Cohort',
                    breadcrumb: {
                      value: 'Param: Cohort Name',
                    }
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
                breadcrumb: {
                  value: 'Concepts',
                  intermediate: true
                }
              }
            },
            {
              path: 'concepts/sets',
              data: {
                breadcrumb: {
                  value: 'Concept Sets',
                  intermediate: true
                }
              },
              children: [{
                path: '',
                component: ConceptSetListComponent,
                data: {
                  title: 'View Concept Sets',
                }
              }, {
                path: ':csid',
                component: ConceptSetDetailsComponent,
                data: {
                  title: 'Concept Set',
                  breadcrumb: {
                    value: 'Param: Concept Set Name',
                  }
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
    RegistrationGuard,
    SignInGuard,
    WorkspaceResolver,
  ]
})
export class AppRoutingModule {

 constructor(public router: Router) {
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        gtag('config', environment.gaId, { 'page_path': event.urlAfterRedirects });
      }
    });
  }
}
