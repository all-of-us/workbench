import {AfterViewInit, Component, Input, OnInit, ViewChild} from '@angular/core';
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
    this.option.displayName =
      highlightMatches([this.searchTerm], this.option.name, this.option.id.toString());
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
    this.isTruncated = (elem.offsetWidth - 15) < elem.scrollWidth;
    if (this.isTruncated && highlight) {
      this.checkPosition(elem, highlight);
    } else if (highlight !== null) {
      console.log(highlight);
      highlight.style.background =
        'linear-gradient(to right, rgba(101,159,61,0.2) 0, rgba(101,159,61,0.2) 100%)';
    } else {
    }
  }

  checkPosition(elem: any, highlight: any) {
    const eCoords = elem.getBoundingClientRect();
    const hCoords = highlight.getBoundingClientRect();
    const padding = parseFloat(window.getComputedStyle(elem).getPropertyValue('padding-right'));
    if (hCoords.right > (eCoords.right - padding)) {
      const diff = hCoords.right - (eCoords.right - padding - 15);
      if (diff > hCoords.width) {
        console.log(this.option.name);
        highlight.style.background = 'none';
      } else {
        const percentage = (((hCoords.width - diff) / hCoords.width) * 100);
        // this.option.displayName =
        //   highlightMatches(
        //     [this.searchTerm], this.option.name, this.option.id.toString(), percentage
        //   );
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
