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

  isTruncated = false;

  @ViewChild('match') match;
  checkMatch() {
    console.log(this.match.nativeElement);
  }

  @ViewChild('button') button;

  constructor() { }

  ngOnInit() {

  }

  ngAfterViewInit() {
    setTimeout(() => this.checkTruncation(), 1000);
  }

  checkTruncation() {
    const elem = this.button.nativeElement;
    const highlight = document.getElementById('match');
    if (highlight) {
      console.log(this.option.name);
      console.log(elem);
      console.log(highlight);
    }
    this.isTruncated = elem.offsetWidth < elem.scrollWidth;
  }

  get popperName() {
    return stripHtml(this.option.displayName);
  }

}
