import {Component, EventEmitter, Output} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {ConceptSet, ConceptsService, CreateConceptSetRequest, DomainInfo} from 'generated';
import {ConceptSetsService} from 'generated/api/conceptSets.service';
import {Domain} from 'generated/model/domain';

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
export class CreateConceptSetModalComponent {
  @Output() onUpdate: EventEmitter<void> = new EventEmitter();
  public modalOpen  = false;
  wsNamespace: string;
  wsId: string;
  name: string;
  description: string;
  domain: Domain;
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
      this.domain = this.conceptDomainList[0].domain;
    });
    this.modalOpen = true;
  }

  close(): void {
    this.modalOpen = false;
  }

  reset(): void {
    this.name = '';
    this.description = '';
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
    const request: CreateConceptSetRequest = {
      conceptSet: concept
    };
    this.conceptSetService.createConceptSet(this.wsNamespace, this.wsId, request)
      .subscribe(() => {
        this.modalOpen = false;
        this.onUpdate.emit();
      });
  }
}

