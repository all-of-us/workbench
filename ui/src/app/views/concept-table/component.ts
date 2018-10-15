import {Component, EventEmitter, Input, Output} from '@angular/core';

import {Domain} from 'generated';

@Component({
  selector: 'app-concept-table',
  styleUrls: ['./component.css',
    '../../styles/buttons.css'],
  templateUrl: './component.html',
})
export class ConceptTableComponent {
  @Input() concepts: Object[];
  @Output() getSelectedConcepts = new EventEmitter<any>();
  @Input() loading = false;
  @Input() searchTerm = '';
  @Input() placeholderValue = '';


  selectedConcepts: Array<any> = [];

  onSelectedChanged($event) {
    this.getSelectedConcepts.emit($event);
  }
}
