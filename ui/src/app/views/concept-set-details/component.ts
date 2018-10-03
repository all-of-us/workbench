import {Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {BehaviorSubject} from 'rxjs/BehaviorSubject';

import {ConceptAddModalComponent} from 'app/views/concept-add-modal/component';
import {ConceptTableComponent} from 'app/views/concept-table/component';


import {
  ConceptSet,
  ConceptSetsService,
  Domain,
  DomainInfo,
  StandardConceptFilter,
} from 'generated';

@Component({
  styleUrls: ['../../styles/buttons.css',
    '../../styles/cards.css',
    '../../styles/headers.css',
    '../../styles/inputs.css',
    '../../styles/errors.css',
    './component.css'],
  templateUrl: './component.html',
})
export class ConceptSetDetailsComponent {
  wsNamespace: string;
  wsId: string;
  conceptSet: ConceptSet;

  editHover = false;

  constructor(
    private conceptSetsService: ConceptSetsService,
    private route: ActivatedRoute,
  ) {
    this.wsNamespace = this.route.snapshot.params['ns'];
    this.wsId = this.route.snapshot.params['wsid'];
    this.conceptSet = this.route.snapshot.data.conceptSet;
  }

  openRemoveModal() {
    // TODO(calbach): Implement.
  }
}
