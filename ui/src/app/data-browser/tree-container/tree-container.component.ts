import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { TreeService } from '../services/tree.service';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { Location } from '@angular/common';
@Component({
  selector: 'app-tree-container',
  templateUrl: './tree-container.component.html',
  styleUrls: ['./tree-container.component.css']
})
export class TreeContainerComponent implements OnInit {
  conceptsArray = []
  pageDomainId = null;
  tree_nodes = {};
  routeDomain = {
    'Lifestyle': 'Lifestyle',
    'Overall-Health': 'OverallHealth',
    'Person-Medical-History': 'PersonalMedicalHistory',
    'Family-Medical-History': 'FamilyHistory'
  }
  vocabs =  [{vocabulary_id: 'PPI', name: 'PPI'}, {vocabulary_id: 'ICD9CM', name: 'ICD9CM'}, {vocabulary_id: 'ICD10CM', name: 'ICD10'},{vocabulary_id: 'ICD10CM', name: 'ICD10CM'}];

  vocabularyId
  routeId
  @Output() conceptsArrayEvent = new EventEmitter()
  constructor(private treeService: TreeService,
    private route: ActivatedRoute,
    private location: Location) {
      // Inititialize trees to first level
      for (let v of this.vocabs) {
        this.treeService.getTreeNodes(v.vocabulary_id, 0).subscribe(results => {
          this.tree_nodes[v.vocabulary_id] = results;
        })
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
    // reinit tree nodes on changes if the vocabularyId changed
    if (changes.vocabularyId.previousValue != changes.vocabularyId.currentValue ) {

    }

  }
  openVocabTree(v) {

    if (this.vocabularyId != v && this.tree_nodes[v].length == 0) {
      this.vocabularyId = v;

    }
  }

  passChildren(event) {
    this.conceptsArrayEvent.emit(event)
  }
  receiveItem(item) {

    this.conceptsArrayEvent.emit(item)
  }

}
