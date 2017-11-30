import { Location } from '@angular/common';
import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { ActivatedRoute, ParamMap } from '@angular/router';
import {TreeComponent} from '../../cohort-search/criteria-wizard/tree/tree.component';
import { TreeService } from '../services/tree.service';
@Component({
  selector: 'app-tree-container',
  templateUrl: './tree-container.component.html',
  styleUrls: ['./tree-container.component.css']
})
export class TreeContainerComponent implements OnInit {
  conceptsArray = [];
  pageDomainId = null;
  tree_nodes = {};
  routeDomain = {
    'Lifestyle': 'Lifestyle',
    'Overall-Health': 'OverallHealth',
    'Person-Medical-History': 'PersonalMedicalHistory',
    'Family-Medical-History': 'FamilyHistory'
  };
  vocabs =  [{type: 'PPI', id: 0, vocabulary_id: 'PPI', name: 'PPI'}, {type: 'ICD9', id: 0, vocabulary_id: 'ICD9', name: 'ICD9'}, {type: 'ICD10', id: 0, vocabulary_id: 'ICD10', name: 'ICD10'}];

  vocabularyId;
  routeId;
  @Output() conceptsArrayEvent = new EventEmitter();
  constructor(private treeService: TreeService,
    private route: ActivatedRoute,
    private location: Location) {
      // Inititialize trees to first level
      for (const v of this.vocabs) {
        this.treeService.getTreeNodes(v.vocabulary_id, 0).subscribe(results => {
          this.tree_nodes[v.vocabulary_id] = results;
        });
      }


    }

  ngOnInit() {
  //  Note, on changes is always run the first time too so don't do stuff in both
    /*this.route.params.subscribe(params => {
      //get parameter from URL...  example:  http://localhost:4200/data-browser/drug  <--drug is params.id, which is defined in router.
      this.routeId = params.id
    });
    //initialize the tree nodes
    this.treeService.getTreeNodes(this.vocabularyId, 0).subscribe(results => {
      this.tree_nodes = results;
    })
    */
  }
  ngOnChanges(changes) {


  }
  openVocabTree(v) {

    if (this.vocabularyId != v && this.tree_nodes[v].length == 0) {
      this.vocabularyId = v;

    }
  }

  passChildren(event) {
    this.conceptsArrayEvent.emit(event);
  }
  receiveItem(item) {

    this.conceptsArrayEvent.emit(item);
  }

}
