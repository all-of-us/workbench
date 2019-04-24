import {AfterViewInit, Component, Input, OnInit, ViewChild} from '@angular/core';
import {highlightMatches} from 'app/cohort-search/utils';
import {TreeSubType, TreeType} from 'generated';

@Component({
  selector: 'app-list-option-info',
  templateUrl: './list-option-info.component.html',
  styleUrls: ['./list-option-info.component.css']
})
export class ListOptionInfoComponent implements AfterViewInit, OnInit {

  @Input() option: any;
  @Input() searchTerm: string;
  @Input() highlighted: boolean;

  @ViewChild('button') button;
  isTruncated = false;

  constructor() { }

  ngOnInit() {
    const code = this.showCode
      ? '<span style="font-weight: 700;">'
      + highlightMatches([this.searchTerm], this.option.code, true)
      + '</span> ' : '';
    this.option.displayName = code +
      highlightMatches([this.searchTerm], this.option.name, true, this.option.id.toString());
  }

  ngAfterViewInit() {
    setTimeout(() => {
      this.checkTruncation();
    });
  }

  checkTruncation() {
    const elem = this.button.nativeElement;
    const _class = 'match' + this.option.id.toString();
    const highlights = document.getElementsByClassName(_class);
    this.isTruncated = elem.offsetWidth < elem.scrollWidth;
    if (this.isTruncated && highlights) {
      Array.from(highlights).forEach(highlight => {
        this.checkPosition(elem, highlight);
      });
    } else if (highlights !== null) {
      Array.from(highlights).forEach(highlight => {
        highlight.setAttribute(
          'style',
          'background: linear-gradient(to right, rgba(101,159,61,0.2) 0, rgba(101,159,61,0.2) 100%)'
        );
      });
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
      || (TreeSubType.ATC === this.option.subtype && !this.option.group);
  }
}
