import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { AchillesService } from '../services/achilles.service';
import { IConcept, Concept } from '../ConceptClasses';
import { ConceptDrawerComponent } from '../concept-drawer/concept-drawer.component';


@Component({
  selector: 'app-my-concepts',
  templateUrl: './my-concepts.component.html',
  styleUrls: ['./my-concepts.component.css']
})
export class MyConceptsComponent implements OnInit {
  @Input() conceptsArray: IConcept[] = []
  @Input() newConcept: IConcept; // Last concept added
  @Input() route: string
  @Output() removalEmit = new EventEmitter()
  @Output() resetConcepts = new EventEmitter()
  @Output() addEmit = new EventEmitter()
  @Output() resetEmit = new EventEmitter()
  analyses
  countAnalysis

  redraw: number[] = []; // flag te redraw analysis , indexed exactly like analyses

  constructor(private achillesService: AchillesService) {
    let section = 3000;
    let aids = [3000, 3001, 3002]
    //
    this.analyses = [];
    this.countAnalysis = null
    this.achillesService.getSectionAnalyses(section, aids)
      .then(analyses => {
        // Get the count analysis separate id 3000
        //this.analyses = analyses;

        for (let a of analyses) {
          if (a.analysis_id == 3000) {
            this.countAnalysis = a;
          }
          else {
            this.analyses.push(a);
          }
        }

        // Call onchanges when this comes back to do some drawing
        this.ngOnChanges();

      });

  }

  // Handle click on a parent in the drawer:
  // Set the concept to the newConcept so it expands in the drawer
  // Add it to the concept array if it is  the drawer.
  // We do this by just emitting to the component (ie search page ), so it can add to its array too and graph it
  drawerParentSelected(item: IConcept) {
    this.addEmit.emit(item);
  }

  ngOnInit() {
    //
  }
  ngOnChanges() {
    let colors = ['#262262', '#8bc990', '#6cace4', '#f58771', '#f8c954', '#216fb4'] //colors we use for our charts
    let b = 0
    for (let i = 0; i < this.conceptsArray.length; i++) {
      this.conceptsArray[i].color = colors[b];
      b++
      if (b >= 6) { //forces to loop back through our colors Array.
        b = 0;
      }

    }


    // Run total count analysis for all concepts
    if (this.countAnalysis) {
      if (this.conceptsArray.length) {


        // Clone the analysis so chart updates
        this.countAnalysis = this.achillesService.cloneAnalysis(this.countAnalysis);
        var arr = []
        for (let i = 0; i < this.conceptsArray.length; i++) {
          arr.push(this.conceptsArray[i].concept_id)
        }

        this.countAnalysis.stratum[0] = arr.join(",");
        // Run the analysis -- getting the results in the analysis.results
        this.achillesService.getAnalysisResults(this.countAnalysis)
          .then(results => {
            for (let i = 0; i < this.conceptsArray.length; i++) {
              for (let b = 0; b < results.length; b++) {
                if (this.conceptsArray[i].concept_name == results[b].stratum_name[0]) {
                  results.splice(i, 0, results.splice(b, 1)[0]);
                }
              }
            }
            this.countAnalysis.results = results;

          });
      } else {
        this.countAnalysis.results = []
      }
    }
  }  //end of onChanges()

  reset() {
    this.resetEmit.emit()
  }

  sendRemove(node) {
    this.removalEmit.emit(node)
    this.removeFromSeries(node)
  }

  removeFromSeries(node) {
    let results = this.countAnalysis.results;
    for (let i = 0; i < results.length; i++) {
      if (results[i].stratum_name[0] == node.concept_name) {
        results.splice(i, 1);
        this.countAnalysis.results = results
        if (this.countAnalysis.results.length) {
          this.redraw[i] = Math.random();
        }
      }
    }
    this.ngOnChanges()
  }




}
