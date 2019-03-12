import {Component, EventEmitter, Input, Output} from '@angular/core';

import {urlParamsStore} from 'app/utils/navigation';
import {
  Concept,
  ConceptSet,
  ConceptSetsService,
  CreateConceptSetRequest,
  Domain,
  UpdateConceptSetRequest
} from 'generated';

@Component({
  selector: 'app-concept-add-modal',
  styleUrls: [
    '../../styles/buttons.css',
    '../../styles/inputs.css',
    '../../styles/errors.css',
    './component.css'],
  templateUrl: './component.html',
})
export class ConceptAddModalComponent {
  public modalOpen = false;
  loading = true;
  saving = false;
  conceptSets: ConceptSet[] = [];
  wsNamespace: string;
  wsId: string;
  name: string;
  description: string;
  selectedConceptSet: ConceptSet;
  selectConceptList: Concept[] = [];
  selectDomain: Domain;
  existingSetSelected = true;
  errorSaving = false;
  errorNameReq = false;
  errorMsg: string;

  @Input() selectedDomain: Domain;
  @Input() selectedConcepts: Concept[];
  @Output('saveComplete') saveComplete = new EventEmitter<void>();

  constructor(
    private conceptSetsService: ConceptSetsService,
  ) {
    const {ns, wsid} = urlParamsStore.getValue();
    this.wsNamespace = ns;
    this.wsId = wsid;
  }

  open(): void {
    this.loading = true;
    this.conceptSetsService.getConceptSetsInWorkspace(this.wsNamespace, this.wsId).subscribe(
      (response) => {
        this.conceptSets = response.items.filter((concept) => {
          return concept.domain === this.selectedDomain;
        });
        this.existingSetSelected = this.conceptSets && this.conceptSets.length > 0;
        if (this.conceptSets && this.conceptSets.length > 0) {
          this.selectedConceptSet = this.conceptSets[0];
        }
        this.loading = false;
      }, (error) => {
      this.loading = false;
    });
    this.modalOpen = true;
    this.selectDomain = this.selectedDomain;
    this.selectConceptList = this.selectedConcepts
        .filter((concepts) => concepts.domainId.toUpperCase() ===
            this.selectDomain.toString().toUpperCase());
    this.name = '';
    this.description = '';
    this.errorNameReq = false;
    this.errorSaving = false;
    this.errorMsg = '';
  }

  close(): void {
    this.modalOpen = false;
  }

  selectChange(): void {
    this.existingSetSelected = !this.existingSetSelected;
  }

  save(): void {
    this.errorSaving = false;
    this.errorNameReq = false;
    this.saving = true;

    const conceptIds = [];
    this.selectConceptList.forEach((selected) => {
      conceptIds.push(selected.conceptId);
    });
    if (this.existingSetSelected) {
      const updateConceptSetReq: UpdateConceptSetRequest = {
        etag: this.selectedConceptSet.etag  ,
        addedIds: conceptIds
      };
      this.conceptSetsService.updateConceptSetConcepts(
        this.wsNamespace, this.wsId, this.selectedConceptSet.id, updateConceptSetReq)
        .subscribe((response) => {
          this.saving = false;
          this.modalOpen = false;
          this.saveComplete.emit();
        }, (error) => {
          this.errorMsg = error.toString();
        });
      return;
    }

    if (!this.name) {
      setTimeout(() => {
        this.errorNameReq = false;
        this.errorMsg = '';
      }, 5000);
      this.errorNameReq = true;
      this.errorMsg = 'Name is a required field';
      return;
    }

    const conceptSet: ConceptSet = {
      name: this.name,
      description: this.description,
      domain: this.selectDomain
    };
    const request: CreateConceptSetRequest = {
      conceptSet: conceptSet,
      addedIds: conceptIds
    };

    this.conceptSetsService.createConceptSet(this.wsNamespace, this.wsId, request)
      .subscribe((response) => {
        this.saving = false;
        this.modalOpen = false;
        this.saveComplete.emit();
      }, (error) => {
        this.errorSaving = true;
        if (error.status === 400) {
          this.errorMsg = 'Concept with same name already exist';
        } else {
          this.errorMsg = 'Error while saving concept please try again';
        }
        setTimeout(() => {
          this.errorSaving = false;
          this.errorMsg = '';
        }, 5000);
      });
  }
}
