import { Component, OnInit, Input, Output, EventEmitter } from '@angular/core';
import { AchillesService } from '../services/achilles.service'
import { IConcept, Concept } from '../ConceptClasses'

@Component({
  selector: 'app-one-concept',
  templateUrl: './one-concept.component.html',
  styleUrls: ['./one-concept.component.css']
})
export class OneConceptComponent implements OnInit {
  // @Input() newConcept:IConcept; // Last concept added
  @Input() redraw;
  @Input() concept
  @Input() analyses
  @Input() routeId
  @Output() removeOneEmit = new EventEmitter()
  singleGraph = []

  // @Input() ppi
  // @Output() onParentSelected:EventEmitter<any> = new EventEmitter();
  initialized: boolean = false; // Flag to set initialized
  arrayConcept = [];
  show_source_graph = false
  show_source_table = false

  randNum

  constructor(private achillesService: AchillesService) {

  }

  //  makeChartOptions = this.analysis.hcChartOptions.bind(this.analysis);
  ngOnChanges() {

    let aids = [3001, 3002]
    this.show_source_graph = false
    this.show_source_table = false

    this.achillesService.getSectionAnalyses(aids)
      .then(analyses => {
        // this.hideTen = true;
        this.analyses = analyses;
        // Clone each analysis on the concept object so they have their own copy for results
        // This is run every time for a clarity drawer .
        let aclones = [];
        for (let a of this.analyses) {
          //
          aclones.push(this.achillesService.cloneAnalysis(a));
        }
        this.analyses = aclones;
        //
        this.achillesService.runAnalysis(this.analyses, this.concept);
        this.initialized = true;
        if (!this.concept.children) {
          this.concept.children = [];
        }

        //  Initialize first graph to show. If we have children show that. Otherwise first analyses
        if (this.concept.children.length > 0) {
          this.show_source_table = true;
          this.show_source_graph = false;
          for (let i = 0; i < this.analyses.length; i++) {
            this.analyses[i].showgraph = false
          }
        }
        else if (this.analyses.length > 0) {
          this.analyses[0].showgraph = true
        }

        this.randNum = Math.random();

        // Get any maps to parents and children and add them to the concept
        if (this.concept.vocabulary_id != "PPI") {
          this.achillesService.getConceptMapsTo(this.concept.concept_id, 2)
            .subscribe(data => {
              this.concept.children = data;
            });
          this.achillesService.getConceptMapsTo(this.concept.concept_id, 1)
            .subscribe(data => {
              this.concept.parents = data;
            });
        }
      });//end of .subscribe
  } // end of ngOnChanges()

  //  makeChartOptions = this.analysis.hcChartOptions.bind(this.analysis);
  ngOnInit() {

  }

  sendRemove(node) {
    this.removeOneEmit.emit(node)
  }

  graphBool(analysis, item) {
    // Toggle on graph items
    if (analysis == null) {
      if (item == 'source-graph') {
        // toggle children graph
        this.show_source_graph = !this.show_source_graph;
        if (this.show_source_graph) {
          this.show_source_table = false;
        }

      }
      else if (item == 'source-table') {
        this.show_source_table = !this.show_source_table
        if (this.show_source_table) {
          this.show_source_graph = false;
        }
      }
      if (this.show_source_table || this.show_source_graph) {
        for (let a of this.analyses) {
          a.showgraph = false;
        }
      }

      return;
    }

    else {
      for (let i = 0; i < this.analyses.length; i++) {
        if (this.analyses[i] == analysis) {
          if (this.analyses[i].showgraph == false || typeof (this.analyses[i].showgraph) == 'undefined') {

            this.analyses[i].showgraph = true;
            this.show_source_graph = false;
            this.show_source_table = false;

            // this.singleGraph.push(this.analyses[i])
          }
          else {
            this.analyses[i].showgraph = false;
            // this.singleGraph.splice(this.analyses[i], 1)
          }
        }
        else {
          this.analyses[i].showgraph = false
        }
      }
    }
    //
  }


}
