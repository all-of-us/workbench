import { Component, OnInit } from '@angular/core';
import {BugsService} from 'generated';
import {BugReport} from 'generated';

@Component({
  selector: 'app-bug-report',
  templateUrl: './component.html',
  styleUrls: ['./component.css']
})
export class BugReportComponent implements OnInit {
  reporting = false;
  shortDescription: string;
  reproSteps: string;
  bugReport: BugReport = {shortDescription: '', reproSteps: ''};
  constructor(
    private bugsService: BugsService
  ) {}

  ngOnInit() {
  }

  reportBug() {
    this.reporting = true;
    this.shortDescription = '';
    this.reproSteps = '';
  }

  send() {
    this.reporting = false;
    this.bugReport.shortDescription = this.shortDescription;
    this.bugReport.reproSteps = this.reproSteps;
    this.bugsService.sendBug(this.bugReport).subscribe((bugReport: BugReport) => {});
  }
}
