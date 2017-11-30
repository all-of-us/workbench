import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Concept, IConcept } from '../ConceptClasses';
import { AchillesService } from '../services/achilles.service';
@Component({
  selector: 'app-mobile-charts',
  templateUrl: './mobile-charts.component.html',
  styleUrls: ['./mobile-charts.component.css']
})
export class MobileChartsComponent implements OnInit {
  // @Input() newConcept:IConcept; // Last concept added
  @Input() redraw;
  @Input() concept;
  @Input() analyses;
  @Output() removalEmit = new EventEmitter();
  singleGraph = [];

  // @Input() ppi
  // @Output() onParentSelected:EventEmitter<any> = new EventEmitter();
  initialized = false; // Flag to set initialized
  arrayConcept = [];
  randNum;
  show_source_graph = false;

  constructor(private achillesService: AchillesService) {

  }

  ngOnChanges() {
    // //
  }

  //  makeChartOptions = this.analysis.hcChartOptions.bind(this.analysis);
  ngOnInit() {
    const section = 3000;
    const aids = [3101, 3102];
    //
    this.achillesService.getSectionAnalyses(aids)
      .then(analyses => {
        // this.hideTen = true;

        this.analyses = analyses;
        // Clone each analysis on the concept object so they have their own copy for results
        //
        // This is run every time for a clarity drawer .
        const aclones = [];
        for (const a of this.analyses) {
          //
          aclones.push(this.achillesService.cloneAnalysis(a));
        }
        this.analyses = aclones;

        console.log('clone a', this.analyses);
        this.achillesService.runAnalysis(this.analyses, this.concept);
        this.initialized = true;
        if (!this.concept.children) { this.concept.children = []; }

        this.randNum = Math.random();



        // Get any maps to parents and children and add them to the concept
        if (this.concept.vocabulary_id != 'PPI') {
          this.achillesService.getConceptMapsTo(this.concept.concept_id, 2)
            .subscribe(data => {
              this.concept.children = data;
            });
          this.achillesService.getConceptMapsTo(this.concept.concept_id, 1)
            .subscribe(data => {
              this.concept.parents = data;
            });
        }


      }); //end of .subscribe



  }

  sendRemove(node) {
    this.removalEmit.emit(node);
  }

  graphBool(analysis) {
    if (analysis == null){
      // SHow children graph
      if (this.show_source_graph == false) {
      this.show_source_graph = true;
    }
    else {
      this.show_source_graph = false;
    }
      for (const a of this.analyses) {
        a.showgraph = false;
      }
      return;
    }
else {
    for (let i = 0; i < this.analyses.length; i++) {
      if (this.analyses[i] == analysis) {
        if (this.analyses[i].showgraph == false || typeof (this.analyses[i].showgraph) == 'undefined') {
          this.analyses[i].showgraph = true;
          this.show_source_graph = false;
          // this.singleGraph.push(this.analyses[i])
        }
        else {
          this.analyses[i].showgraph = false;
          // this.singleGraph.splice(this.analyses[i], 1)
        }
      }
      else {
          this.analyses[i].showgraph = false;
      }
    }
  }
    //
  }


}
