import {NgModule} from '@angular/core';
import {NavigationEnd, Router, RouterModule, Routes} from '@angular/router';

import {RegistrationGuard} from './guards/registration-guard.service';
import {SignInGuard} from './guards/sign-in-guard.service';

import {DataPageComponent} from 'app/pages/data/data-page';
import {DataSetPageComponent} from 'app/pages/data/data-set/dataset-page';
import {DataUseAgreementComponent} from 'app/pages/profile/data-use-agreement';
import {AdminReviewWorkspaceComponent} from './pages/admin/admin-review-workspace';
import {AdminUserComponent} from './pages/admin/admin-user';
import {NotebookListComponent} from './pages/analysis/notebook-list';
import {NotebookRedirectComponent} from './pages/analysis/notebook-redirect';
import {CohortReviewComponent} from './pages/data/cohort-review/cohort-review';
import {DetailPageComponent} from './pages/data/cohort-review/detail-page';
import {QueryReportComponent} from './pages/data/cohort-review/query-report.component';
import {TablePage} from './pages/data/cohort-review/table-page';
import {CohortActionsComponent} from './pages/data/cohort/cohort-actions';
import {ConceptHomepageComponent} from './pages/data/concept/concept-homepage';
import {ConceptSetActionsComponent} from './pages/data/concept/concept-set-actions';
import {ConceptSetDetailsComponent} from './pages/data/concept/concept-set-details';
import {HomepageComponent} from './pages/homepage/homepage';
import {SignInComponent} from './pages/login/sign-in';
import {ProfilePageComponent} from './pages/profile/profile-page';
import {SignedInComponent} from './pages/signed-in/component';
import {StigmatizationPageComponent} from './pages/workspace/stigmatization-page';
import {WorkspaceAboutComponent} from './pages/workspace/workspace-about';
import {WorkspaceEditComponent, WorkspaceEditMode} from './pages/workspace/workspace-edit';
import {WorkspaceLibraryComponent} from './pages/workspace/workspace-library';
import {WorkspaceListComponent} from './pages/workspace/workspace-list';
import {WorkspaceWrapperComponent} from './pages/workspace/workspace-wrapper/component';

import {environment} from 'environments/environment';
import {InteractiveNotebookComponent} from './pages/analysis/interactive-notebook';
import {BreadcrumbType, NavStore} from './utils/navigation';


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
        path: 'library',
        component: WorkspaceLibraryComponent,
        data: {title: 'Workspace Library'}
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
                path: 'about',
                component: WorkspaceAboutComponent,
                data: {
                  title: 'View Workspace Details',
                  breadcrumb: BreadcrumbType.Workspace,
                  helpContent: 'data'
                }
              }, {
                path: 'edit',
                component: WorkspaceEditComponent,
                data: {
                  title: 'Edit Workspace',
                  mode: WorkspaceEditMode.Edit,
                  breadcrumb: BreadcrumbType.WorkspaceEdit,
                  helpContent: 'data'
                }
              }, {
                path: 'duplicate',
                component: WorkspaceEditComponent,
                data: {
                  title: 'Duplicate Workspace',
                  mode: WorkspaceEditMode.Duplicate,
                  breadcrumb: BreadcrumbType.WorkspaceDuplicate,
                  helpContent: 'data'
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
                      breadcrumb: BreadcrumbType.Workspace,
                      helpContent: 'data'
                    }
                  }, {
                    path: ':nbName',
                    component: NotebookRedirectComponent,
                    data: {
                      // use the (urldecoded) captured value nbName
                      pathElementForTitle: 'nbName',
                      breadcrumb: BreadcrumbType.Notebook,
                      minimizeChrome: true
                    }
                  }, {
                    path: 'preview/:nbName',
                    component: InteractiveNotebookComponent,
                    data: {
                      pathElementForTitle: 'nbName',
                      breadcrumb: BreadcrumbType.Notebook,
                      minimizeChrome: true
                    }
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
                      helpContent: 'data'
                    }
                  },
                  {
                    path: 'data-sets',
                    component: DataSetPageComponent,
                    data: {
                      title: 'Dataset Page',
                      breadcrumb: BreadcrumbType.Dataset,
                      helpContent: 'datasetBuilder'
                    }
                  },
                  {
                    path: 'data-sets/:dataSetId',
                    component: DataSetPageComponent,
                    data: {
                      title: 'Edit Dataset',
                      breadcrumb: BreadcrumbType.Dataset,
                      helpContent: 'datasetBuilder'
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
                          helpContent: 'cohortBuilder'
                        },
                      },
                      {
                        path: 'build',
                        loadChildren: './cohort-search/cohort-search.module#CohortSearchModule',
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
                              helpContent: 'reviewParticipants'
                            }
                          }, {
                            path: 'participants',
                            component: TablePage,
                            data: {
                              title: 'Review Cohort Participants',
                              breadcrumb: BreadcrumbType.Cohort,
                              helpContent: 'reviewParticipants'
                            }
                          }, {
                            path: 'cohort-description',
                            component: QueryReportComponent,
                            data: {
                              title: 'Review Cohort Description',
                              breadcrumb: BreadcrumbType.Cohort,
                              helpContent: 'reviewParticipants'
                            }
                          }, {
                            path: 'participants/:pid',
                            component: DetailPageComponent,
                            data: {
                              title: 'Participant Detail',
                              breadcrumb: BreadcrumbType.Participant,
                              shouldReuse: true,
                              helpContent: 'reviewParticipantDetail'
                            }
                          }
                        ],
                      }
                    ]
                  },
                  {
                    path: 'concepts',
                    component: ConceptHomepageComponent,
                    data: {
                      title: 'Search Concepts',
                      breadcrumb: BreadcrumbType.SearchConcepts,
                      helpContent: 'conceptSets'
                    }
                  },
                  {
                    path: 'concepts/sets',
                    children: [{
                      path: ':csid',
                      component: ConceptSetDetailsComponent,
                      data: {
                        title: 'Concept Set',
                        breadcrumb: BreadcrumbType.ConceptSet,
                        helpContent: 'conceptSets'
                      },
                    }, {
                      path: ':csid/actions',
                      component: ConceptSetActionsComponent,
                      data: {
                        title: 'Concept Set Actions',
                        breadcrumb: BreadcrumbType.ConceptSet,
                        helpContent: 'conceptSets'
                      },
                    }, ]
                  }
                ]
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
