
import { Component, EventEmitter, Output } from '@angular/core';
import { TreeService } from '../services/tree.service';
@Component({
  selector: 'app-tree-container',
  templateUrl: './tree-container.component.html',
  styleUrls: ['./tree-container.component.css']
})
export class TreeContainerComponent {
  conceptsArray = [];
  pageDomainId = null;
  tree_nodes = {};
  routeDomain = {
    'Lifestyle': 'Lifestyle',
    'Overall-Health': 'OverallHealth',
    'Person-Medical-History': 'PersonalMedicalHistory',
    'Family-Medical-History': 'FamilyHistory'
  };
  vocabs =  [{type: 'PPI', id: 0, vocabulary_id: 'PPI', name: 'PPI'},
      {type: 'ICD9', id: 0, vocabulary_id: 'ICD9', name: 'ICD9'},
      {type: 'ICD10', id: 0, vocabulary_id: 'ICD10', name: 'ICD10'}];

  vocabularyId;
  routeId;
  @Output() conceptsArrayEvent = new EventEmitter();
  constructor(private treeService: TreeService) {
      // Inititialize trees to first level
      for (const v of this.vocabs) {
        this.treeService.getTreeNodes(v.vocabulary_id, 0).subscribe(results => {
          this.tree_nodes[v.vocabulary_id] = results;
        });
      }


    }

  openVocabTree(v: any) {
    if (this.vocabularyId !== v && this.tree_nodes[v].length === 0) {
      this.vocabularyId = v;
    }
  }

  passChildren(event: any) {
    this.conceptsArrayEvent.emit(event);
  }
  receiveItem(item: any) {
    this.conceptsArrayEvent.emit(item);
  }

}
