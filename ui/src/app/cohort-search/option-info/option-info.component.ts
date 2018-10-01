import {AfterViewInit, Component, Input, OnInit, ViewChild} from '@angular/core';
import {stripHtml} from '../utils';

@Component({
  selector: 'app-option-info',
  templateUrl: './option-info.component.html',
  styleUrls: ['./option-info.component.css']
})
export class OptionInfoComponent implements AfterViewInit, OnInit {

  @Input() option: any;
  @Input() highlighted: boolean;

  @ViewChild('button') button;
  isTruncated = false;

  constructor() { }

  ngOnInit() {

  }

  ngAfterViewInit() {
    setTimeout(() => this.checkTruncation(), 1);
  }

  checkTruncation() {
    const elem = this.button.nativeElement;
    this.isTruncated = elem.offsetWidth < elem.scrollWidth;
  }

  get popperName() {
    return stripHtml(this.option.displayName);
  }

}
