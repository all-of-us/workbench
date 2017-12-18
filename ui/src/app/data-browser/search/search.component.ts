import { Component } from '@angular/core';
import { Concept } from '../ConceptClasses';
import { AchillesService } from '../services/achilles.service';

@Component({
  selector: 'app-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.css']
})
export class SearchComponent {
  redraw: number[] = []; // flag te redraw analysis , indexed exactly like analyses
  analyses = [];
  conceptsArray = [];
  resetAnalyses;
  routeId: string;
  colors: ['#262262', '#8bc990', '#6cace4', '#f58771', '#f8c954', '#216fb4'];
  loading = true;
  clickedConcept: Concept;
  toggleTree: boolean;
  vocabulary_id: string;
  savedSearchString: string;
  itemFromHeader;
  toggleColumn = false;
  toggleAdv = true;
  pageTitle = 'All Participants\' Summary Analyses';
  pageDomainId = null;


  constructor(private achillesService: AchillesService) {
    const alist = [3000];
    this.achillesService.getSectionAnalyses(alist)
      .then(analyses => {
        this.analyses = analyses;
        this.resetAnalyses = analyses;
      });
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
    // Set the clicked concept to this one to trigger things on page
    this.clickedConcept = item;
    //
    // Check that it's not in concept list before adding
    let addConcept = true;
    if (item.concept_id) {
      for (const c of this.conceptsArray) {
        if (c.concept_id === item.concept_id) {
          addConcept = false;
          return;
        }
      }
    }
    // if we have concept id or children ,
      // then add . icd trees have some parents that aren't concepts
    if (addConcept && item.concept_id !== null && item.concept_id > 0) {
      this.conceptsArray.unshift(item);
    } else if (item.children.length) {
        // add children of item to concept array if it is a parent place holder concept
      this.conceptsArray = item.children;
      /*let c = null;
      for (c of item.children) {
        addConcept = true;
        if (c.concept_id ) {
          for (const a of this.conceptsArray) {
            if (c.concept_id === a.concept_id) {
              addConcept = false;
              break;
            }
          }
          if (addConcept) {
            this.conceptsArray.unshift(c);
          }
        }
      }*/

    }


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
      // Loop through results ,
        // remove if the result is for this node and trigger redraw of that analysis
      if (typeof b.results !== 'undefined') {
        const removeIndexes = [];
        //
        for (let index = 0; index < b.results.length; index++) {
          const r = b.results[index];
          for (let i2 = 0; i2 < r.stratum.length; i2++) {
            if (r.stratum[i2] === node.concept_id) {
              removeIndexes.push(index);
            }
          }
          // if (r.stratum[0] == node.concept_id || r.stratum[1] == node.concept_id) {
          //   b.results.splice(index, 1);
          // }
        }
        // Loop through the list to remove and remove them.
        // Note the offset is needed because the index changes when we splice it
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
  }

  sectionFromHeader(event) {
    // clear out the concepts and the analysis results on route change
    this.itemFromHeader = event;
    this.conceptsArray = [];
    for (const a of this.analyses){
      a.results = [];
    }

  }
  toggleBind() {
    if (this.toggleTree === false) {
      this.toggleAdv = true;
    } else {
      this.toggleAdv = false;
    }
  }



}
