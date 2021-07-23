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
import {SignInService} from 'app/services/sign-in.service';
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
  initialSpinner = true;
  cookiesEnabled = true;
  configLoaded = false;
  overriddenUrl: string = null;
  pollAborter = new AbortController();

  private subscriptions = [];

  constructor(
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private titleService: Title,
    private zone: NgZone,
    private signInService: SignInService
  ) {}

  async ngOnInit() {
    this.checkBrowserSupport();

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

    this.subscriptions.push(this.router.events.subscribe((e: RouterEvent) => {
      this.setTitleFromRoute(e);
      if (e instanceof NavigationEnd || e instanceof NavigationError) {
        // Terminal navigation events.
        this.initialSpinner = false;
      }
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
        this.initialSpinner = false;
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

    this.subscriptions.push(urlParamsStore
      .map(({ns, wsid}) => ({ns, wsid}))
      .debounceTime(1000) // Kind of hacky but this prevents multiple update requests going out simultaneously
      // due to urlParamsStore being updated multiple times while rendering a route.
      // What we really want to subscribe to here is an event that triggers on navigation start or end
      // Debounce 1000 (ms) will throttle the output events to once a second which should be OK for real life usage
      // since multiple update recent workspace requests (from the same page) within the span of 1 second should
      // almost always be for the same workspace and extremely rarely for different workspaces
      .subscribe(({ns, wsid}) => {
        if (ns && wsid) {
          workspacesApi().updateRecentWorkspaces(ns, wsid);
        }
      }));

    this.subscriptions.push(urlParamsStore
      .map(({ns, wsid}) => ({ns, wsid}))
      .distinctUntilChanged(fp.isEqual)
      .switchMap(async({ns, wsid}) => {
        currentWorkspaceStore.next(null);

        // This needs to happen for testing because we seed the urlParamsStore with {}.
        // Otherwise it tries to make an api call with undefined, because the component
        // initializes before we have access to the route.
        if (!ns || !wsid) {
          return null;
        }

        // In a handful of situations - namely on workspace creation/clone,
        // the application will preload the next workspace to avoid a redundant
        // refetch here.
        const nextWs = nextWorkspaceWarmupStore.getValue();
        nextWorkspaceWarmupStore.next(undefined);
        if (nextWs && nextWs.namespace === ns && nextWs.id === wsid) {
          return nextWs;
        }

        // Hack to ensure auth is loaded before a workspaces API call.
        await this.signInService.isSignedIn$.first().toPromise();

        return await workspacesApi().getWorkspace(ns, wsid).then((wsResponse) => {
          return {
            ...wsResponse.workspace,
            accessLevel: wsResponse.accessLevel
          };
        });
      })
      .subscribe(async(workspace) => {
        if (workspace === null) {
          // This handles the empty urlParamsStore story.
          return;
        }
        currentWorkspaceStore.next(workspace);
        runtimeStore.set({workspaceNamespace: workspace.namespace, runtime: undefined});
        this.pollAborter.abort();
        this.pollAborter = new AbortController();
        try {
          await LeoRuntimeInitializer.initialize({
            workspaceNamespace: workspace.namespace,
            pollAbortSignal: this.pollAborter.signal,
            maxCreateCount: 0,
            maxDeleteCount: 0,
            maxResumeCount: 0
          });
        } catch (e) {
          // Ignore ExceededActionCountError. This is thrown when the runtime doesn't exist, or
          // isn't started. Both of these scenarios are expected, since we don't want to do any lazy
          // initialization here.
          if (!(e instanceof ExceededActionCountError)) {
            throw e;
          }
        }
      })
    );

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
