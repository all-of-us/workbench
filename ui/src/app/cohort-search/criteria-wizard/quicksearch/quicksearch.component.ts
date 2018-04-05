import {Component, EventEmitter, Input, Output} from '@angular/core';
import {FormControl} from '@angular/forms';

@Component({
  selector: 'crit-quicksearch',
  templateUrl: './quicksearch.component.html',
  styleUrls: ['./quicksearch.component.css'],
})
export class QuickSearchComponent {
  fuzzyFinder = new FormControl();
  isFocused = false;
  @Output() value = new EventEmitter<string>();

  @Input()
  set disabled(val: boolean) {
    if (val) {
      this.fuzzyFinder.disable();
    } else {
      this.fuzzyFinder.enable();
    }
  }

  onInput(event) {
    this.value.emit(event.target.value);
  }
}
