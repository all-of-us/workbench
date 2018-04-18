import {Location} from '@angular/common';
import {Component, OnInit} from '@angular/core';
import {Title} from '@angular/platform-browser';
import {
  ActivatedRoute,
  Event as RouterEvent,
  NavigationEnd,
  Router,
} from '@angular/router';


import {environment} from 'environments/environment';

export const overriddenUrlKey = 'allOfUsApiUrlOverride';
export const overriddenPublicUrlKey = 'publicApiUrlOverride';


@Component({
  selector: 'app-aou',
  styleUrls: ['./component.css',
              '../../styles/buttons.css'],
  templateUrl: './component.html'
})
export class AppComponent implements OnInit {
  isSignedIn = false;
  overriddenUrl: string = null;
  private baseTitle: string;
  private overriddenPublicUrl: string = null;


  constructor(
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private titleService: Title
  ) {}

  ngOnInit(): void {
    this.overriddenUrl = localStorage.getItem(overriddenUrlKey);
    this.overriddenPublicUrl = localStorage.getItem(overriddenPublicUrlKey);
    window['setAllOfUsApiUrl'] = (url: string) => {
      if (url) {
        if (!url.match(/^https?:[/][/][a-z0-9.:-]+$/)) {
          throw new Error('URL should be of the form "http[s]://host.example.com[:port]"');
        }
        this.overriddenUrl = url;
        localStorage.setItem(overriddenUrlKey, url);
      } else {
        this.overriddenUrl = null;
        localStorage.removeItem(overriddenUrlKey);
      }
      window.location.reload();
    };
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

    this.router.events.subscribe((event: RouterEvent) => {
      this.setTitleFromRoute(event);
    });
  }

  /**
   * Uses the title service to set the page title after nagivation events
   */
  private setTitleFromRoute(event: RouterEvent): void {
    if (event instanceof NavigationEnd) {

      let currentRoute = this.activatedRoute;
      while (currentRoute.firstChild) {
        currentRoute = currentRoute.firstChild;
      }
      if (currentRoute.outlet === 'primary') {
        currentRoute.data.subscribe(value =>
            this.titleService.setTitle(`${value.title} | ${this.baseTitle}`));
      }
    }
  }

}
