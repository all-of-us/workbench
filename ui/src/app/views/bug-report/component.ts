import {Component, OnInit} from '@angular/core';

import {ErrorHandlingService} from 'app/services/error-handling.service';

import {BugReportService} from 'generated';
import {BugReport} from 'generated';
import {ProfileService} from 'generated';

@Component({
  selector: 'app-bug-report',
  templateUrl: './component.html',
  styleUrls: ['./component.css']
})
export class BugReportComponent implements OnInit {
  reporting = false;
  shortDescription: string;
  reproSteps: string;
  contactEmail: string;
  bugReport: BugReport = {shortDescription: '', reproSteps: '', contactEmail: ''};


  constructor(
    private bugReportService: BugReportService,
    private profileService: ProfileService
  ) {}

  ngOnInit() {
  }

  reportBug() {
    this.reporting = true;
    this.shortDescription = '';
    this.reproSteps = '';
    this.profileService.getMe().subscribe(profile => {
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
