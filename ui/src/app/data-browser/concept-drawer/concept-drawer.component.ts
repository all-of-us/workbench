import { Component, EventEmitter, Input, Output } from '@angular/core';
// import { Chart } from 'angular-highcharts';
import { Router } from '@angular/router';

import { Analysis } from '../AnalysisClasses';
import { IConcept } from '../ConceptClasses';
import { AchillesService } from '../services/achilles.service';

@Component({
  selector: 'app-concept-drawer',
  templateUrl: './concept-drawer.component.html',
  styleUrls: ['./concept-drawer.component.css']
})

export class ConceptDrawerComponent {
  @Input() redraw;
  @Input() concept;
  @Input() analyses;
  @Input() ppi;
  @Output() onParentSelected: EventEmitter<any> = new EventEmitter();
  initialized = false; // Flag to set initialized
  arrayConcept = [];
  randNum;
  singleGraph = [];
  show_source_graph = false;
  show_source_table = false;

  constructor(private achillesService: AchillesService, private router: Router) {
  }

  parentClick(concept: any) {
    this.onParentSelected.emit(concept);
  }

  //  makeChartOptions = this.analysis.hcChartOptions.bind(this.analysis);
  ngOnChanges() {
    this.show_source_graph = false;
    this.show_source_table = false;
    // This is run every time for a clarity drawer .
    const aclones = [];

    if (this.analyses) {
      for (const a of this.analyses) {
        aclones.push(this.achillesService.cloneAnalysis(a));
      }
    }

    this.analyses = aclones;
    //
    this.achillesService.runAnalysis(this.analyses, this.concept);
    this.initialized = true;
    if (!this.concept.children) { this.concept.children = []; }

    this.randNum = Math.random();
    // Get any maps to parents and children and add them to the concept
    if (this.concept.vocabulary_id !== 'PPI') {
      this.achillesService.getConceptMapsTo(this.concept.concept_id, 2)
        .subscribe(data => {
          this.concept.children = data;
          // Initialize first graph to show.
            // If we have children show that. Otherwise first analyses
          if (this.concept.children.length > 0) {
            this.show_source_table = true;
            this.show_source_graph = false;
            for (let i = 0; i < this.analyses.length; i++) {
              this.analyses[i].showgraph = false;
            }
          }
          else if (this.analyses.length > 0){
            this.analyses[0].showgraph = true;
          }
        });
      this.achillesService.getConceptMapsTo(this.concept.concept_id, 1)
        .subscribe(data => {
          this.concept.parents = data;
        });
    }

  } // end of ngOnChanges()

  routeToSurvey(id) {
    const link = [id];
    this.router.navigate(link);
  }

  graphBool(analysis, item) {
    // Toggle on graph items
    if (analysis == null) {
      if (item === 'source-graph') {
        // toggle children graph
        this.show_source_graph = !this.show_source_graph;
        if (this.show_source_graph) {
            this.show_source_table = false;
        }
      } else if (item === 'source-table') {
        this.show_source_table = !this.show_source_table;
        if (this.show_source_table) {
          this.show_source_graph = false;
        }
      }
      if (this.show_source_table || this.show_source_graph) {
        for (const a of this.analyses) {
          a.showgraph = false;
        }
      }
      return;
    } else {
      for (let i = 0; i < this.analyses.length; i++) {
        if (this.analyses[i] === analysis) {
          if (this.analyses[i].showgraph === false ||
              typeof (this.analyses[i].showgraph) === 'undefined') {
            this.analyses[i].showgraph = true;
            this.show_source_graph = false;
            this.show_source_table = false;
          } else {
            this.analyses[i].showgraph = false;
          }
        } else {
            this.analyses[i].showgraph = false;
        }
      }
    }
  }

}
