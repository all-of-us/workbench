import {ErrorHandler, Injectable} from '@angular/core';
import {StackdriverErrorReporter} from 'stackdriver-errors-js';

import {ServerConfigService} from 'app/services/server-config.service';
import {ConfigResponse} from 'generated';

@Injectable()
export class ErrorReporterService extends ErrorHandler {
  private stackdriverReporter: StackdriverErrorReporter;

  constructor(serverConfigService: ServerConfigService) {
    super();
    if (window.location.protocol !== 'https:') {
      // The scheme is an indirect proxy for whether this is a local dev
      // deployment. If it is local, we want to disable Stackdriver reporting as
      // it's not useful and likely won't work due to the origin.
      return;
    }
    serverConfigService.getConfig().subscribe((config: ConfigResponse) => {
      if (!config.stackdriverApiKey) {
        return;
      }
      const r = new StackdriverErrorReporter();
      r.start({
        key: config.stackdriverApiKey,
        projectId: config.projectId,
      });
      this.stackdriverReporter = r;
    });
  }

  handleError(error: any) {
    if (!this.stackdriverReporter) {
      super.handleError(error);
      return;
    }
    this.stackdriverReporter.report(error, (e) => {
      // Note: this does not detect non-200 responses from Stackdriver:
      // https://github.com/GoogleCloudPlatform/stackdriver-errors-js/issues/32
      if (e) {
        console.log('failed to send error report: ' + e);
      }
    });
  }
}
