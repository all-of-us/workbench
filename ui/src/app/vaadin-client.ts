// Declare an injection token for vaadin, so it can be injected like other services.
// See https://angular.io/guide/dependency-injection#injectiontoken

import {InjectionToken} from '@angular/core';

export let VAADIN_CLIENT = new InjectionToken('VaadinClient');
