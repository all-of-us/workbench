import {Component, OnInit} from '@angular/core';
import {NgRedux} from '@angular-redux/store';

import {CohortSearchState} from '../../redux';

@Component({
  selector: 'crit-alerts',
  templateUrl: './alerts.component.html',
})
export class AlertsComponent implements OnInit {

  criteriaErrors = [
    {kind: 'icd9', parentId: 0},
    {kind: 'icd9', parentId: 1},
  ];

  constructor(
    private ngRedux: NgRedux<CohortSearchState>,
  ) { }

  ngOnInit() {
  }

  /* TODO(jms) hook all this up to actually listen for errors */
  get hasErrors() {
    return this.criteriaErrors.length > 0;
  }

  closeAlert(error) {
    this.criteriaErrors = this.criteriaErrors.filter(err => err !== error);
  }

}
