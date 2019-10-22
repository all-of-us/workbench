import {ErrorHandler, Injectable} from '@angular/core';
import {StackdriverErrorReporter} from 'stackdriver-errors-js';

import {ServerConfigService} from 'app/services/server-config.service';
import {setStackdriverErrorReporter} from 'app/utils/errors';
import {environment} from 'environments/environment';
import {ConfigResponse} from 'generated';

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

  handleError(error: any) {
    // Always log to console regardless of whether Stackdriver is enabled.
    super.handleError(error);
    if (!this.stackdriverReporter) {
      return;
    }


    // We want to avoid sending XHR errors to stackdriver because they should
    // already be being logged by the API with the stacktrace and information
    // we care about.

    // xhrError is set to true by the interceptor. The rejection piece handles
    // return values from promises that error out.
    if (error.rejection && error.rejection.xhrError) {
      return;
    }
    // The check on xhrError explicitly handles error values that are returned from direct
    // api errors, rather than promises.
    if (error.xhrError) {
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
