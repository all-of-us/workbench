import { DOCUMENT, Location } from '@angular/common';
import { Component, ElementRef, HostListener, Inject, OnInit, ViewChild } from '@angular/core';
import 'rxjs/add/operator/filter';
import 'rxjs/add/operator/map';
import 'rxjs/add/operator/mergeMap';

import { Title } from '@angular/platform-browser';
import {
  ActivatedRoute,
  Event as RouterEvent,
  NavigationEnd, NavigationStart,
  Router,
} from '@angular/router';

import { Observable } from 'rxjs/Observable';

import { environment } from 'environments/environment';

import { SignInService } from 'app/services/sign-in.service';

export const overriddenUrlKey = 'allOfUsApiUrlOverride';
export const overriddenPublicUrlKey = 'publicApiUrlOverride';


@Component({
  selector: 'app-public-aou',
  styleUrls: ['./app.component.css'],
  templateUrl: './app.component.html'
})
export class AppComponent implements OnInit {
  overriddenUrl: string = null;
  private baseTitle: string;
  private overriddenPublicUrl: string = null;
  public noHeaderMenu = false;
  signedIn = false;

  constructor(
    /* Ours */
    @Inject(DOCUMENT) private doc: any,
    /* Angular's */
    private activatedRoute: ActivatedRoute,
    private locationService: Location,
    private router: Router,
    private signInService: SignInService,
    private titleService: Title
  ) { }

  ngOnInit(): void {
    this.overriddenUrl = localStorage.getItem(overriddenUrlKey);
    this.overriddenPublicUrl = localStorage.getItem(overriddenPublicUrlKey);

    this.signInService.isSignedIn$.subscribe((isSignedIn) => {
      this.signedIn = isSignedIn;
    });

    window['setPublicApiUrl'] = (url: string) => {
      if (url) {
        if (!url.match(/^https?:[/][/][a-z0-9.:-]+$/)) {
          throw new Error('URL should be of the form "http[s]://host.example.com[:port]"');
        }
        this.overriddenPublicUrl = url;
        localStorage.setItem(overriddenPublicUrlKey, url);
      } else {
        this.overriddenPublicUrl = null;
        localStorage.removeItem(overriddenPublicUrlKey);
      }
      window.location.reload();
    };
    console.log('activatedRoute ', this.activatedRoute);
    console.log('To override the API URLs, try:\n' +
      'setAllOfUsApiUrl(\'https://host.example.com:1234\')\n' +
      'setPublicApiUrl(\'https://host.example.com:5678\')');

    // Pick up the global site title from HTML, and (for non-prod) add a tag
    // naming the current environment.
    this.baseTitle = this.titleService.getTitle();
    if (environment.displayTag) {
      this.baseTitle = `[${environment.displayTag}] ${this.baseTitle}`;
      this.titleService.setTitle(this.baseTitle);
    }

    this.router.events
      .filter((event) => event instanceof NavigationEnd)
      .subscribe((event: RouterEvent) => {
        // Set the db header no menu if we are on home page
        // Not sure why an instance of RouteConfigLoadStart comes in here when we filter
        if (event instanceof NavigationEnd && event.url === '/') {
          this.noHeaderMenu = true;
        }
        this.setTitleFromRoute(event);
      });

    this.setGTagManager();
    this.setTCellAgent();
  }

  /**
   * Uses the title service to set the page title after nagivation events
   */
  private setTitleFromRoute(event: RouterEvent): void {
    let currentRoute = this.activatedRoute;
    while (currentRoute.firstChild) {
      currentRoute = currentRoute.firstChild;
    }
    if (currentRoute.outlet === 'primary') {
      currentRoute.data.subscribe(value =>
        this.titleService.setTitle(`${value.title} | ${this.baseTitle}`));
    }
  }

  /**
   * Setting the Google Analytics ID here.
   * This first injects Google's gtag script via iife, then secondarily defines
   * the global gtag function.
   */
  private setGTagManager() {
    const s = this.doc.createElement('script');
    s.type = 'text/javascript';
    s.innerHTML =
      '(function(w,d,s,l,i){' +
      'w[l]=w[l]||[];' +
      'var f=d.getElementsByTagName(s)[0];' +
      'var j=d.createElement(s);' +
      'var dl=l!=\'dataLayer\'?\'&l=\'+l:\'\';' +
      'j.async=true;' +
      'j.src=\'https://www.googletagmanager.com/gtag/js?id=\'+i+dl;' +
      'f.parentNode.insertBefore(j,f);' +
      '})' +
      '(window, document, \'script\', \'dataLayer\', \'' + environment.gaId + '\');' +
      'window.dataLayer = window.dataLayer || [];' +
      'function gtag(){dataLayer.push(arguments);}' +
      'gtag(\'js\', new Date());' +
      'gtag(\'config\', \'' + environment.gaId + '\');';
    const head = this.doc.getElementsByTagName('head')[0];
    head.appendChild(s);
  }

  private setTCellAgent(): void {
    const s = this.doc.createElement('script');
    s.type = 'text/javascript';
    s.src = 'https://jsagent.tcell.io/tcellagent.min.js';
    s.setAttribute('tcellappid', environment.tcellappid);
    s.setAttribute('tcellapikey', environment.tcellapikey);
    const head = this.doc.getElementsByTagName('head')[0];
    head.appendChild(s);
  }

}
