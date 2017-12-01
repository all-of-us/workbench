import { Component, OnInit } from '@angular/core';
//import {NgForm,FormBuilder,FormGroup, Validators, AbstractControl} from '@angular/forms';
import { ActivatedRoute, ParamMap } from '@angular/router';
import { Concept, IConcept } from '../ConceptClasses';
import { AchillesService } from '../services/achilles.service';

@Component({
  selector: 'app-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.css']
})
export class SearchComponent implements OnInit {

  filterValueAr = [];
  conceptResults;
  conceptCard;
  redraw: number[] = []; // flag te redraw analysis , indexed exactly like analyses
  showCard = false;
  conceptArr;
  analyses = [];
  topTenAnalysis;
  conceptsArray = [];
  resetAnalyses;
  routeId: string;
  colors: ['#262262', '#8bc990', '#6cace4', '#f58771', '#f8c954', '#216fb4'];
  counter = 0;
  loading = true;
  clickedConcept: IConcept;
  toggleTree: boolean;
  vocabulary_id: string;
  savedSearchString: string;
  itemFromHeader;
  showAdvanced;
  toggleColumn = false;
  toggleAdv = true;
  searchTitle  = 'Standard Vocabularies Only';
  pageTitle = 'All Participants\' Summary Analyses';

  // Route domain map -- todo , these can be more than one eventually
  routeDomain = {
    'conditions': 'Condition',
    'procedures': 'Procedure',
    'drugs': 'Drug',
    'all': 'all',
    'icd9cm': 'ICD9CM',
    'icd9': 'ICD9',
    'icd10': 'ICD10',
    'icd10cm': 'ICD10CM',
    'cpt4': 'cpt4',
    'snomed': 'snomed',
  };
  // Map route to vocabulary_id
  routeVocabulary = {

    'all': 'all',
    'icd9cm': 'ICD9CM',
    'icd9': 'ICD9',
    'icd10': 'ICD10',
    'icd10cm': 'ICD10CM',
    'cpt4': 'CPT4',
    'snomed': 'SNOMED',
  };
  pageDomainId = null;


  constructor(private achillesService: AchillesService, private route: ActivatedRoute) {

    this.route.params.subscribe(params => {
      this.routeId = params.id;
      if (this.routeDomain[this.routeId]) {
        this.pageDomainId = this.routeDomain[this.routeId];
      }

      // What do we need for vocabulary_id
      if (this.routeVocabulary[this.routeId]) {
        this.vocabulary_id = this.routeVocabulary[this.routeId];
      }
      else {
        //this.vocabulary_id = this.routeId;
      }

      if (this.routeId != 'PPI') {
        this.toggleTree = false;
      } else {
        this.toggleTree = true;
      }


      //
    }); //end of subscribe




    const alist = [3000];
    this.achillesService.getSectionAnalyses( alist)
      .then(analyses => {
        this.analyses = analyses;
        this.resetAnalyses = analyses;
      }); //end of .subscribe

  }

  ngOnInit() {

  }
  ngOnChanges(){


  }


