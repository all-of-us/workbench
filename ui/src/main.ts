// The main file is boilerplate; AppModule configures our app.

import {platformBrowserDynamic} from '@angular/platform-browser-dynamic';
import ReactModal from 'react-modal';

import {AppModule} from 'app/app.module';

ReactModal.defaultStyles = { overlay: {}, content: {} };
platformBrowserDynamic().bootstrapModule(AppModule);
