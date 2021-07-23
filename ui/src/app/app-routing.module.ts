import {NgModule, NgZone} from '@angular/core';
import {NavigationEnd, Router, RouterModule, Routes} from '@angular/router';

import {AppRouting} from './app-routing';

import {environment} from 'environments/environment';
import {NavigationGuard} from './guards/navigation-guard';
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
  {
    path: '',
    children: [
      // legacy / duplicated routes go HERE
      {
        path: 'library',
        component: AppRouting,
        data: {}
      },
      {
        path: 'workspaces',
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
          {
            /* TODO The children under ./views need refactoring to use the data
             * provided by the route rather than double-requesting it.
             */
            path: ':ns/:wsid',
            // TODO: When removing ng router, replace this with react-router's Prompt component.
            runGuardsAndResolvers: 'always',
            canDeactivate: [NavigationGuard],
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
  }
];

@NgModule({
  imports: [RouterModule.forRoot(routes,
    {onSameUrlNavigation: 'reload', paramsInheritanceStrategy: 'always'})],
  exports: [RouterModule]
})
export class AppRoutingModule {

  constructor(private router: Router, zone: NgZone) {
    NavStore.navigate = (commands, extras) => {
      zone.run(() => this.router.navigate(commands, extras));
    };
    NavStore.navigateByUrl = (url, extras) => {
      zone.run(() => this.router.navigateByUrl(url, extras));
    };
    this.router.events.subscribe(event => {
      if (event instanceof NavigationEnd) {
        gtag('config', environment.gaId, { 'page_path': event.urlAfterRedirects });
      }
    });
  }
}
