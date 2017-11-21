import { Injectable } from '@angular/core';
import { Headers, Http, URLSearchParams } from '@angular/http';
import { Observable } from 'rxjs';
import 'rxjs/Rx'; //unlocks all rxjs operators, such as map()
import { AnalysisSection } from '../analysisClasses';
import { Analysis, AnalysisResult, IAnalysis, IAnalysisResult } from '../analysisClasses';
import { AnalysisDist, AnalysisDistResult } from '../analysisSubClasses';
import { AnalysisMap } from '../analysisMap';
import { Concept } from '../ConceptClasses';
import { Vocabulary } from '../VocabularyClasses';
import { DomainClass } from '../domain-class';


import 'rxjs/add/operator/toPromise';

@Injectable()
export class AchillesService {

  private baseUrl = "https://cpmdev.app.vumc.org/api/public/";
//  private baseUrl = "https://dev.aou-api.org/api/public/";
  private sections = [];

  // private baseUrl = "https://cpmsolr-test.app.vumc.org/WebAPI/cdmresults/AOU/";
  //private baseUrl = "/api/"; //http://api.ohdsi.org/WebAPI/cdmresults/1PCT/person
  constructor(
    private http: Http
  ) { }
  private handleError(error: any): Promise<any> {
    console.error("An error occurred: ", error);
    return Promise.reject(error.message || error);
  }


  getAnalysisResults(a: IAnalysis): Promise<any[]> {

    var url = this.baseUrl + 'analysis_result';
    // var q = '?analysis_id='+a.analysis_id+'&dataType='+a.dataType;
    var q = {
      'analysis_id': a.analysis_id,
      'dataType': a.dataType
    }

    let i = 0;
    for (i = 0; i < a.stratum.length; i++) {
      /*
      Todo: split stratum one on a comma for multiple stratum id's
      */

      if (a.stratum[i] != '') {
        if (typeof a.stratum[i] == "string") {
          var makeArray = a.stratum[i].split(",");
        } else {
          makeArray = [a.stratum[i]];
        }
        // //
        let i_name = i + 1;
        let stratumVar = "stratum_" + i_name + '[]'; // must add [] for array params
        q[stratumVar] = makeArray
      }
    }

    //
    return this.http.get(url, { search: q })
      .toPromise()
      .then(response => {
        let data = response.json();

        // For age data , analyis 3, bin the data by decade
        for (let i = 0; i < data.length; i++) {
          if (data[i].analysis_id == 3002) {
            // data = this.binDataByDecade(data);
            //
            data = this.binDataByAge(data);
            break;
          }
        }

        if (!data.length) { return []; }
        let r = data.map(item => {
          let ar = null;
          if (a.dataType == 'distribution') {
            ar = new AnalysisDistResult(item);
            return ar;
          }
          if (a.chartType == 'map') {
            ar = new AnalysisResult(item);
          }

          else {

            ar = new AnalysisResult(item);
          }

          return ar;
        });
        ////
        return r;
      })
      .catch(this.handleError);
  }

  getAllConceptResults(args?) {

    let q: any = {
      "concept_id": args.concept_id,
      "stratum[]": args.stratum,
    }




    var url = this.baseUrl + "all_concept_results";

    return this.http.get(url, { search: q })
      // return this.http.get(url)
      .map(
      (response) => {
        const data = response.json();

      })
  } // end of getAllConceptResults()

  getSections(routeId: string): Promise<AnalysisSection[]> {
    // Get sections for this routeId. These are used to make the header menu
    var url = this.baseUrl + 'analysis_sections';
    //
    url = url + "?route_id=" + routeId;

    //var  url = "assets/data/concept_analysis_counts_1.json"
    const sectionAnalysesMap = {
      0: [1, 2, 3, 4, 12, 1101],
      100: [101, 102],
      400: [3000, 3001, 3002], // Conditions
      500: [500, 501, 505, 506],
      600: [600, 601, 606],
      700: [700, 701, 703, 706],
      800: [800, 801, 803, 805, 806],
      1100: [1101, 1103],
      2000: [2000, 2001, 2002, 2003],
      3000: [3000, 3001, 3002],
      3100: [3000, 3001, 3002], // Lifestyle survey
      3200: [3000, 3001, 3002], // Overall Health

    }
    return this.http.get(url)
      .toPromise()
      .then(response => {
        let sections: AnalysisSection[] = response.json().map(item => {
          if (sectionAnalysesMap[item.section_id]) {
            item.analyses = sectionAnalysesMap[item.section_id];
          }
          else {
            item.analyses = sectionAnalysesMap[3000];
          }
          // item.analyses.push();
          return item as AnalysisSection;
        });
        return sections;
      })
      .catch(this.handleError);
    //return  this.getData(['analysis_sections']).then();
  }

