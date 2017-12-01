import { Injectable } from '@angular/core';
import { Http } from '@angular/http';
import 'rxjs/Rx';
import {CohortBuilderService} from '../../../generated/api/cohortBuilder.service';
import { AnalysisSection } from '../AnalysisClasses';
import { Concept, ConceptPpi } from '../ConceptClasses';
import { AchillesService } from './achilles.service';


@Injectable()
export class TreeService {
  private base_url = 'https://cpmdev.app.vumc.org/api/public/';

  constructor(private http: Http, private achillesService: AchillesService, private api: CohortBuilderService) { }


  // Return Nodes and options for a ABC concept tree
  getSectionAbcTree(section: AnalysisSection) {
    const roots = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W',
      'X', 'Y', 'Z'];
    const nodes = [];
    for (const ltr of roots) {
      const obj = {
        'section_id': section.section_id,
        'name': ltr,
        'analysis_ids': [],
        //  'children': [],
        'hasChildren': true
      };
      nodes.push(obj);
    }
    return { 'nodes': nodes, 'options': null };
  }

  // Get Tree Nodes based on vocabulary_id and maybe domain id
  getTreeNodes(vocabulary_id: string, parent_id: number, domain_id = null) {
    //
    if (vocabulary_id == 'PPI') {
      return this.getSurveySections(domain_id);
    }
    else {
      return this.getVocabTree(vocabulary_id, parent_id);
    }

    // else call http with vocabulary_id and domain id
    // return getGeneralTreeData(vocabulary_id, domain_id);
  }
  getSurveySections(domainId) {
    //
    const url = this.base_url + 'codebook_full?domain_id=' + domainId;
    return this.http.get(url)
      .map(
      (response) => {
        const data = response.json();
        // Get rid of the first three items PMI, Consent PII, EHRConsent
        data.data.splice(0, 3);
        return data.data.map(item => {
          item.domain_id = item.concept_name;
          return new ConceptPpi(item);
        });
      });
  }
  getVocabTree(vocabulary_id: string, parent_id: number) {
    vocabulary_id = vocabulary_id.toLowerCase();

    return this.api.getCriteriaByTypeAndParentId(vocabulary_id, parent_id)
      .map(
      (response) => {
        console.log("Vocab Resuponse ", response);
        return response.items.map(item => {
          return Concept.conceptFromCriteria(item);
        });
      });
  }
}
