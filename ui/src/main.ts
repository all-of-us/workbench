// The main file is boilerplate; AppModule configures our app.

import {enableProdMode} from '@angular/core';
import {platformBrowserDynamic} from '@angular/platform-browser-dynamic';
import {environment} from 'environments/environment';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
import * as ReactModal from 'react-modal';

import {AppModule} from 'app/app.module';
import {ErrorHandler} from 'app/components/error-handler';
import {setupCustomValidators} from 'app/services/setup';


if (!environment.debug) {
  enableProdMode();
}

setupCustomValidators();
ReactModal.defaultStyles.overlay = {};
ReactModal.defaultStyles.content = {};

const domContainer = document.querySelector('#error-handler-root');
ReactDOM.render(React.createElement(ErrorHandler), domContainer);

platformBrowserDynamic().bootstrapModule(AppModule);