  /* Catch output from search table select or my concepts click and
    add to selected concepts list and run analysis if it isn't in list
    */
  itemSelected(obj) {
    const params = {
      search : this.savedSearchString,
      toggleTree: this.toggleTree,
      toggleAdv: this.toggleAdv
    };
    if (params.toggleTree) {
      params.search = '';
    }
    this.achillesService.logClickedConcept(obj, params);

    const item = obj;
    const children = obj.children;
    // Set the clicked concept to this one to trigger things on page
    this.clickedConcept = item;
    //
    // Check that it's not in concept list before adding
    let addConcept = true;
    const prevConceptsLen = this.conceptsArray.length;
    if (item.concept_id) {
      for (const c of this.conceptsArray) {
        if (c.concept_id == item.concept_id) {
          addConcept = false;
          return;
        }
      }
    }
    // if we have concept id or children , then add . icd trees have some parents that aren't concepts
    if (addConcept && item.concept_id !== null && item.concept_id > 0) {
      //
      this.conceptsArray.unshift(item);
      //this.runAnalysis(item);
    }

    // We clicked on a non concept tree parent .
    // Add icd or some tree node children to concept array
    // * We reset concept array so it only shows the group we clicked on
    else if (item.children.length) {
      this.conceptsArray = item.children;
      let c = null;
      for (c of item.children) {
        addConcept = true;
        if (c.concept_id ) {
          for (const a of this.conceptsArray) {
            if (c.concept_id == a.concept_id) {
              addConcept = false;
              break;
            }
          }
          if (addConcept) {
            this.conceptsArray.unshift(c);
          }
        }
      }
      // Run analyses if we added concept
      if (this.conceptsArray.length > prevConceptsLen) {
        //this.runAnalysis(null);
      }
    }


  }
  runAnalysis(node) {
    // note

    for (let i = 0; i < this.analyses.length; i++) {
      const b = this.analyses[i];


      // Put the selected concept as the stratum for the analysis
      const arr = [];
      for (let i = 0; i < this.conceptsArray.length; i++) {
        arr.push(this.conceptsArray[i].concept_id);
      }

      b.stratum[0] = arr.join(',');

      //
      // Run the analysis -- getting the results in the analysis.results
      this.achillesService.getAnalysisResults(b)
        .then(results => {
          b.results = results;
          // Put result for this concept on the concept.count
          // if node is passed , we update the count value for that one
          if (node) {
            for (const r of b.results) {
              if (r.stratum[0] == node.concept_id) {
                node.count = r.count_value;
                break;
              }
            }
          }
          //
          b.status = 'Done';
          this.redraw[i] = Math.random();
        }); //end of .then
    }//end of for loop

  }

/*
  drawConceptCard(arg) {
    this.showCard = true;
    this.conceptCard = arg;
  }
*/
  columnSwitch() {
    this.toggleColumn = !this.toggleColumn;
  }

  remove(node) {
    const index = this.conceptsArray.indexOf(node);
    if (index > -1) {
      this.conceptsArray.splice(index, 1);
    }
    // remove data from series for this concept;
    // this will remove it from the graph without redrawing graph and hitting DB
    this.removeFromSeries(node);
  }

  removeFromSeries(node) {
    // remove the result object from all the analysis on this page for this node
    for (let i = 0; i < this.analyses.length; i++) {
      const b = this.analyses[i];
      // Loop through results , remove if the result is for this node and trigger redraw of that analysis
      if (typeof b.results != 'undefined') {
        const removeIndexes = [];
        //
        for (let index = 0; index < b.results.length; index++) {
          const r = b.results[index];
          for (let i2 = 0; i2 < r.stratum.length; i2++) {
            if (r.stratum[i2] == node.concept_id) {
              removeIndexes.push(index);
              //
              //b.results.splice(index, 1);
              //this.redraw[index] = Math.random();
            }
          }
          // if (r.stratum[0] == node.concept_id || r.stratum[1] == node.concept_id) {
          //   b.results.splice(index, 1);
          // }
        }
        // Loop through the list to remove and remove them.
        // Note the offset is needed because the index changes when we splice it
        //removeIndexes.sort((a,b )=>  a-b); no need to sort as its already sorted
        let offset = 0;
        //
        for (const i3 of removeIndexes) {
          const realIndex = i3 - offset;
          b.results.splice(realIndex, 1);
          offset = 1;
        }
        if (removeIndexes.length) {
          this.redraw[i] = Math.random();
        }
      }
    }
    //find node's data in series
    //remove that node's data from series
    //redraw chart?
    //
  }


  reset() {
    this.conceptsArray = [];
    this.analyses = this.resetAnalyses;
    for (let i = 0; i < this.analyses.length; i++) {
      this.analyses[i].results = [];
    }
  }

  removeOne() {
    this.reset();
  }
  setString(event) {
    this.savedSearchString = event;
    //
  }
  sectionFromHeader(event){
//clear out the concepts and the analysis results on route change
    this.itemFromHeader = event;
    this.conceptsArray = [];
    for (const a of this.analyses){
      a.results = [];
    }

  }
  toggleBind(){
    if (this.toggleTree == false) {
      this.toggleAdv = true;
    }else{
      this.toggleAdv = false;
    }
  }



}
