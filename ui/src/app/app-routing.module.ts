import {NgModule} from '@angular/core';
import {NavigationEnd, Router, RouterModule, Routes} from '@angular/router';

import {DataSetGuard} from './guards/dataset-guard.service';
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
import {SignedInComponent} from './views/signed-in/component';
import {StigmatizationPageComponent} from './views/stigmatization-page/component';
import {WorkspaceEditComponent, WorkspaceEditMode} from './views/workspace-edit/component';
import {WorkspaceListComponent} from './views/workspace-list/component';
import {WorkspaceWrapperComponent} from './views/workspace-wrapper/component';
import {WorkspaceComponent} from './views/workspace/component';

import {DataPageComponent} from 'app/views/data-page/component';
import {DataUseAgreementComponent} from 'app/views/data-use-agreement/component';
import {DataSetPageComponent} from 'app/views/dataset-page/component';
import {environment} from 'environments/environment';
import {BreadcrumbType, NavStore} from './utils/navigation';
import {CohortActionsComponent} from './views/cohort-actions/cohort-actions.component';
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
    canActivateChild: [SignInGuard, RegistrationGuard],
    runGuardsAndResolvers: 'always',
    children: [
      {
        path: '',
        component: HomepageComponent,
        data: {title: 'Homepage'},
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
        path: 'data-use-agreement',
        component: DataUseAgreementComponent,
        data: {title: 'Data Use Agreement'}
      }, {
        path: 'workspaces',
        children: [
          {
            path: '',
            component: WorkspaceListComponent,
            data: {
              title: 'View Workspaces',
              breadcrumb: BreadcrumbType.Workspaces
            }
          },
          {
            /* TODO The children under ./views need refactoring to use the data
             * provided by the route rather than double-requesting it.
             */
            path: ':ns/:wsid',
            component: WorkspaceWrapperComponent,
            runGuardsAndResolvers: 'always',
            children: [
              {
                path: '',
                component: WorkspaceComponent,
                data: {
                  title: 'View Workspace Details',
                  breadcrumb: BreadcrumbType.Workspace
                }
              }, {
                path: 'edit',
                component: WorkspaceEditComponent,
                data: {
                  title: 'Edit Workspace',
                  mode: WorkspaceEditMode.Edit,
                  breadcrumb: BreadcrumbType.WorkspaceEdit
                }
              }, {
                path: 'duplicate',
                component: WorkspaceEditComponent,
                data: {
                  title: 'Duplicate Workspace',
                  mode: WorkspaceEditMode.Duplicate,
                  breadcrumb: BreadcrumbType.WorkspaceDuplicate
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
                      breadcrumb: BreadcrumbType.Workspace
                    }
                  }, {
                    path: ':nbName',
                    component: NotebookRedirectComponent,
                    data: {
                      title: 'Notebook',
                      breadcrumb: BreadcrumbType.Notebook,
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
                      breadcrumb: BreadcrumbType.Workspace
                    },
                  },
                  {
                    path: ':cid/actions',
                    component: CohortActionsComponent,
                    data: {
                      title: 'Cohort Actions',
                      breadcrumb: BreadcrumbType.Cohort
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
                  }
                ]
              },
              {
                path: 'concepts',
                component: ConceptHomepageComponent,
                data: {
                  title: 'Search Concepts',
                  breadcrumb: BreadcrumbType.Workspace
                }
              },
              {
                path: 'data',
                canActivate: [DataSetGuard],
                canActivateChild: [DataSetGuard],
                children: [
                  {
                    path: '',
                    component: DataPageComponent,
                    data: {
                      title: 'Data Page',
                      breadcrumb: BreadcrumbType.Workspace
                    }
                  },
                  {
                    path: 'data-sets',
                    component: DataSetPageComponent,
                    data: {
                      title: 'Data Set Page',
                      breadcrumb: BreadcrumbType.Dataset
                    }
                  },
                  {
                    path: 'data-sets/:dataSetId',
                    component: DataSetPageComponent,
                    data: {
                      title: 'Edit Data Set',
                      breadcrumb: BreadcrumbType.Dataset
                    }
                  }
                ]
              },
              {
                path: 'concepts/sets',
                children: [{
                  path: '',
                  component: ConceptSetListComponent,
                  data: {
                    title: 'View Concept Sets',
                    breadcrumb: BreadcrumbType.Workspace
                  }
                }, {
                  path: ':csid',
                  component: ConceptSetDetailsComponent,
                  data: {
                    title: 'Concept Set',
                    breadcrumb: BreadcrumbType.ConceptSet
                  },
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
    DataSetGuard,
    RegistrationGuard,
    SignInGuard,
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
