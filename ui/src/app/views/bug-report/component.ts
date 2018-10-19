import {Component} from '@angular/core';

import {ProfileStorageService} from 'app/services/profile-storage.service';

import {
  BugReport,
  BugReportService,
  BugReportType
} from 'generated';

@Component({
  selector: 'app-bug-report',
  templateUrl: './component.html',
  styleUrls: ['./component.css',
              '../../styles/buttons.css',
              '../../styles/errors.css',
              '../../styles/inputs.css']
})
export class BugReportComponent {
  bugReportTypes = Object.keys(BugReportType);

  reporting = false;
  submitting = false;
  bugReport: BugReport = this.emptyReport();
  sendBugReportError = false;

  constructor(
    private bugReportService: BugReportService,
    public profileStorageService: ProfileStorageService
  ) {}

  private emptyReport(): BugReport {
    return {
      bugType: BugReportType.APPLICATION,
      shortDescription: '',
      reproSteps: '',
      includeNotebookLogs: true,
      contactEmail: ''
    };
  }

  reportBug() {
    this.reporting = true;
    this.sendBugReportError = false;
    this.bugReport = this.emptyReport();
    this.profileStorageService.profile$.subscribe((profile) => {
      this.bugReport.contactEmail = profile.contactEmail;
    });
  }

  send() {
    this.submitting = true;
    this.sendBugReportError = false;
    this.bugReportService.sendBugReport(this.bugReport).subscribe(() => {
      this.reporting = false;
      this.submitting = false;
    }, () => {
      this.sendBugReportError = true;
      this.submitting = false;
    });
  }
}
