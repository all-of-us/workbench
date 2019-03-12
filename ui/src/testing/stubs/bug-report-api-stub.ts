import {BugReport, BugReportApi, BugReportType} from 'generated/fetch';

export const EMPTY_BUG_REPORT = {
  bugType: BugReportType.APPLICATION,
  shortDescription: '',
  reproSteps: '',
  includeNotebookLogs: true,
  contactEmail: ''
};

export class BugReportApiStub extends BugReportApi {
  public bugReport: BugReport;
  constructor() {
    super(undefined, undefined, (..._: any[]) => {
      throw Error('cannot fetch in tests');
    });
  }

  public sendBugReport(bugReport: BugReport): Promise<BugReport> {
    return new Promise<BugReport>(resolve => {
      resolve(bugReport);
    });
  }
}
