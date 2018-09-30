import {Component, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {ConceptSet, ConceptsService, DomainInfo} from '../../../generated';
import {ConceptSetsService} from '../../../generated/api/conceptSets.service';
import {ConceptAddModalComponent} from '../concept-add-modal/component';
@Component({
  selector: 'app-concept-list-modal',
  styleUrls: [
    './component.css',
    '../../styles/buttons.css',
    '../../styles/inputs.css',
  ],
  templateUrl: './component.html',
})
export class CreateConceptModalComponent {
  public modalOpen  = false;
  wsNamespace: string;
  wsId: string;
  name: string;
  description: string;
  domain: any;
  conceptDomainList: Array<DomainInfo> = [];

  constructor(private conceptsService: ConceptsService,
              private conceptSetService: ConceptSetsService,
              private route: ActivatedRoute) {
    this.wsNamespace = this.route.snapshot.params['ns'];
    this.wsId = this.route.snapshot.params['wsid'];
  }

  open(): void {
    this.reset();
    this.conceptsService.getDomainInfo(this.wsNamespace, this.wsId).subscribe((response) => {
      this.conceptDomainList = response.items;
    });
      this.modalOpen = true;
  }

  reset(): void {
    this.name = '';
    this.description = '';
    this.domain = '';
  }

  saveConcept(): void {
    const concepts: ConceptSet = {
      name: this.name,
      description: this.description,
      domain: this.domain
    };
    this.conceptSetService.createConceptSet(this.wsNamespace, this.wsId, concepts)
        .subscribe((response) => {
      this.modalOpen = false;
    });
    this.modalOpen = false;
  }
}

