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
    this.isTruncated = (elem.offsetWidth - 15) < elem.scrollWidth;
    const id = 'match' + this.option.id.toString();
    const highlight = document.getElementById(id);
    if (this.isTruncated && highlight) {
      this.checkPosition(elem, highlight);
    }
  }

  checkPosition(elem: any, highlight: any) {
    const eCoords = elem.getBoundingClientRect();
    const hCoords = highlight.getBoundingClientRect();
    const padding = parseFloat(window.getComputedStyle(elem).getPropertyValue('padding-right'));
    if (hCoords.right > (eCoords.right - padding)) {
      const diff = hCoords.right - (eCoords.right - padding - 15);
      if (diff > hCoords.width) {
        highlight.style.background = 'none';
      } else {
        console.log(this.option.name);
        console.log(diff);
        console.log(hCoords);
        // highlight.style.backgroundSize = (hCoords.width - diff).toString() + 'px';
        const percentage = (((hCoords.width - diff) / hCoords.width) * 100).toString();
        highlight.style.background =
          'linear-gradient(to right, rgba(101,159,61,0.2) 0, rgba(101,159,61,0.2) '
          + percentage + '%, transparent ' + percentage + '%)';
      }
    }
  }

  get popperName() {
    return stripHtml(this.option.displayName);
  }

}
