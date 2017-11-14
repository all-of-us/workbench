import {Component, Input} from '@angular/core';
import {FormControl} from '@angular/forms';

@Component({
  selector: 'crit-quicksearch',
  templateUrl: './quicksearch.component.html',
  styleUrls: ['./quicksearch.component.css'],
})
export class QuickSearchComponent {
  fuzzyFinder = new FormControl();
  @Input() criteriaType: string;

  private isFocused = false;
  private searchString = '';

  onInput(event) {
    this.searchString = event.target.value;
  }
}
