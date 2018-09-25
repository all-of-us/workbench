import {Component, Input} from '@angular/core';

@Component({
  selector: 'app-concept-table',
  styleUrls: ['./component.css',
    '../../styles/buttons.css'],
  templateUrl: './component.html',
})
export class ConceptTableComponent {
  @Input() concepts: Object[];

  loading = false;

  selectedConcepts: Array<any> = [];
}
