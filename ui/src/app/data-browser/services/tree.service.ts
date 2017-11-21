import { Injectable } from '@angular/core';
import { Http } from '@angular/http';
import { AchillesService } from './achilles.service';
import { AnalysisSection } from '../analysisClasses';
import 'rxjs/Rx'; //unlocks all rxjs operators, such as map()
import { ConceptPpi, Concept } from '../ConceptClasses'



@Injectable()
export class TreeService {
  private base_url: string = "https://cpmdev.app.vumc.org/api/public/";

  constructor(private http: Http, private achillesService: AchillesService) { }
  getTree() {


    return this.http.get('https://cpmdev.app.vumc.org/api/public/codebook?ancestor=0')

      .map(
      (response) => {
        const data = response.json();
        return data
      }
      )
  }

  getChildren(node: any) {
    //
    return this.achillesService.getAnalysisConcepts(node.section_id, node.name).then(data => {
      return data;
    })
  }

  // Return Nodes and options for a ABC concept tree
  getSectionAbcTree(section: AnalysisSection) {
    let roots = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W',
      'X', 'Y', 'Z'];
    let nodes = [];
    for (let ltr of roots) {
      let obj = {
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
    if (vocabulary_id == "PPI") {
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
    var url = this.base_url + "codebook_full?domain_id=" + domainId
    return this.http.get(url)
      .map(
      (response) => {
        let data = response.json();
        // Get rid of the first three items PMI, Consent PII, EHRConsent
        data.data.splice(0,3);
        return data.data.map(item => {
          item.domain_id = item.concept_name;
          return new ConceptPpi(item);
        });
      });
  }
  getVocabTree(vocabulary_id: string, parent_id: number) {
    vocabulary_id = vocabulary_id.toLowerCase();
    var url = this.base_url + "vocab_tree?vocabulary_id=" + vocabulary_id + "&parent_id=" + parent_id
    return this.http.get(url)
      .map(
      (response) => {
        const data = response.json();
        return data.data.map(item => {
          return new Concept(item);
        });
      });
  }
}
