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
    setTimeout(() => this.checkTruncation());
  }

  checkTruncation() {
    const elem = this.button.nativeElement;
    this.isTruncated = (elem.offsetWidth - 20) < elem.scrollWidth;
    if (this.isTruncated) {
      this.checkPosition(elem);
    }
  }

  checkPosition(elem: any) {
    const id = 'match' + this.option.id.toString();
    const highlight = document.getElementById(id);
    const eCoords = elem.getBoundingClientRect();
    const hCoords = highlight.getBoundingClientRect();
    const padding = parseFloat(window.getComputedStyle(elem).getPropertyValue('padding-left'))
      + parseFloat(window.getComputedStyle(elem).getPropertyValue('padding-right'));
    const diff = (hCoords.left + hCoords.width) - eCoords.left;
    if (diff > (eCoords.width - padding))  {
      highlight.style.background = 'none';
    }
  }

  get popperName() {
    return stripHtml(this.option.displayName);
  }

}
