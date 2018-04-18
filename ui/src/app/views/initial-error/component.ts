import {Location} from '@angular/common';
import {Component, Injectable} from '@angular/core';
import {NavigationError, Router} from '@angular/router';

// Mostly borrowed from the Job Manager.
@Component({
    selector: 'app-initial-error',
    templateUrl: './component.html',
    styleUrls: ['./component.css']
})
export class InitialErrorComponent {
  initialLoadErrorHeader: string;
  initialLoadFailure = false;

  constructor(router: Router, private loc: Location) {
    router.events.subscribe((e) => {
      if (e instanceof NavigationError && !router.navigated) {
        /*
         * Hack: On navigation failure, Angular strips the path from the URL.
         * This is undesirable in the event of a 500/503 on initial load refresh
         * the page, or on other errors where the user may want to inspect the
         * URL. Restore the URL here; luckily this doesn't cause another Angular
         * Router nagivate.
         */
        loc.replaceState(e.url);
        this.initialLoadFailure = true;
        const status = e.error.status || 'unknown';
        const title = e.error.title || 'Unknown error';
        this.initialLoadErrorHeader = `${status}: ${title}`;
      }
      if (router.navigated) {
        /*
         * In the event that one of our resolvers/activators did another
         * navigate on failure, hide the error. Unexpected and not ideal as we'd
         * briefly flash the error html on screen.
         */
        this.initialLoadFailure = false;
      }
    });
  }
}
