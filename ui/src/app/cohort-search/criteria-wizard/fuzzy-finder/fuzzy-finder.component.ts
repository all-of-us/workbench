import {Component, Input} from '@angular/core';
import {FormControl} from '@angular/forms';
import {transition, trigger, style, state, animate} from '@angular/animations';

@Component({
  selector: 'crit-fuzzy-finder',
  templateUrl: './fuzzy-finder.component.html',
  styleUrls: ['./fuzzy-finder.component.css'],
  animations: [
    trigger('blueBar', [
      state('focused', style({
        backgroundSize: '100% 100%',
        borderBottom: '1px solid #0094d2',
      })),
      state('blurred', style({
        backgroundSize: '0 100%',
        borderBottom: '1px solid #9a9a9a',
      })),
      transition('* => *', animate('0.5s ease-in-out')),
    ]),
  ],
})
export class FuzzyFinderComponent {
  fuzzyFinder = new FormControl();
  @Input() criteriaType: string;

  private isFocused = false;
  private searchString = '';

  get blueBarState() {
    return this.isFocused ? 'focused' : 'blurred';
  }

  get isInfoClass() {
    return this.isFocused ? 'is-info' : '';
  }

  onInput(event) {
    this.searchString = event.target.value;
  }
}
