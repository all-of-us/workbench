// The main file is boilerplate; AppModule configures our app.

import * as React from 'react';
import * as ReactDOM from 'react-dom';
import * as ReactModal from 'react-modal';
import * as StackTrace from 'stacktrace-js';

import {ErrorHandler} from 'app/components/error-handler';
import {setupCustomValidators} from 'app/services/setup';
import {AppRoutingComponent} from './app/routing/app-routing';

import "../node_modules/@clr/icons/clr-icons.min.css";
import "../node_modules/@clr/ui/clr-ui.min.css";
import "../node_modules/nouislider/distribute/nouislider.min.css";
import "../node_modules/primereact/resources/primereact.min.css";
import "../node_modules/primeicons/primeicons.css";
import "../node_modules/primereact/resources/themes/nova-light/theme.css";
import "../node_modules/react-calendar/dist/Calendar.css";
import "../node_modules/outdated-browser-rework/dist/style.css";
import "styles.css";
import "app/styles/sidebar.css";
import "app/styles/genome-extraction-datatable.css";

import "../node_modules/mutationobserver-shim/dist/mutationobserver.min.js";
import "../node_modules/@webcomponents/custom-elements/custom-elements.min.js";
import "../node_modules/@clr/icons/clr-icons.min.js";

// Unfortunately stackdriver-errors-js doesn't properly declare dependencies, so
// we need to explicitly load its StackTrace dep:
// https://github.com/GoogleCloudPlatform/stackdriver-errors-js/issues/2
(<any>window).StackTrace = StackTrace;

setupCustomValidators();
ReactModal.defaultStyles.overlay = {};
ReactModal.defaultStyles.content = {};

const domContainer = document.querySelector('#error-handler-root');
const bodyContainer = document.querySelector('#root');
ReactDOM.render(React.createElement(ErrorHandler), domContainer);
ReactDOM.render(React.createElement(AppRoutingComponent), bodyContainer);
