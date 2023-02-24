import validate from 'validate.js';

import { environment } from 'environments/environment';
import { LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN } from 'app/utils/cookies';
import outdatedBrowserRework from 'outdated-browser-rework';

export const setupCustomValidators = () => {
  validate.validators.custom = (value, options, key, attributes) => {
    return options.fn(value, key, attributes);
  };

  validate.validators.truthiness = (value) => {
    if (!value) {
      return 'must be true';
    } else {
      return undefined;
    }
  };
};

export const exposeAccessTokenSetter = () => {
  if (environment.allowTestAccessTokenOverride) {
    // Called by e2e tests. In e2e tests we circumvent Google sign-in because
    // puppeteer could be flagged as a bot by Google sign-in.
    // @ts-ignore
    window.setTestAccessTokenOverride = (token: string) => {
      // Disclaimer: console.log statements here are unlikely to captured by
      // Puppeteer, since it typically reloads the page immediately after
      // invoking this function.
      if (token) {
        window.localStorage.setItem(LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN, token);
        location.replace('/');
      } else {
        window.localStorage.removeItem(LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN);
      }
    };
  }
};

export const checkBrowserSupport = () => {
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
      IE: false,
    },
    isUnknownBrowserOK: false,
    messages: {
      en: {
        outOfDate:
          'Researcher Workbench may not function correctly in this browser.',
        update: {
          web: `If you experience issues, please install Google Chrome \
            version ${minChromeVersion} or greater.`,
          googlePlay: 'Please install Chrome from Google Play',
          appStore: 'Please install Chrome from the App Store',
        },
        url: 'https://www.google.com/chrome/',
        callToAction: 'Download Chrome now',
        close: 'Close',
      },
    },
  });
};
