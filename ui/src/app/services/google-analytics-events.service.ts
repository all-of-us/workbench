import {Injectable} from '@angular/core';
declare let gtag: Function;

@Injectable()
export class GoogleAnalyticsEventsService {

  public emitEvent(eventName: string,
                   eventCategory: string,
                   eventAction: string,
                   eventLabel: string = null,
                   eventValue: number = null) {
    gtag('event', eventName, { eventCategory, eventLabel, eventAction, eventValue });
  }
}
