import {Component, NgZone, OnDestroy, OnInit} from '@angular/core';
import {Title} from '@angular/platform-browser';
import {
  ActivatedRoute,
  NavigationEnd,
  NavigationError,
  Router,
  RouterEvent,
} from '@angular/router';
import {buildPageTitleForEnvironment} from 'app/utils/title';
import * as fp from 'lodash/fp';

import {StackdriverErrorReporter} from 'stackdriver-errors-js';

import {initializeAnalytics} from 'app/utils/analytics';
import {cookiesEnabled, LOCAL_STORAGE_API_OVERRIDE_KEY} from 'app/utils/cookies';
import {
  currentWorkspaceStore,
  nextWorkspaceWarmupStore,
  queryParamsStore,
  routeConfigDataStore,
  setSidebarActiveIconStore,
  urlParamsStore
} from 'app/utils/navigation';
import {routeDataStore, runtimeStore, serverConfigStore, stackdriverErrorReporterStore} from 'app/utils/stores';
import {environment} from 'environments/environment';

import {LOCAL_STORAGE_KEY_SIDEBAR_STATE} from 'app/components/help-sidebar';
import {configApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import {ExceededActionCountError, LeoRuntimeInitializer} from 'app/utils/leo-runtime-initializer';
import outdatedBrowserRework from 'outdated-browser-rework';

@Component({
  selector: 'app-aou',
  styleUrls: ['./component.css',
    '../../styles/buttons.css'],
  templateUrl: './component.html'
})
export class AppComponent implements OnInit, OnDestroy {
  cookiesEnabled = true;
  configLoaded = false;
  overriddenUrl: string = null;
  pollAborter = new AbortController();

  private subscriptions = [];

  constructor(
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private titleService: Title,
    private zone: NgZone
  ) {}

  async ngOnInit() {
    this.cookiesEnabled = cookiesEnabled();
    // Local storage breaks if cookies are not enabled
    if (this.cookiesEnabled) {
      try {
        this.overriddenUrl = localStorage.getItem(LOCAL_STORAGE_API_OVERRIDE_KEY);
        window['setAllOfUsApiUrl'] = (url: string) => {
          if (url) {
            if (!url.match(/^https?:[/][/][a-z0-9.:-]+$/)) {
              throw new Error('URL should be of the form "http[s]://host.example.com[:port]"');
            }
            this.overriddenUrl = url;
            localStorage.setItem(LOCAL_STORAGE_API_OVERRIDE_KEY, url);
          } else {
            this.overriddenUrl = null;
            localStorage.removeItem(LOCAL_STORAGE_API_OVERRIDE_KEY);
          }
          window.location.reload();
        };
        console.log('To override the API URLs, try:\n' +
            'setAllOfUsApiUrl(\'https://host.example.com:1234\')');
      } catch (err) {
        console.log('Error setting urls: ' + err);
      }
    }

    this.subscriptions.push(this.router.events.subscribe((e: RouterEvent) => {
      this.setTitleFromRoute(e);
      if (e instanceof NavigationEnd) {
        if (!this.activatedRoute) {
          return;
        }
        const {snapshot: {params, queryParams, routeConfig}} = this.getLeafRoute();
        urlParamsStore.next(params);
        queryParamsStore.next(queryParams);
        routeConfigDataStore.next(routeConfig.data);
      }
    }));

    this.subscriptions.push(routeDataStore.subscribe(({title, pathElementForTitle}) => {
      this.zone.run(() => {
        this.setTitleFromReactRoute({title, pathElementForTitle});
      });
    }));


    // TODO angular2react: this active icon stuff can move into help-sidebar once it reaches a state where it
    // doesn't remount on every navigation event
    setSidebarActiveIconStore.next(localStorage.getItem(LOCAL_STORAGE_KEY_SIDEBAR_STATE));

    this.subscriptions.push(routeDataStore.subscribe((newRoute, oldRoute) => {
      if (!fp.isEmpty(oldRoute) && !fp.isEqual(newRoute, oldRoute)) {
        setSidebarActiveIconStore.next(null);
      }
    }));

    await this.loadConfig();
    this.loadErrorReporter();
    initializeAnalytics();
  }

  getLeafRoute(route = this.activatedRoute) {
    return route.firstChild ? this.getLeafRoute(route.firstChild) : route;
  }

  private titleFromPathElement(currentRoute, pathElement?): string|undefined {
    if (!pathElement) {
      return undefined;
    }
    const value = currentRoute.params.getValue()[pathElement];
    if (!value) {
      return undefined;
    }
    return decodeURIComponent(value);
  }

  /**
   * Uses the title service to set the page title after navigation events
   */
  private setTitleFromRoute(event: RouterEvent): void {
    if (event instanceof NavigationEnd) {

      const currentRoute = this.getLeafRoute();
      if (currentRoute.outlet === 'primary') {
        currentRoute.data.subscribe(value => {
          const routeTitle = value.title || this.titleFromPathElement(currentRoute, value.pathElementForTitle);
          this.titleService.setTitle(buildPageTitleForEnvironment(routeTitle));
        });
      }
    }
  }

  private setTitleFromReactRoute({title = '', pathElementForTitle}: {title: string, pathElementForTitle?: string}): void {
    const currentRoute = this.getLeafRoute();
    if (currentRoute.outlet === 'primary') {
      currentRoute.data.subscribe(() => {
        const routeTitle = title || this.titleFromPathElement(currentRoute, pathElementForTitle);
        this.titleService.setTitle(buildPageTitleForEnvironment(routeTitle));
      });
    }
  }



  ngOnDestroy() {
    this.subscriptions.forEach(sub => sub.unsubscribe());
  }

  private async loadConfig() {
    const config = await configApi().getConfig();
    serverConfigStore.set({config: config});
  }

  private loadErrorReporter() {
    // We don't report to stackdriver on local servers.
    if (environment.debug) {
      return;
    }
    const reporter = new StackdriverErrorReporter();
    const {config} = serverConfigStore.get();
    if (!config.publicApiKeyForErrorReports) {
      return;
    }
    reporter.start({
      key: config.publicApiKeyForErrorReports,
      projectId: config.projectId,
    });
    stackdriverErrorReporterStore.set(reporter);
  }
}
