import {Component, Input} from '@angular/core';
import {FormControl} from '@angular/forms';

@Component({
  selector: 'crit-fuzzy-finder',
  templateUrl: './fuzzy-finder.component.html',
  styleUrls: ['./fuzzy-finder.component.css'],
})
export class FuzzyFinderComponent {
  fuzzyFinder = new FormControl();
  @Input() criteriaType: string;

  private isFocused = false;
  private searchString = '';

  onInput(event) {
    this.searchString = event.target.value;
  }
}
