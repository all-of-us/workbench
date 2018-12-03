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
  reString: RegExp;
  toBeHighlightedWords: string[];
  ngOnChanges() {
    if (this.searchTerm) {
      let searchWords = this.searchTerm.split(new RegExp(',| '));
      searchWords = searchWords.filter(w => w.length > 0 );
      searchWords = searchWords.map(word => word.replace(/[&!^\/\\#,+()$~%.'":*?<>{}]/g, ''));
      this.reString = new RegExp(searchWords.join('|'));
    }
    this.words = this.text.split(' ');
  }
  public highlight(word) {
    const splitWords = word.split(' ');
    const temp = [];
    for (let i = 0; i < splitWords.length; i++) {
      const m = splitWords[i].match(this.reString);
      if (m) {
        const splitMatchedWord = splitWords[i].split(m[0]);
        for (let j = 0; j < splitMatchedWord.length; j++) {
          if (j !== splitMatchedWord.length - 1 ) {
            temp.push(splitMatchedWord[j]);
            temp.push(m[0]);
          } else {
            temp.push(splitMatchedWord[j]);
          }
        }
      } else {
        temp.push(splitWords[i]);
      }
    }
    return temp;
  }
}
