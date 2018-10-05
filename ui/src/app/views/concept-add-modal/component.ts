import {Component, Input} from '@angular/core';
import {ActivatedRoute} from '@angular/router';

import {
  Concept,
  ConceptSet,
  ConceptSetsService, ConceptsService, Domain, UpdateConceptSetRequest
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
  conceptSets: ConceptSet[] = [];
  wsNamespace: string;
  wsId: string;
  name: string;
  description: string;
  selectedConceptSet: ConceptSet;
  selectConceptList: Concept[] = [];
  selectDomain: Domain;
  existingSetSelected = false;
  nameExistError = false;
  errorSaving = false;
  @Input() selectedDomain: Domain;
  @Input() selectedConcepts: Concept[];

  constructor(
    private conceptSetsService: ConceptSetsService,
    private conceptService: ConceptsService,
    private route: ActivatedRoute) {
    this.wsNamespace = this.route.snapshot.params['ns'];
    this.wsId = this.route.snapshot.params['wsid'];
  }

  open(): void {
    this.conceptSetsService.getConceptSetsInWorkspace(this.wsNamespace, this.wsId).subscribe(
        (response) => {
          this.conceptSets = response.items.filter((concept) => {
            return concept.domain === this.selectedDomain;
          });
          if (this.conceptSets && this.conceptSets.length > 0) {
            this.selectedConceptSet = this.conceptSets[0];
          }
          this.loading = false;
        }, (error) => {
          this.loading = false;
        });
    this.modalOpen = true;
    this.reset();
  }

  reset() {
    this.loading = true;
    this.selectDomain = this.selectedDomain;
    this.selectConceptList = this.selectedConcepts
        .filter((concepts) => concepts.domainId.toUpperCase() ===
            this.selectDomain.toString().toUpperCase());
    this.existingSetSelected = !this.existingSetSelected;
    this.nameExistError = false;
    this.errorSaving = false;
    this.name = '';
    this.description = '';
  }

  close(): void {
    this.modalOpen = false;
  }

  createConceptSet(): void {
    this.nameExistError = false;
    this.errorSaving = false;

    if (this.existingSetSelected) {
      const conceptIds = [];
      this.selectConceptList.forEach((selected) => {
        conceptIds.push(selected.conceptId);
      });
      const updateConceptSetReq: UpdateConceptSetRequest = {
        etag: this.selectedConceptSet.etag  ,
        addedIds: conceptIds
      };
      this.conceptSetsService.updateConceptSetConcepts(this.wsNamespace, this.wsId,
          this.selectedConceptSet.id, updateConceptSetReq)
          .subscribe((response) => {
            this.modalOpen = false;
          }, (error) => {
            this.errorSaving = true;
          });
      return;
    }

    this.conceptSetsService
        .createConceptSet(this.wsNamespace, this.wsId, {
          name: this.name,
          description: this.description,
          domain: this.selectDomain,
          concepts: this.selectConceptList
        })
        .subscribe((response) => {
          this.modalOpen = false;
        }, (error) => {
          this.errorSaving = true;
          this.existingSetSelected = false;
          // TODO(Neha) : better error message handling
        });
  }


}
