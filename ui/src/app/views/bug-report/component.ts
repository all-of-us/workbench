import { Component, OnInit } from '@angular/core';
import { getDomToImage } from 'app/app.module';
import { saveAs } from 'file-saver';

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
    let root = document.documentElement;
    // toPng(root).then(function (dataUrl) {
    //   let img = new Image();
    //   img.src = dataUrl;
    //   document.body.appendChild(img);
    //   toBlob(img).then(function (blob) {
    //     saveAs(blob, 'error-picture.png');
    //   }).catch(function (error) {
    //     console.log("Error converting image to blob");
    //   });
    // })
    // .catch(function (error) {
    //   console.error('Could not screenshot page!', error);
    // });
    console.log(this.shortDescription);
    console.log(this.reproSteps);
  }
}
