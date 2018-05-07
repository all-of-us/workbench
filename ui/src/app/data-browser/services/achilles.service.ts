import { Injectable } from '@angular/core';
import {DataBrowserService} from 'publicGenerated';
import 'rxjs/add/operator/toPromise';
import 'rxjs/Rx'; // unlocks all rxjs operators, such as map()
import { Analysis, AnalysisResult, IAnalysis} from '../AnalysisClasses';
import { AnalysisDist, AnalysisDistResult } from '../AnalysisSubClasses';
import { Concept} from '../ConceptClasses';
import { DomainClass } from '../DomainClasses';

@Injectable()
export class AchillesService {
  private totalParticipants: number;

  constructor(
     private api: DataBrowserService
  ) {
    api.getParticipantCount().subscribe(result => this.totalParticipants = result.countValue);
  }
  private handleError(error: any): Promise<any> {
    console.error('An error occurred: ', error);
    return Promise.reject(error.message || error);
  }

  getTotalParticipants() {
    return this.totalParticipants;
  }
  // Return an Analysis object with results for the concepts array.
  // This analysis obj can then be passed to app-chart
  makeConceptsCountAnalysis(concepts: Concept[]): Analysis {
      const obj = {
          analysisId: 3000,
          analysisName: 'Number of Participants',
          results: [],
          chartType: 'column',
          dataType: 'counts',
          stratum1Name: ['Concept id']
      };
      for (let i = 0; i < concepts.length; i++) {
          const ar =  new AnalysisResult({
              analysisId: 3000,
              stratum1Name: concepts[i].concept_name,
              stratum1: concepts[i].concept_id,
              countValue: concepts[i].count_value
          });
          obj.results.push(ar);
      }
      const a =  new Analysis(obj);
      return a;
  }

  getAnalysisResults(a: IAnalysis): Promise<any[]> {
      // Todo take out databrowser in next merge from ui
      return null;
  }

  /* Get all the analysis Objects we want to run for an analysisSection
     If justThese is an array , just return ones with id in array  */
  getSectionAnalyses( aids: number[]): Promise<IAnalysis[]> {
    return null;
  }
  getConceptResults(args) {

    /* Todo make object to pass with search params
    *
     const q = {
        concept_name: args.search,
        page: args.page,
        page_len: args.page_len,
        page_from: args.page_from,
        page_to: args.page_to,
        observed: args.observed,
    };

    if (args.observed) {
      q.observed = true;
    }
    if (args.standard_concept === 's') {
      q['standard_concept'] = 's';
    }
    // datagrid filters
    if (args.filters) {

      for (const b of args.filters) {
        q[b.property] = b.value;

      }
    }
    if (args.sort) {
      q['sortCol'] = args.sort.by;

      if (args.sort.reverse === false) {
        q['sortDir'] = 'ASC';
      } else {
        q['sortDir'] = 'DESC';
      }
    }

    const vocabs = []; // array of vocabulary_id filters
    const domains = [];
    for (const v of Object.keys(args.standard_vocabs)) {
      if (args.standard_vocabs[v] === true) {
        vocabs.push(v);
      }
    }
    for (const v of Object.keys(args.source_vocabs)) {
      if (args.source_vocabs[v] === true) {
        vocabs.push(v);
      }
    }
    if (vocabs.length > 0) {
      q['vocabulary_id[]'] = vocabs;
    }

    for (const v of Object.keys(args.domains)) {
      if (args.domains[v] === true) {
        domains.push(v);
      }
    }
    if (domains.length > 0) {
      q['domain_id[]'] = domains;
    }

    */
    let search = args.search;
    if (!search || search === '') {
      search = null;
    }
    return this.api.getConceptsSearch(search).map(
      (response) => {
        const  data = response.items.map(item => {
          return new Concept(item);
        });
        return {data: data, totalItems: data.length};

      });

  }

  logSearchParams(args) {
    return;
  }
  logClickedConcept(concept, params) {
    return;
  }

  // pass the concept obj in an array of analyses and the analyses with only that concept obj
  // or
  // pass an array of analyses with the stratum and it will run ( no concept Obj )
  runAnalysis(analyses: any[], concept?: any) {
    for (let i = 0; i < analyses.length; i++) {
      // getAnalysisResults to get results

      if (concept && typeof concept.concept_id !== 'undefined') {
        analyses[i].stratum[0] = concept.concept_id.toString();
      }

      // Run the analysis -- getting the results in the analysis.results
      this.getAnalysisResults(analyses[i])
        .then(results => {
          analyses[i].results = results;
          ////
          analyses[i].status = 'Done';
        }); // end of .then
    } // end of for loop
  }

  cloneAnalysis(analysis: IAnalysis) {
    const aclass = analysis.constructor.name;
    let a = null;
    if (aclass === 'Analysis') {
      a =  Analysis.analysisClone(analysis);
    }
    if (aclass === 'AnalysisDist') {
      a = new AnalysisDist(analysis);
    }
    a.results = []; // clear out any results that were in there
    return a;
  }

  cloneConceptChildren(children) {
    const aclass = children.constructor.name;
    //
    let a = null;
    if (aclass === 'Concept') {
      a = new Concept(children);
    }
    return a;
  }

  // Get concept children for the concept
  getChildConcepts(concept_id: number) {
    return this.api.getChildConcepts(concept_id)
      .map(response => {
        const data = response.items;
        return data.map(item => {
          return new Concept(item);
        });
      });
  }
  // Get concept parents for the concept
  getParentConcepts(concept_id: number) {
    return this.api.getParentConcepts(concept_id)
      .map(response => {
        const data = response.items;
        return data.map(item => {
          return new Concept(item);
        });
      });
  }

  /* todo
  VocabShow(args: any) {
    const q: any = {
        vocabulary_id: args.vocabulary_id,
        standard_concept: args.standard_concept,
        domain_id: args.domain_id
      };
    const url = this.baseUrl + 'vocab_show';
    return this.http.get(url, { search: q })
      .map(
      (response) => {
        const data = response.json();

        //
        return data.map(item => {
          return new Vocabulary(item);
        });

      }
      );
  }
  */

  getDomains() {

     return this.api.getDbDomains().map(data => {
             console.log('Doamin data ', data);
             return data.items.map(item => {
                 return new DomainClass(item);
             });
         }
     );
  }

}
