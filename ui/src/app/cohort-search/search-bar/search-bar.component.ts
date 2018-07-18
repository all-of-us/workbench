import {Component, OnInit} from '@angular/core';
import {CohortSearchActions} from '../redux';

@Component({
  selector: 'app-search-bar',
  templateUrl: './search-bar.component.html',
  styleUrls: ['./search-bar.component.css']
})
export class SearchBarComponent implements OnInit {
  searchTerm = '';
  constructor(private actions: CohortSearchActions) { }

  ngOnInit() {
    // console.log(this.tree);
  }

  inputChange(newVal: string) {
    this.actions.setCriteriaSearchTerms(newVal);
  }
}
