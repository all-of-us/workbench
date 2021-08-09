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

const domContainer = document.querySelector('#error-handler-root');
const bodyContainer = document.querySelector('#root');
ReactDOM.render(React.createElement(ErrorHandler), domContainer);
ReactDOM.render(React.createElement(AppRoutingComponent), bodyContainer);
