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
  words: string[] = [];
  matchString: RegExp;
  ngOnChanges() {
    if (this.searchTerm) {
      let searchWords = this.searchTerm.split(new RegExp(',| '));
      searchWords = searchWords.filter(w => w.length > 0 );
      searchWords = searchWords.map(word => word.replace(/[&!^\/\\#,+()$~%.'":*?<>{}]/g, ''));
      this.matchString = new RegExp(searchWords.join('|'));
    }
    const matches = this.text.match(new RegExp(this.matchString, 'gi'));
    const splits = this.text.split(new RegExp(this.matchString, 'i'));
    if (matches && this.searchTerm) {
      for (let i = 0; i < matches.length; i++) {
        // did not want to highlight whole of disorder of (, when searched by disorder
        const tempSplitwords = splits[i].split(' ');
        for (let j = 0; j < tempSplitwords.length; j++) {
          this.words.push(tempSplitwords[j]);
        }
        const tempMatchWords = matches[i].split(' ');
        for (let j = 0; j < tempMatchWords.length; j++) {
          this.words.push(tempMatchWords[j]);
        }
      }
      const tempSplitWords = splits[splits.length - 1].split(' ');
      for (let j = 0; j < tempSplitWords.length; j++) {
        this.words.push(tempSplitWords[j]);
      }
    } else {
      this.words.push(this.text);
    }
  }
}
