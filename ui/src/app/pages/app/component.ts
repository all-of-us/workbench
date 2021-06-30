import {Component, NgZone, OnInit} from '@angular/core';
import {Title} from '@angular/platform-browser';
import {
  ActivatedRoute,
  Event as RouterEvent,
  NavigationEnd,
  NavigationError,
  Router,
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
  routeConfigDataStore
} from 'app/utils/navigation';
import {routeDataStore, runtimeStore, serverConfigStore, stackdriverErrorReporterStore} from 'app/utils/stores';
import {environment} from 'environments/environment';

import {configApi, workspacesApi} from 'app/services/swagger-fetch-clients';
import outdatedBrowserRework from 'outdated-browser-rework';
import {ExceededActionCountError, LeoRuntimeInitializer} from '../../utils/leo-runtime-initializer';
import {urlParamsStore} from '../../utils/url-params-store';

@Component({
  selector: 'app-aou',
  styleUrls: ['./component.css',
    '../../styles/buttons.css'],
  templateUrl: './component.html'
})
export class AppComponent implements OnInit {
  isSignedIn = false;
  initialSpinner = true;
  cookiesEnabled = true;
  overriddenUrl: string = null;
  pollAborter = new AbortController();

  constructor(
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private titleService: Title,
    private zone: NgZone
  ) {
    this.zone = zone;
  }

  async ngOnInit() {
    this.checkBrowserSupport();
    await this.loadConfig();
    this.loadErrorReporter();

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

    // Pick up the global site title from HTML, and (for non-prod) add a tag
    // naming the current environment.
    if (environment.shouldShowDisplayTag) {
      this.titleService.setTitle(buildPageTitleForEnvironment());
    }

    this.router.events.subscribe((e: RouterEvent) => {
      this.setTitleFromRoute(e);
      if (e instanceof NavigationEnd || e instanceof NavigationError) {
        // Terminal navigation events.
        this.initialSpinner = false;
      }
      if (e instanceof NavigationEnd) {
        const {snapshot: {params, queryParams, routeConfig}} = this.getLeafRoute();
        console.log('This is where the params are coming from');
        console.log(params);
        urlParamsStore.next(params);
        console.log(queryParams);
        queryParamsStore.next(queryParams);
        console.log(routeConfig.data);
        routeConfigDataStore.next(routeConfig.data);
      }
    });

    routeDataStore.subscribe(({title, pathElementForTitle}) => {
      this.zone.run(() => {
        this.setTitleFromReactRoute({title, pathElementForTitle});
        this.initialSpinner = false;
      });
    });

    urlParamsStore
      .map(({ns, wsid}) => ({ns, wsid}))
      .distinctUntilChanged((x,y) => {
        console.log(fp.isEqual(x)(y));
        return fp.isEqual(x)(y);
      })
      .switchMap(({ns, wsid}) => {
        // This needs to happen for testing because we seed the urlParamsStore with {}.
        // Otherwise it tries to make an api call with undefined, because the component
        // initializes before we have access to the route.
        if (ns === undefined || wsid === undefined) {
          return Promise.resolve(null);
        }

        // In a handful of situations - namely on workspace creation/clone,
        // the application will preload the next workspace to avoid a redundant
        // refetch here.
        const nextWs = nextWorkspaceWarmupStore.getValue();
        nextWorkspaceWarmupStore.next(undefined);
        if (nextWs && nextWs.namespace === ns && nextWs.id === wsid) {
          console.log("Resolving from the next workspace store");
          return Promise.resolve(nextWs);
        }
        return workspacesApi().getWorkspace(ns, wsid).then((wsResponse) => {
          return {
            ...wsResponse.workspace,
            accessLevel: wsResponse.accessLevel
          };
        });
      })
      .subscribe(async(workspace) => {
        console.log(workspace);
        if (workspace === null) {
          // This handles the empty urlParamsStore story.
          return;
        }
        console.log('setting store through url params');
        currentWorkspaceStore.next(workspace);
        console.log('setting store through url params - 1');
        runtimeStore.set({workspaceNamespace: workspace.namespace, runtime: undefined});
        console.log('setting store through url params - 2');
        this.pollAborter.abort();
        console.log('setting store through url params - 3');
        this.pollAborter = new AbortController();
        console.log('setting store through url params - 4');
        try {
          console.log('setting store through url params - 5');
          await LeoRuntimeInitializer.initialize({
            workspaceNamespace: workspace.namespace,
            pollAbortSignal: this.pollAborter.signal,
            maxCreateCount: 0,
            maxDeleteCount: 0,
            maxResumeCount: 0
          });
          console.log('setting store through url params - 6');
        } catch (e) {
          console.log('setting store through url params - 7');
          // Ignore ExceededActionCountError. This is thrown when the runtime doesn't exist, or
          // isn't started. Both of these scenarios are expected, since we don't want to do any lazy
          // initialization here.
          if (!(e instanceof ExceededActionCountError)) {
            throw e;
          }
        }
      });

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

  private checkBrowserSupport() {
    const minChromeVersion = 67;

    outdatedBrowserRework({
      browserSupport: {
        Chrome: minChromeVersion, // Includes Chrome for mobile devices
        Edge: false,
        Safari: false,
        'Mobile Safari': false,
        Opera: false,
        Firefox: false,
        Vivaldi: false,
        IE: false
      },
      isUnknownBrowserOK: false,
      messages: {
        en: {
          outOfDate: 'Researcher Workbench may not function correctly in this browser.',
          update: {
            web: `If you experience issues, please install Google Chrome \
            version ${minChromeVersion} or greater.`,
            googlePlay: 'Please install Chrome from Google Play',
            appStore: 'Please install Chrome from the App Store'
          },
          url: 'https://www.google.com/chrome/',
          callToAction: 'Download Chrome now',
          close: 'Close'
        }
      }
    });
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
