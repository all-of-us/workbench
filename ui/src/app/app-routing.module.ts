import {NgModule} from '@angular/core';
import {NavigationEnd, Router, RouterModule, Routes} from '@angular/router';

import {AppRouting} from './app-routing';
import {CanDeactivateGuard} from './guards/can-deactivate-guard.service';
import {RegistrationGuard} from './guards/registration-guard.service';
import {SignInGuard} from './guards/sign-in-guard.service';

import {DataPageComponent} from 'app/pages/data/data-page';
import {DataSetPageComponent} from 'app/pages/data/data-set/dataset-page';
import {CohortPageComponent} from './cohort-search/cohort-page/cohort-page.component';
import {AdminBannerComponent} from './pages/admin/admin-banner';
import {AdminReviewWorkspaceComponent} from './pages/admin/admin-review-workspace';
import {CohortReviewComponent} from './pages/data/cohort-review/cohort-review';
import {DetailPageComponent} from './pages/data/cohort-review/detail-page';
import {QueryReportComponent} from './pages/data/cohort-review/query-report.component';
import {TablePage} from './pages/data/cohort-review/table-page';
import {CohortActionsComponent} from './pages/data/cohort/cohort-actions';
import {ConceptHomepageComponent} from './pages/data/concept/concept-homepage';
import {ConceptSearchComponent} from './pages/data/concept/concept-search';
import {ConceptSetActionsComponent} from './pages/data/concept/concept-set-actions';
import {ProfilePageComponent} from './pages/profile/profile-page';
import {SignedInComponent} from './pages/signed-in/component';
import {WorkspaceAboutComponent} from './pages/workspace/workspace-about';
import {WorkspaceEditComponent, WorkspaceEditMode} from './pages/workspace/workspace-edit';
import {WorkspaceListComponent} from './pages/workspace/workspace-list';
import {WorkspaceWrapperComponent} from './pages/workspace/workspace-wrapper/component';

import {environment} from 'environments/environment';
import {DisabledGuard} from './guards/disabled-guard.service';
import {WorkspaceGuard} from './guards/workspace-guard.service';
import {BreadcrumbType, NavStore} from './utils/navigation';


declare let gtag: Function;

