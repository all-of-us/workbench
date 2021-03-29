import {ErrorHandler, Injectable} from '@angular/core';
import {StackdriverErrorReporter} from 'stackdriver-errors-js';

import {ServerConfigService} from 'app/services/server-config.service';
import {setStackdriverErrorReporter} from 'app/utils/errors';
import {environment} from 'environments/environment';
import {ConfigResponse} from 'generated/fetch';

@Injectable()
export class ErrorReporterService extends ErrorHandler {
  private stackdriverReporter: StackdriverErrorReporter;

  constructor(serverConfigService: ServerConfigService) {
    super();
    if (environment.debug) {
      // This is a local dev server, we want to disable Stackdriver reporting as
      // it's not useful and likely won't work due to the origin.
      return;
    }
    serverConfigService.getConfig().subscribe((config: ConfigResponse) => {
      if (!config.publicApiKeyForErrorReports) {
        return;
      }
      const r = new StackdriverErrorReporter();
      r.start({
        key: config.publicApiKeyForErrorReports,
        projectId: config.projectId,
      });
      this.stackdriverReporter = r;
      setStackdriverErrorReporter(r);
    });
  }

  /**
   * Unwrap to avoid vague top-level errors like: "Promise rejected: {}". This method
   * mutates the incoming error in order to allow preservation of the original stack trace.
   */
  private async expandErrorMessage(error: any): Promise<any> {
    if (!error.rejection || !(error.rejection instanceof Response)) {
      return;
    }

    const httpResp = error.rejection;
    const prefix = `${httpResp.status} @ ${httpResp.url}`;
    if (httpResp.bodyUsed) {
      error.message = prefix;
      return;
    }
    const json = JSON.stringify(await httpResp.json());
    error.message = `${prefix}: ${json}`;
    return;
  }

  async handleError(error: any) {
    await this.expandErrorMessage(error);

    // Always log to console regardless of whether Stackdriver is enabled.
    super.handleError(error);

    if (!this.stackdriverReporter) {
      return;
    }

    this.stackdriverReporter.report(error, (e) => {
      // Note: this does not detect non-200 responses from Stackdriver:
      // https://github.com/GoogleCloudPlatform/stackdriver-errors-js/issues/32
      if (e) {
        console.error('failed to send error report: ', e);
      }
    });
  }
}