  /* Get all the analysis Objects we want to run for an analysisSection
     If justThese is an array , just return ones with id in array  */

  //getSectionAnalyses(section_id: number, aids?: number[]): Observable<IAnalysis[]> {
  getSectionAnalyses(section_id: number, aids?: number[]): Promise<IAnalysis[]> {

    var url = this.baseUrl + 'analyses';
    return this.http.get(url)
      .toPromise()
      .then(response => {
        //.map((response) => {
        let data = response.json();
        // If justThese array analysis_ids then ,  filter list to those analysis_ids
        if (aids && aids.length) {
          data = data.filter((item) => aids.indexOf(item.analysis_id) != -1);
        }
        // //
        return data.map(item => {

          if (item.dataType == 'distribution') {
            let a = new AnalysisDist(item);
            //
            return a;
          }
          if (item.chartType == 'map') {
            return new AnalysisMap(item);
          } else if (item.analysis_id == 3) {
            let a = new Analysis(item);
          }
          // Return generic count analysis
          return new Analysis(item);
        });


      });

  }

  getAnalysisConcepts(section_id: number, name_search?: string): Promise<any[]> {
    var url = this.baseUrl + 'analysis_concepts?section_id=' + section_id + '&name_search=' + name_search;

    // //
    return this.http.get(url)
      .toPromise()
      .then((response) => {
        let data = response.json();
        // //
        return data;
      });
  }

  getConceptResults(args) {
    //
    let q =
      {
        concept_name: args.search,
        page: args.page,
        page_len: args.page_len,
        page_from: args.page_from,
        page_to: args.page_to,
        observed: args.observed,
      }
    if (args.observed) {
      q.observed = true;
    }
    if (args.standard_concept == 's') {
      q['standard_concept'] = "s"
    }
    //datagrid filters
    if (args.filters) {

      for (let b of args.filters) {
        /*if (b.property == "domain_id") {
          q['domain_id[]'] = [b.value];
        }
        if (b.property == "vocabulary_id") {
          q['vocabulary_id[]'] = b.value
        }*/
        q[b.property] = b.value;

      }
    }

    if (args.sort) {
      q['sortCol'] = args.sort.by

      if (args.sort.reverse == false) {
        q['sortDir'] = 'ASC'
      } else {
        q['sortDir'] = 'DESC'
      }
    }


    let vocabs = []; // array of vocabulary_id filters
    let domains = [];
    for (let v of Object.keys(args.standard_vocabs)) {
      if (args.standard_vocabs[v] == true) {
        vocabs.push(v);
      }
    }
    for (let v of Object.keys(args.source_vocabs)) {
      if (args.source_vocabs[v] == true) {
        vocabs.push(v);
      }
    }
    if (vocabs.length > 0) {
      q['vocabulary_id[]'] = vocabs
    }

    for (let v of Object.keys(args.domains)) {
      if (args.domains[v] == true) {
        domains.push(v);
      }
    }
    if (domains.length > 0) {
      q['domain_id[]'] = domains
    }


    var url = this.baseUrl + 'concept_search';

    return this.http.get(url, { search: q })
      //return this.http.get(url)
      .map(
      (response) => {
        const data = response.json();

        //
        data.data = data.data.map(item => {
          return new Concept(item);
        });
        return data;

      });

  }

  logSearchParams(args) {
    return;
    /*
    let url = this.baseUrl + "log/search"
    //URLSearchParams needed to make a post request
    let urlSearchParams = new URLSearchParams();

    for (let v of Object.keys(args.standard_vocabs)) {
      if (args.standard_vocabs[v] == true) {
        urlSearchParams.append('vocabulary[]', v);
      }
    }
    for (let v of Object.keys(args.source_vocabs)) {
      if (args.source_vocabs[v] == true) {
        urlSearchParams.append('vocabulary[]', v);
      }
    }
    for (let d of Object.keys(args.domains)) {
      if (args.domains[d] == true) {
        urlSearchParams.append('domain[]', d);
      }
    }
    urlSearchParams.append('search', args.search);
    urlSearchParams.append('advanced', args.advanced);
    return this.http.post(url, urlSearchParams).subscribe();
    */

  }
  logClickedConcept(concept, params) {
    return;
    /*
    let url = this.baseUrl + "log/concept_click";
    let urlSearchParams = new URLSearchParams();
    urlSearchParams.append('concept_id', concept.concept_id);
    urlSearchParams.append('search', params.search);
    urlSearchParams.append('toggleTree', params.toggleTree);
    urlSearchParams.append('toggleAdv', params.toggleTree);
    return this.http.post(url, urlSearchParams).subscribe();
    */
  }

