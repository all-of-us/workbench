import { Component, OnInit, Input, OnChanges } from '@angular/core';

@Component({
  selector: 'app-html-wrapper',
  template: '<span [innerHTML]="markedData"></span>',
  styleUrls: ['./html-wrapper.component.css']
})
export class HtmlWrapperComponent implements OnInit, OnChanges {
  @Input() markedData: string;
  @Input() isSource: boolean;
  @Input() isStandard: boolean;
  @Input() conceptId: boolean;
  @Input() indNum: number;
  constructor() { }

  ngOnInit() {
  }
  ngOnChanges() {
    if (this.isSource) {
      this.markedData = ' <b>Source</b> ' + this.markedData;
    } else if (this.isStandard) {
      this.markedData = ' <b>Standard</b> ' + this.markedData;
    }
    if (this.indNum) {
      this.markedData = this.indNum + '. ' + this.markedData;
    }
    if (this.conceptId) {
      this.markedData = this.markedData + ' -- ' + this.conceptId;
    }
  }

}
