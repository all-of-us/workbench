import {NgModule} from '@angular/core';
import {NavigationEnd, Router, RouterModule, Routes} from '@angular/router';

import {NavigationGuard} from 'app/guards/navigation-guard';
import {AppRouting} from './app-routing';
import {RegistrationGuard} from './guards/registration-guard.service';
import {SignInGuard} from './guards/sign-in-guard.service';

import {SignedInComponent} from './pages/signed-in/component';
import {WorkspaceWrapperComponent} from './pages/workspace/workspace-wrapper/component';

import {environment} from 'environments/environment';
import {DisabledGuard} from './guards/disabled-guard.service';
import {WorkspaceGuard} from './guards/workspace-guard.service';
import {NavStore} from './utils/navigation';


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
    canDeactivate: [NavigationGuard],
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
      {
        path: 'ras-callback',
        component: AppRouting,
        data: {}
      },
      {
        path: 'access-renewal',
        component: AppRouting,
        data: {}
      },
      {
        path: 'profile',
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
              // legacy / duplicated routes go HERE
              {
                path: '',
                component: AppRouting,
                data: {}
              },
              {
                path: 'build',
                component: AppRouting,
                data: {}
              },
              // non-migrated routes go HERE
              {
                /* TODO The children under ./views need refactoring to use the data
                 * provided by the route rather than double-requesting it.
                 */
                path: ':ns/:wsid',
                component: WorkspaceWrapperComponent,
                runGuardsAndResolvers: 'always',
                children: [
                  // legacy / duplicated routes go HERE
                  {
                    path: 'about',
                    component: AppRouting,
                    data: {}
                  },
                  {
                    path: 'edit',
                    component: AppRouting,
                    data: {}
                  },
                  {
                    path: 'duplicate',
                    component: AppRouting,
                    data: {}
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
                    component: AppRouting,
                    children: [
                      {
                        path: '',
                        component: AppRouting,
                        data: {}
                      },
                      {
                        path: 'data-sets',
                        component: AppRouting,
                        data: {}
                      },
                      {
                        path: 'data-sets/:dataSetId',
                        component: AppRouting,
                        data: {}
                      },
                      // non-migrated routes go HERE
                      {
                        path: 'cohorts',
                        children: [
                          {
                            path: ':cid/actions',
                            component: AppRouting,
                            data: {},
                          },
                          {
                            path: 'build',
                            children: [
                              {
                                path: '',
                                component: AppRouting,
                                data: {}
                              },
                            ]
                          },
                          {
                            path: ':cid/review',
                            children: [
                              {
                                path: '',
                                component: AppRouting,
                                data: {},
                              }, {
                                path: 'participants',
                                component: AppRouting,
                                data: {},
                              }, {
                                path: 'cohort-description',
                                component: AppRouting,
                                data: {},
                              }, {
                                path: 'participants/:pid',
                                component: AppRouting,
                                data: {},
                              }
                            ],
                          }
                        ]
                      },
                      {
                        path: 'concepts',
                        children: [{
                          path: '',
                          component: AppRouting,
                          data: {}
                        }, {
                          path: ':domain',
                          component: AppRouting,
                          data: {}
                        }]
                      },
                      {
                        path: 'concepts/sets',
                        children: [
                          {
                            path: ':csid',
                            component: AppRouting,
                            data: {}
                          },
                          {
                            path: ':csid/actions',
                            component: AppRouting,
                            data: {},
                          },
                        ]
                      }
                    ]
                  }]
              }]
          }
        ]
      },
      {
        path: 'admin',
        children: [
          {
            path: 'banner',
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
          {
            path: 'review-workspace',
            component: AppRouting,
            data: {}
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
