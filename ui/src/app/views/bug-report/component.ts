import {Component, OnInit} from '@angular/core';

import {ProfileStorageService} from 'app/services/profile-storage.service';

import {BugReportService} from 'generated';
import {BugReport} from 'generated';

@Component({
  selector: 'app-bug-report',
  templateUrl: './component.html',
  styleUrls: ['./component.css',
              '../../styles/buttons.css']
})
export class BugReportComponent implements OnInit {
  reporting = false;
  bugReport: BugReport = this.emptyReport();
  sendBugReportError: boolean;

  constructor(
    private bugReportService: BugReportService,
    public profileStorageService: ProfileStorageService
  ) {}

  private emptyReport(): BugReport {
    return {
      shortDescription: '',
      reproSteps: '',
      includeNotebookLogs: true,
      contactEmail: ''
    };
  }

  ngOnInit() {
    this.sendBugReportError = false;
  }

  reportBug() {
    this.reporting = true;
    this.bugReport = this.emptyReport();
    this.profileStorageService.profile$.subscribe((profile) => {
      this.bugReport.contactEmail = profile.contactEmail;
    });
  }

  send() {
    this.reporting = false;
    this.bugReportService.sendBugReport(this.bugReport).subscribe(() => {}, () => {
      this.sendBugReportError = true;
    });
  }
}
