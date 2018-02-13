import {Component, Input} from '@angular/core';
import {Subject} from 'rxjs/Subject';

import {Participant} from '../participant.model';

@Component({
  selector: 'app-choice-filter',
  templateUrl: './choice-filter.component.html',
  styleUrls: ['./choice-filter.component.css']
})
export class ChoiceFilterComponent {
  @Input() options: any;

  changes = new Subject<any>();

  isActive(): boolean {
    return true;
  }

  accepts(person: Participant): boolean {
    return true;
  }
}
