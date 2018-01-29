import { Injectable } from '@angular/core';
import 'rxjs/Rx';
import {CohortBuilderService} from '../../../generated/api/cohortBuilder.service';
import { Concept } from '../ConceptClasses';


@Injectable()
export class TreeService {
  constructor(
      private api: CohortBuilderService) { }

  // Get Tree Nodes based on vocabulary_id and maybe domain id
  getTreeNodes(vocabulary_id: string, parent_id: number, domain_id = null) {
    if (vocabulary_id === 'PPI') {
      // todo this tree return this.getSurveySections(domain_id);
    } else {
      return this.getVocabTree(vocabulary_id, parent_id);
    }
  }

  getVocabTree(vocabulary_id: string, parent_id: number) {
    vocabulary_id = vocabulary_id.toLowerCase();

    return this.api.getCriteriaByTypeAndParentId(1, vocabulary_id, parent_id)
      .map(
      (response) => {
        console.log('Vocab Resuponse ', response);
        return response.items.map(item => {
          return Concept.conceptFromCriteria(item);
        });
      });
  }
}
