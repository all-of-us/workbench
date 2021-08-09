// The main file is boilerplate; AppModule configures our app.

import * as React from 'react';
import * as ReactDOM from 'react-dom';
import * as ReactModal from 'react-modal';

import {ErrorHandler} from 'app/components/error-handler';
import {setupCustomValidators} from 'app/services/setup';
import {AppRoutingComponent} from './app/routing/app-routing';
import {environment} from "./environments/environment";
import {LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN} from "./app/utils/cookies";

setupCustomValidators();
ReactModal.defaultStyles.overlay = {};
ReactModal.defaultStyles.content = {};

console.log("allowTestAccessTokenOverride: " + environment.allowTestAccessTokenOverride)
if (environment.allowTestAccessTokenOverride) {
  window.setTestAccessTokenOverride = (token: string) => {
    // Disclaimer: console.log statements here are unlikely to captured by
    // Puppeteer, since it typically reloads the page immediately after
    // invoking this function.
    debugger;
    if (token) {
      window.localStorage.setItem(LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN, token);
      location.replace('/');
    } else {
      window.localStorage.removeItem(LOCAL_STORAGE_KEY_TEST_ACCESS_TOKEN);
    }
  };
  console.log("setTestAccessTokenOverride: " + window.setTestAccessTokenOverride);
}

const domContainer = document.querySelector('#error-handler-root');
const bodyContainer = document.querySelector('#root');
ReactDOM.render(React.createElement(ErrorHandler), domContainer);
ReactDOM.render(React.createElement(AppRoutingComponent), bodyContainer);
