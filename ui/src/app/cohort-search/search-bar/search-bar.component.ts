import {Component} from '@angular/core';
import {CohortSearchActions} from '../redux';

@Component({
  selector: 'app-search-bar',
  templateUrl: './search-bar.component.html',
  styleUrls: ['./search-bar.component.css']
})
export class SearchBarComponent {
  searchTerm = '';
  constructor(private actions: CohortSearchActions) { }

  inputChange(newVal: string) {
    this.actions.setCriteriaSearchTerms(newVal);
  }
}
