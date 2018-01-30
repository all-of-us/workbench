import {ErrorHandler, Injectable} from '@angular/core';
import {StackdriverErrorReporter} from 'stackdriver-errors-js';

import {ServerConfigService} from 'app/services/server-config.service';
import {ConfigResponse} from 'generated';

@Injectable()
export class ErrorReporterService extends ErrorHandler {
  private stackdriverReporter: StackdriverErrorReporter;

  constructor(serverConfigService: ServerConfigService) {
    super();
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
      if (e) {
        console.log('failed to send error report: ' + e);
      }
    });
  }
}
