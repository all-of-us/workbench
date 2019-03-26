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
  outdatedBrowserRework({
    browserSupport: {
      Chrome: 67, // Includes Chrome for mobile devices
      Edge: false,
      Safari: false,
      "Mobile Safari": false,
      Opera: false,
      Firefox: false,
      Vivaldi: false,
      IE: false
    },
    isUnknownBrowserOK: false,
    messages: {
      en: {
        outOfDate: "Terra may not function correctly in this browser.",
        update: {
          web: "If you experience issues, please try " + (!!window['chrome']? "updating" : "using") + " Google Chrome.",
          googlePlay: "Please install Chrome from Google Play",
          appStore: "Please update iOS from the Settings App"
        },
        url: "https://www.google.com/chrome/",
        callToAction: (!!window['chrome'] ? "Update" : "Download") + " Chrome now",
        close: "Close"
      }
    }
  });
}