  getSearchVocabFilters() {
    //
    var url = this.baseUrl + "/concept_vocabularies"
    return this.http.get(url)
      .map(
      (response) => {
        const data = response.json();
        //
        return data.data;
      }
      )
  }

  binDataByAge(data) {
    //

    let newResults = [{ analysis_id: 3002, count_value: 1, stratum_1: '' }, { analysis_id: 3002, count_value: 1, stratum_1: '' }, { analysis_id: 3002, count_value: 1, stratum_1: '' }, { analysis_id: 3002, count_value: 1, stratum_1: '' }, { analysis_id: 3002, count_value: 1, stratum_1: '' }, { analysis_id: 3002, count_value: 1, stratum_1: '' }, { analysis_id: 3002, count_value: 1, stratum_1: '' }, { analysis_id: 3002, count_value: 1, stratum_1: '' }, { analysis_id: 3, count_value: 1, stratum_1: '' }, { analysis_id: 3, count_value: 1, stratum_1: '' }];
    let value1 = 0, value2 = 0, value3 = 0, value4 = 0, value5 = 0, value6 = 0, value7 = 0, value8 = 0, value9 = 0, value10 = 0
    for (let i = 0; i < data.length; i++) {
      let age = data[i].stratum_2;
      let count = parseInt(data[i].count_value)
      if (age < 20) {
        newResults[0].count_value = count;
        newResults[0].stratum_1 = "under 20";
      }
      if (age == 20) {
        newResults[1].count_value = count;
        newResults[1].stratum_1 = age;
      }
      if (age == 30) {
        newResults[2].count_value = count;
        newResults[2].stratum_1 = age;
      }
      if (age == 40) {
        newResults[3].count_value = count;
        newResults[3].stratum_1 = age;
      }
      if (age == 50) {
        newResults[4].count_value = count;
        newResults[4].stratum_1 = age;
      }
      if (age == 60) {
        newResults[5].count_value = count;
        newResults[5].stratum_1 = age;
      }
      if (age == 70) {
        newResults[6].count_value = count;
        newResults[6].stratum_1 = age;
      }
      if (age == 80) {
        newResults[7].count_value = count;
        newResults[7].stratum_1 = age;
      }
      if (age == 90) {
        newResults[8].count_value = count;
        newResults[8].stratum_1 = age;
      }
      if (age == 100) {
        newResults[9].count_value = count;
        newResults[9].stratum_1 = age;
      }


    }

    //

    return newResults
  }


