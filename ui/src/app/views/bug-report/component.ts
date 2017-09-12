import { Component, OnInit } from '@angular/core';
import domtoimage from 'dom-to-image';
import {BugsService} from 'generated'
import {BugReport} from 'generated'
// import { saveAs } from 'file-saver';

@Component({
  selector: 'app-bug-report',
  templateUrl: './component.html',
  styleUrls: ['./component.css']
})
export class BugReportComponent implements OnInit {
  reporting = false;
  shortDescription: string;
  reproSteps: string;
  bugReport: BugReport = {shortDescription: "", reproSteps: ""};
  constructor(
    private bugsService: BugsService
  ) {}

  ngOnInit() {
  }

  reportBug() {
    this.reporting = true;
    this.shortDescription='';
    this.reproSteps='';
  }

  send() {
    this.reporting = false;
    // let root = document.getElementById('body');
    // console.log(root);
    // console.log(domtoimage)
    // console.log(domtoimage.toPng(root))
    // domtoimage.toSvg(document.getElementById('body'), {quality: 0.95})
    //   .then(function (dataUrl : any) {
    //     var link = document.createElement('a');
    //     link.download = 'my-image-name.svg';
    //     link.href = dataUrl;
    //     link.click();
    //   }).catch(function(error:any){
    //     console.error("error", error);
    //   })
    this.bugReport.shortDescription = this.shortDescription;
    this.bugReport.reproSteps = this.reproSteps;
    // TODO: Handle error reporting error.
    this.bugsService.sendBug(this.bugReport).subscribe((bugReport: BugReport)=>{});

    console.log(this.shortDescription);
    console.log(this.reproSteps);

  }
}
