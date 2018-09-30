import {Component, OnInit, ViewChild} from '@angular/core';
import {ActivatedRoute} from '@angular/router';
import {ConceptSetsService} from '../../../generated/api/conceptSets.service';
import {CreateConceptModalComponent} from '../concept-create-modal/component';

@Component({
  styleUrls: [
    '../../styles/cards.css'],
  templateUrl: './component.html',
})
export class ConceptsListComponent implements OnInit {
  wsNamespace: string;
  wsId: string;

  constructor(private conceptsService: ConceptSetsService,
              private route: ActivatedRoute) {
  this.wsNamespace = this.route.snapshot.params['ns'];
  this.wsId = this.route.snapshot.params['wsid'];
}

  @ViewChild(CreateConceptModalComponent)
  conceptCreateModal: CreateConceptModalComponent;

  addConcept() {
   this.conceptCreateModal.open();
  }

  ngOnInit() {
 this.conceptsService.getConceptSetsInWorkspace(this.wsNamespace, this.wsId)
       .subscribe((response) => {
     const res = response;
   });
  }
}
