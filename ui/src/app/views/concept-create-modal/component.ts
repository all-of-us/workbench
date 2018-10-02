import {Component, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {ConceptSet, ConceptsService, DomainInfo} from '../../../generated';
import {ConceptSetsService} from '../../../generated/api/conceptSets.service';
import {ConceptAddModalComponent} from '../concept-add-modal/component';
@Component({
  selector: 'app-create-concept-modal',
  styleUrls: [
    '../../styles/buttons.css',
    '../../styles/inputs.css',
    '../../styles/errors.css',
    './component.css'
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
  required = false;
  alreadyExist = false;

  constructor(private conceptsService: ConceptsService,
              private conceptSetService: ConceptSetsService,
              private route: ActivatedRoute) {
    this.wsNamespace = this.route.snapshot.params['ns'];
    this.wsId = this.route.snapshot.params['wsid'];
  }

  open(): void {
    this.required = false;
    this.alreadyExist = false;
    this.reset();
    this.conceptsService.getDomainInfo(this.wsNamespace, this.wsId).subscribe((response) => {
      this.conceptDomainList = response.items;
      this.domain = this.conceptDomainList[0];
    });
      this.modalOpen = true;
  }

  close(): void {
    this.modalOpen = false;
  }

  reset(): void {
    this.name = '';
    this.description = '';
    this.domain = '';
  }

  saveConcept(): void {
    this.required = false;
    this.alreadyExist = false;

    if (!this.name) {
      this.required = true;
      return;
    }
    const concept: ConceptSet = {
      name: this.name,
      description: this.description,
      domain: this.domain
    };
    console.log(concept);
    console.log(this.wsNamespace + " " + this.wsId);
    this.conceptSetService.createConceptSet(this.wsNamespace, this.wsId, concept)
        .subscribe(() => {
      console.log("in create");
      this.modalOpen = false;
    });
  }
}

