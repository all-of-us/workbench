// The main file is boilerplate; AppModule configures our app.

import {platformBrowserDynamic} from '@angular/platform-browser-dynamic';
import * as ReactModal from 'react-modal';

import {AppModule} from 'app/app.module';

ReactModal.defaultStyles.overlay = {};
ReactModal.defaultStyles.content = {};
platformBrowserDynamic().bootstrapModule(AppModule);
