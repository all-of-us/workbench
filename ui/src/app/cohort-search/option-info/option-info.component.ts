import {AfterViewInit, Component, Input, OnInit, ViewChild} from '@angular/core';
import {TreeType} from 'generated';
import {highlightMatches, stripHtml} from '../utils';

@Component({
  selector: 'app-option-info',
  templateUrl: './option-info.component.html',
  styleUrls: ['./option-info.component.css']
})
export class OptionInfoComponent implements AfterViewInit, OnInit {

  @Input() option: any;
  @Input() searchTerm: string;
  @Input() highlighted: boolean;

  @ViewChild('button') button;
  isTruncated = false;

  constructor() { }

  ngOnInit() {
    const displayName = (this.showCode ? this.option.code + ' ' : '') + this.option.name;
    this.option.displayName =
      highlightMatches([this.searchTerm], displayName, this.option.id.toString());
  }

  ngAfterViewInit() {
    setTimeout(() => {
      this.checkTruncation();
    });
  }

  checkTruncation() {
    const elem = this.button.nativeElement;
    const id = 'match' + this.option.id.toString();
    const highlight = document.getElementById(id);
    this.isTruncated = elem.offsetWidth < elem.scrollWidth;
    if (this.isTruncated && highlight) {
      this.checkPosition(elem, highlight);
    } else if (highlight !== null) {
      highlight.style.background =
        'linear-gradient(to right, rgba(101,159,61,0.2) 0, rgba(101,159,61,0.2) 100%)';
    }
  }

  checkPosition(elem: any, highlight: any) {
    const eCoords = elem.getBoundingClientRect();
    const hCoords = highlight.getBoundingClientRect();
    const padding = parseFloat(window.getComputedStyle(elem).getPropertyValue('padding-right'));
    if (hCoords.right > (eCoords.right - padding)) {
      const diff = hCoords.right - (eCoords.right - padding - 15);
      const percentage = (((hCoords.width - diff) / hCoords.width) * 100);
      highlight.style.background =
        'linear-gradient(to right, rgba(101,159,61,0.2) 0, rgba(101,159,61,0.2) '
        + percentage + '%, transparent ' + percentage + '%)';
    } else {
      highlight.style.background =
        'linear-gradient(to right, rgba(101,159,61,0.2) 0, rgba(101,159,61,0.2) 100%)';
    }
  }

  get showCode() {
    return [TreeType.ICD9, TreeType.ICD10, TreeType.CPT, TreeType.MEAS].includes(this.option.type)
      || (TreeType.DRUG === this.option.type && !this.option.group);
  }
}
