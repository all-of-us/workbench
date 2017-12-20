import {BugReport} from 'generated';
import {Observable} from 'rxjs/Observable';

export class BugReportServiceStub {
  public bugReport: BugReport;
  constructor() {}

  public sendBugReport(bugReport: BugReport): Observable<BugReport> {

    const observable = new Observable(observer => {
      setTimeout(() => {
        this.bugReport = bugReport;
        observer.next(bugReport);
        observer.complete();
      }, 0);
    });
    return <Observable<BugReport>>observable;
  }
}
