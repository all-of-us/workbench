// The main file is boilerplate; AppModule configures our app.

import * as React from 'react';
import * as ReactDOM from 'react-dom';
import * as ReactModal from 'react-modal';
import * as StackTrace from 'stacktrace-js';

import { ErrorHandler } from 'app/components/error-handler';
import {
  checkBrowserSupport,
  exposeAccessTokenSetter,
  setupCustomValidators,
} from 'app/services/setup';
import { AppRoutingComponent } from './app/routing/app-routing';

import 'app/styles/genome-extraction-datatable.css';
import 'app/styles/sidebar.css';
import '@clr/icons/clr-icons.min.css';
import '@clr/icons/clr-icons.min.js';
import '@clr/ui/clr-ui.min.css';
import '@webcomponents/custom-elements/custom-elements.min.js';
import 'mutationobserver-shim/dist/mutationobserver.min.js';
import 'nouislider/distribute/nouislider.min.css';
import 'outdated-browser-rework/dist/style.css';
import 'primeicons/primeicons.css';
import 'primereact/resources/primereact.min.css';
import 'primereact/resources/themes/nova-light/theme.css';
import 'react-calendar/dist/Calendar.css';
import 'styles.css';

// Unfortunately stackdriver-errors-js doesn't properly declare dependencies, so
// we need to explicitly load its StackTrace dep:
// https://github.com/GoogleCloudPlatform/stackdriver-errors-js/issues/2
(<any>window).StackTrace = StackTrace;

setupCustomValidators();
exposeAccessTokenSetter();
checkBrowserSupport();
ReactModal.defaultStyles.overlay = {};
ReactModal.defaultStyles.content = {};

const domContainer = document.querySelector('#error-handler-root');
const bodyContainer = document.querySelector('#root');
ReactDOM.render(React.createElement(ErrorHandler), domContainer);
ReactDOM.render(React.createElement(AppRoutingComponent), bodyContainer);
