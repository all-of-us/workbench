import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-bug-report',
  templateUrl: './component.html',
  styleUrls: ['./component.css']
})
export class BugReportComponent implements OnInit {
  reporting = false;
  shortDescription: string;
  reproSteps: string;
  constructor() { }

  ngOnInit() {
  }

  reportBug() {
    this.reporting = true;
    this.shortDescription='';
    this.reproSteps='';
  }
  send() {
    this.reporting = false;
    console.log(this.shortDescription);
    console.log(this.reproSteps);
  }
}
