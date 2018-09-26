import {Component, EventEmitter, Input, Output} from '@angular/core';
import {ActivatedRoute} from '@angular/router';

import {
  ConceptSet,
  ConceptSetsService
} from 'generated';

@Component({
  selector: 'app-concept-add-modal',
  styleUrls: ['./component.css',
    '../../styles/buttons.css',
    '../../styles/inputs.css'],
  templateUrl: './component.html',
})
export class ConceptAddModalComponent {
  public modalOpen = false;

  loading = false;

  conceptSets: ConceptSet[] = [];

  wsNamespace: string;
  wsId: string;

  constructor(
    private conceptSetsService: ConceptSetsService,
    private route: ActivatedRoute) {
    this.wsNamespace = this.route.snapshot.params['ns'];
    this.wsId = this.route.snapshot.params['wsid'];
  }

  open(): void {
    this.conceptSetsService.getConceptSetsInWorkspace(this.wsNamespace, this.wsId)
      .subscribe((response) => {
        this.conceptSets = response.items;
    });
    this.modalOpen = true;
    this.loading = false;
  }

  close(): void {
    this.modalOpen = false;
  }

  createConceptSet(): void {
    // TODO: Implement
  }


}
