import { Component, Input, OnChanges } from '@angular/core';

@Component({
  selector: 'app-highlight-search',
  templateUrl: './highlight-search.component.html',
  styleUrls: ['./highlight-search.component.css']
})
export class HighlightSearchComponent implements OnChanges {
  @Input() text: string;
  @Input() searchTerm: string;
  @Input() isSource: boolean;
  @Input() conceptId: string;
  @Input() isStandard: string;
  @Input() indNum: number;
  words: string[];
  matchString: RegExp;
  ngOnChanges() {
    if (this.searchTerm) {
      let searchWords = this.searchTerm.split(new RegExp(',| '));
      searchWords = searchWords.filter(w => w.length > 0 );
      searchWords = searchWords.map(word => word.replace(/[&!^\/\\#,+()$~%.'":*?<>{}]/g, ''));
      this.matchString = new RegExp(searchWords.join('|'));
    }
    this.words = this.text.split(' ');
  }
  public highlight(word) {
    const matches = word.match(new RegExp(this.matchString, 'g'));
    const splits = word.split(new RegExp(this.matchString));
    const temp = [];
    if (matches) {
      for (let i = 0; i < matches.length; i++) {
        temp.push(splits[i], matches[i]);
      }
      temp.push(splits[splits.length - 1]);
    } else {
      temp.push(word);
    }
    return temp;
  }
}
