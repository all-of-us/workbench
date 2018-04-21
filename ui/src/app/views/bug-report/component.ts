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
  shortDescription: string;
  reproSteps: string;
  contactEmail: string;
  bugReport: BugReport = {shortDescription: '', reproSteps: '', contactEmail: ''};


  constructor(
    private bugReportService: BugReportService,
    public profileStorageService: ProfileStorageService
  ) {}

  ngOnInit() {
  }

  reportBug() {
    this.reporting = true;
    this.shortDescription = '';
    this.reproSteps = '';
    this.profileStorageService.profile$.subscribe((profile) => {
      this.contactEmail = profile.contactEmail;
    });
  }

  send() {
    this.reporting = false;
    this.bugReport.shortDescription = this.shortDescription;
    this.bugReport.reproSteps = this.reproSteps;
    this.bugReport.contactEmail = this.contactEmail;
    this.bugReportService.sendBugReport(this.bugReport).subscribe((bugReport: BugReport) => {});
  }
}
