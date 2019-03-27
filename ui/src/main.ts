// The main file is boilerplate; AppModule configures our app.

import {enableProdMode} from '@angular/core';
import {platformBrowserDynamic} from '@angular/platform-browser-dynamic';
import {environment} from 'environments/environment';
import outdatedBrowserRework from 'outdated-browser-rework';
import * as ReactModal from 'react-modal';

import {AppModule} from 'app/app.module';
import {setupCustomValidators} from 'app/services/setup';


if (!environment.debug) {
  enableProdMode();
}

setupCustomValidators();
ReactModal.defaultStyles.overlay = {};
ReactModal.defaultStyles.content = {};
platformBrowserDynamic().bootstrapModule(AppModule);
checkBrowserSupport();

function checkBrowserSupport() {
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