const routes: Routes = [
  // NOTE: Instead of using Angular wildcard routes, which short-circuit further route
  // discovery and behave strangely with the Angular router anyways, we're going to explicitly
  // list each React route here with component: AppRouting and just delete the entire file
  // when we migrate away from Angular.
  {
    path: 'cookie-policy',
    component: AppRouting
  },
  {
    path: 'login',
    component: AppRouting
  },
  {
    path: 'session-expired',
    component: AppRouting
  },
  {
    path: 'sign-in-again',
    component: AppRouting
  },
  {
    path: 'user-disabled',
    component: AppRouting
  },
  {
    path: '',
    component: SignedInComponent,
    canActivate: [SignInGuard],
    canActivateChild: [SignInGuard, DisabledGuard],
    runGuardsAndResolvers: 'always',
    children: [
      // legacy / duplicated routes go HERE
      {
        path: '',
        component: AppRouting,
        data: {}
      },
      {
        path: 'data-code-of-conduct',
        component: AppRouting,
        data: {}
      },
      {
        path: 'nih-callback',
        component: AppRouting,
        data: {}
      },
      // non-migrated routes go HERE
      {
        path: '',
        canActivateChild: [RegistrationGuard],
        runGuardsAndResolvers: 'always',
        children: [
          // legacy / duplicated routes go HERE
          {
            path: 'library',
            component: AppRouting,
            data: {}
          },
          // non-migrated routes go HERE
          {
            path: 'workspaces',
            canActivateChild: [WorkspaceGuard],
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
                    path: 'about',
                    component: WorkspaceAboutComponent,
                    data: {
                      title: 'View Workspace Details',
                      breadcrumb: BreadcrumbType.Workspace,
                      helpContentKey: 'about'
                    }
                  },
                  {
                    path: 'edit',
                    component: WorkspaceEditComponent,
                    data: {
                      title: 'Edit Workspace',
                      mode: WorkspaceEditMode.Edit,
                      breadcrumb: BreadcrumbType.WorkspaceEdit,
                      helpContentKey: 'edit'
                    }
                  },
                  {
                    path: 'duplicate',
                    component: WorkspaceEditComponent,
                    data: {
                      title: 'Duplicate Workspace',
                      mode: WorkspaceEditMode.Duplicate,
                      breadcrumb: BreadcrumbType.WorkspaceDuplicate,
                      helpContentKey: 'duplicate'
                    }
                  },
                  {
                    path: 'notebooks',
                    children: [
                      {
                        path: '',
                        component: AppRouting,
                        data: {}
                      },
                      {
                        path: ':nbName',
                        component: AppRouting,
                        data: {}
                      },
                      {
                        path: 'preview/:nbName',
                        component: AppRouting,
                        data: {}
                      }
                    ]
                  },
                  {
                    path: 'data',
                    children: [
                      {
                        path: '',
                        component: DataPageComponent,
                        data: {
                          title: 'Data Page',
                          breadcrumb: BreadcrumbType.Workspace,
                          helpContentKey: 'data'
                        }
                      },
                      {
                        path: 'data-sets',
                        component: DataSetPageComponent,
                        data: {
                          title: 'Dataset Page',
                          breadcrumb: BreadcrumbType.Dataset,
                          helpContentKey: 'datasetBuilder'
                        }
                      },
                      {
                        path: 'data-sets/:dataSetId',
                        component: DataSetPageComponent,
                        data: {
                          title: 'Edit Dataset',
                          breadcrumb: BreadcrumbType.Dataset,
                          helpContentKey: 'datasetBuilder'
                        }
                      }, {
                        path: 'cohorts',
                        children: [
                          {
                            path: ':cid/actions',
                            component: CohortActionsComponent,
                            data: {
                              title: 'Cohort Actions',
                              breadcrumb: BreadcrumbType.Cohort,
                              helpContentKey: 'cohortBuilder'
                            },
                          },
                          {
                            path: 'build',
                            children: [
                              {
                                path: '',
                                component: CohortPageComponent,
                                canDeactivate: [CanDeactivateGuard],
                                data: {
                                  title: 'Build Cohort Criteria',
                                  breadcrumb: BreadcrumbType.CohortAdd,
                                  helpContentKey: 'cohortBuilder'
                                }
                              },
                            ]
                          },
                          {
                            path: ':cid/review',
                            children: [
                              {
                                path: '',
                                component: CohortReviewComponent,
                                data: {
                                  title: 'Review Cohort Participants',
                                  breadcrumb: BreadcrumbType.Cohort,
                                  helpContentKey: 'reviewParticipants'
                                }
                              }, {
                                path: 'participants',
                                component: TablePage,
                                data: {
                                  title: 'Review Cohort Participants',
                                  breadcrumb: BreadcrumbType.Cohort,
                                  helpContentKey: 'reviewParticipants'
                                }
                              }, {
                                path: 'cohort-description',
                                component: QueryReportComponent,
                                data: {
                                  title: 'Review Cohort Description',
                                  breadcrumb: BreadcrumbType.Cohort,
                                  helpContentKey: 'cohortDescription'
                                }
                              }, {
                                path: 'participants/:pid',
                                component: DetailPageComponent,
                                data: {
                                  title: 'Participant Detail',
                                  breadcrumb: BreadcrumbType.Participant,
                                  shouldReuse: true,
                                  helpContentKey: 'reviewParticipantDetail'
                                }
                              }
                            ],
                          }
                        ]
                      },
                      {
                        path: 'concepts',
                        children: [{
                          path: '',
                          component: ConceptHomepageComponent,
                          data: {
                            title: 'Search Concepts',
                            breadcrumb: BreadcrumbType.SearchConcepts,
                            helpContentKey: 'conceptSets'
                          }
                        }, {
                          path: ':domain',
                          component: ConceptSearchComponent,
                          canDeactivate: [CanDeactivateGuard],
                          data: {
                            title: 'Search Concepts',
                            breadcrumb: BreadcrumbType.SearchConcepts,
                            helpContentKey: 'conceptSets'
                          }
                        }]
                      },
                      {
                        path: 'concepts/sets',
                        children: [{
                          path: ':csid',
                          component: ConceptSearchComponent,
                          canDeactivate: [CanDeactivateGuard],
                          data: {
                            title: 'Concept Set',
                            breadcrumb: BreadcrumbType.ConceptSet,
                            helpContentKey: 'conceptSets'
                          },
                        }, {
                          path: ':csid/actions',
                          component: ConceptSetActionsComponent,
                          data: {
                            title: 'Concept Set Actions',
                            breadcrumb: BreadcrumbType.ConceptSet,
                            helpContentKey: 'conceptSets'
                          },
                        }, ]
                      }
                    ]
                  }]
              }]
          },
          {
            path: 'profile',
            component: ProfilePageComponent,
            data: {title: 'Profile'}
          },
          {
            path: 'workspaces/build',
            component: WorkspaceEditComponent,
            data: {title: 'Create Workspace', mode: WorkspaceEditMode.Create}
          }
        ]
      },
      {
        path: 'admin',
        children: [
          // legacy / duplicated routes go HERE
          {
            path: 'institution',
            component: AppRouting,
            data: {}
          },
          {
            path: 'institution/add',
            component: AppRouting,
            data: {},
          },
          {
            path: 'institution/edit/:institutionId',
            component: AppRouting,
            data: {},
          },
          {
            path: 'user', // included for backwards compatibility
            component: AppRouting,
            data: {}
          },
          {
            path: 'users',
            component: AppRouting,
            data: {}
          },
          {
            path: 'users/:usernameWithoutGsuiteDomain',
            component: AppRouting,
            data: {title: 'User Admin'}
          },
          {
            path: 'user-audit',
            component: AppRouting,
            data: {}
          },
          {
            path: 'user-audit/:username',
            component: AppRouting,
            data: {}
          },
          {
            path: 'workspace-audit',
            component: AppRouting,
            data: {}
          },
          {
            path: 'workspace-audit/:workspaceNamespace',
            component: AppRouting,
            data: {}
          },
          {
            path: 'workspaces/:workspaceNamespace/:nbName',
            component: AppRouting,
            data: {}
          },
          {
            path: 'workspaces',
            component: AppRouting,
            data: {},
          },
          {
            path: 'workspaces/:workspaceNamespace',
            component: AppRouting,
            data: {}
          },
          {
            path: 'institution',
            component: AppRouting,
            data: {}
          },
          {
            path: 'institution/add',
            component: AppRouting,
            data: {},
          },
          {
            path: 'institution/edit/:institutionId',
            component: AppRouting,
            data: {},
          },
          // non-migrated routes go HERE
          {
            path: 'review-workspace',
            component: AdminReviewWorkspaceComponent,
            data: {title: 'Review Workspaces'}
          },
          {
            path: 'banner',
            component: AdminBannerComponent,
            data: {title: 'Create Banner'}
          }
        ]
      },
    ]
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes,
    {onSameUrlNavigation: 'reload', paramsInheritanceStrategy: 'always'})],
  exports: [RouterModule],
  providers: [
    DisabledGuard,
    RegistrationGuard,
    SignInGuard,
    WorkspaceGuard
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
