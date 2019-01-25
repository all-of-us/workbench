// The main file is boilerplate; AppModule configures our app.

import {enableProdMode} from '@angular/core';
import {platformBrowserDynamic} from '@angular/platform-browser-dynamic';
import {environment} from 'environments/environment';
import * as ReactModal from 'react-modal';

import {AppModule} from 'app/app.module';


if (!environment.debug) {
  enableProdMode();
}

ReactModal.defaultStyles.overlay = {};
ReactModal.defaultStyles.content = {};
platformBrowserDynamic().bootstrapModule(AppModule);
