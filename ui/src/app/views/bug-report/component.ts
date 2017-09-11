import { Component, OnInit } from '@angular/core';
import domtoimage from 'dom-to-image';
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
  constructor() {}

  ngOnInit() {
  }

  reportBug() {
    this.reporting = true;
    this.shortDescription='';
    this.reproSteps='';
  }

  send() {
    this.reporting = false;
    let root = document.getElementById('body');
    console.log(root);
    console.log(domtoimage)
    console.log(domtoimage.toPng(root))
    domtoimage.toSvg(document.getElementById('body'), {quality: 0.95})
      .then(function (dataUrl : any) {
        var link = document.createElement('a');
        link.download = 'my-image-name.svg';
        link.href = dataUrl;
        link.click();
      }).catch(function(error:any){
        console.error("error", error);
      })

    // domtoimage.toPng(root).then(function (dataUrl : any) {
    //   let img = new Image();
    //   img.src = dataUrl;
    //   document.body.appendChild(img);
    //   console.log(img);
    //   domtoimage.toBlob(img).then(function (blob : any) {
    //     console.log(blob);
    //   }).catch(function (error : any) {
    //     console.error("Error converting image to blob", error);
    //   });
    // })
    // .catch(function (error : any) {
    //   console.error('Could not screenshot page!', error);
    // });
    console.log(this.shortDescription);
    console.log(this.reproSteps);
  }
}