  binDataByDecade(data) {

    let value = 0, newResults = [{ analysis_id: 3, count_value: 1, stratum_1: 0 }, { analysis_id: 3, count_value: 1, stratum_1: 0 }, { analysis_id: 3, count_value: 1, stratum_1: 0 }, { analysis_id: 3, count_value: 1, stratum_1: 0 }, { analysis_id: 3, count_value: 1, stratum_1: 0 }, { analysis_id: 3, count_value: 1, stratum_1: 0 }, { analysis_id: 3, count_value: 1, stratum_1: 0 }, { analysis_id: 3, count_value: 1, stratum_1: 0 }, { analysis_id: 3, count_value: 1, stratum_1: 0 }, { analysis_id: 3, count_value: 1, stratum_1: 0 }, { analysis_id: 3, count_value: 1, stratum_1: 0 }, { analysis_id: 3, count_value: 1, stratum_1: 0 }];
    let value1 = 0, value2 = 0, value3 = 0, value4 = 0, value5 = 0, value6 = 0, value7 = 0, value8 = 0, value9 = 0, value10 = 0, value11 = 0, value12 = 0
    //loop though results starting at 1900 and create count bins for every decade.
    //will also have to create new categories to repersent each decade.



    for (let i = 0; i < data.length; i++) {
      let birthYear = parseInt(data[i].stratum_1)
      if (birthYear >= 1900 && birthYear < 1910) {
        value1 = data[i].count_value + value1
        newResults[0].count_value = value1
        newResults[0].stratum_1 = 1900
      }
      if (birthYear >= 1910 && birthYear < 1920) {
        value2 = data[i].count_value + value2
        newResults[1].count_value = value2
        newResults[1].stratum_1 = 100
      }
      if (birthYear >= 1920 && birthYear < 1930) {
        value3 = data[i].count_value + value3
        newResults[2].count_value = value3
        newResults[2].stratum_1 = 1920
      }
      if (birthYear >= 1930 && birthYear < 1940) {
        value4 = data[i].count_value + value4
        newResults[3].count_value = value4
        newResults[3].stratum_1 = 1930
      }
      if (birthYear >= 1940 && birthYear < 1950) {
        value5 = data[i].count_value + value5
        newResults[4].count_value = value5
        newResults[4].stratum_1 = 1940
      }
      if (birthYear >= 1950 && birthYear < 1960) {
        value6 = data[i].count_value + value6
        newResults[5].count_value = value6
        newResults[5].stratum_1 = 1950
      }
      if (birthYear >= 1960 && birthYear < 1970) {
        value7 = data[i].count_value + value7
        newResults[6].count_value = value7
        newResults[6].stratum_1 = 1960
      }
      if (birthYear >= 1970 && birthYear < 1980) {
        value8 = data[i].count_value + value8
        newResults[7].count_value = value8
        newResults[7].stratum_1 = 1970
      }
      if (birthYear >= 1980 && birthYear < 1990) {
        value9 = data[i].count_value + value9
        newResults[8].count_value = value9
        newResults[8].stratum_1 = 1980
      }
      if (birthYear >= 1990 && birthYear < 2000) {
        value10 = data[i].count_value + value10
        newResults[9].count_value = value10
        newResults[9].stratum_1 = 1990
      }
      if (birthYear >= 2000 && birthYear < 2010) {
        value11 = data[i].count_value + value11
        newResults[10].count_value = value11
        newResults[10].stratum_1 = 2000
      }
      if (birthYear >= 2010 && birthYear < 2020) {
        value12 = data[i].count_value + value12
        newResults[11].count_value = value12
        newResults[11].stratum_1 = 2010
      }
    }

    //
    return newResults


  }
  //pass the concept obj in an array of analyses and the analyses with only that concept obj
  //or
  //pass an array of analyses with the stratum and it will run ( no concept Obj )
  runAnalysis(analyses: any[], concept?: any) {
    //

    for (let i = 0; i < analyses.length; i++) {
      // getAnalysisResults to get results

      if (concept && typeof concept.concept_id !== 'undefined') {
        analyses[i].stratum[0] = concept.concept_id.toString();
      }

      // Put the selected concept as the stratum for the analysis
      var arr = []

      // analyses[i].stratum[0] = arr.join(",");
      //
      // Run the analysis -- getting the results in the analysis.results
      this.getAnalysisResults(analyses[i])
        .then(results => {
          analyses[i].results = results;
          ////
          analyses[i].status = 'Done';

        });//end of .then
    }//end of for loop

  }

  cloneAnalysis(analysis: IAnalysis) {
    let aclass = analysis.constructor.name;
    //
    let a = null;
    if (aclass == 'Analysis') {
      a = new Analysis(analysis);

    }

    if (aclass == 'AnalysisDist') {
      a = new AnalysisDist(analysis);

    }
    let rand = Math.random();
    a.stratum.push(rand);
    a.results = []; // clear out any results that were in there
    return a;

  }

  cloneConceptChildren(children) {
    let aclass = children.constructor.name;
    //
    let a = null;

    if (aclass == 'Concept') {
      a = new Concept(children);
    }


    return a;

  }

  // Pass concept_id and concept_num = 2
  // and This will Get concepts that map to the concept arg (Children of the concept ) : concept_num = 2
  // Pass concept_id and  concept_num = 1 if you want the parent(s) of the concept
  // Example -- to get the children of snomed concept_id , pass concept_num=2
  getConceptMapsTo(arg, concept_num: number) {
    var url = this.baseUrl + 'concept_maps_to?concept_id=' + arg + '&concept_num=' + concept_num;
    //
    return this.http.get(url)
      .map(response => {
        const data = response.json();
        return data.map(item => {
          return new Concept(item);
        });
      }
      )

  }

  VocabShow(args: any) {
    //
    let q: any =
      {
        vocabulary_id: args.vocabulary_id,
        standard_concept: args.standard_concept,
        domain_id: args.domain_id
      }
    var url = this.baseUrl + 'vocab_show';
    return this.http.get(url, { search: q })
      .map(
      (response) => {
        const data = response.json();

        //
        return data.map(item => {
          return new Vocabulary(item);
        });

      }
      )


  }

  getDomains() {
    var url = this.baseUrl + "domain"
    return this.http.get(url)
      .map(
      (response) => {
        const data = response.json();
        return data.data.map(item => {
          return new DomainClass(item);
        });

      }
      )

  }


}
