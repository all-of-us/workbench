import { Component, EventEmitter, Input, OnChanges, Output } from '@angular/core';
import { Router } from '@angular/router';
import {IAnalysis} from '../AnalysisClasses';
import { AchillesService } from '../services/achilles.service';

@Component({
  selector: 'app-concept-drawer',
  templateUrl: './concept-drawer.component.html',
  styleUrls: ['./concept-drawer.component.css']
})

export class ConceptDrawerComponent implements OnChanges {
  @Input() redraw;
  @Input() concept;
  @Input() analyses;
  @Input() ppi;
  @Output() onParentSelected: EventEmitter<any> = new EventEmitter();

  show_source_graph = false;
  show_source_table = false;
  analysis_show_graph = {};
  sourcesCountAnalysis: IAnalysis;

  constructor(private achillesService: AchillesService, private router: Router) {
  }

  parentClick(concept: any) {
    this.onParentSelected.emit(concept);
  }

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

    // Initialize show graph for analyses
    for (const a of this.analyses) {
      this.analysis_show_graph[a.analysis_id] = false;
    }
    this.achillesService.runAnalysis(this.analyses, this.concept);

    // Get any maps to parents and children and add them to the concept
    if (this.concept.vocabulary_id !== 'PPI') {
      this.achillesService.getChildConcepts(this.concept.concept_id)
        .subscribe(data => {
          this.concept.children = data;
          // Initialize first graph to show.
            // If we have children show that. Otherwise first analyses
          if (this.concept.children.length > 0) {
            this.show_source_table = true;
            this.show_source_graph = false;
            this.sourcesCountAnalysis =
                this.achillesService.makeConceptsCountAnalysis(this.concept.children);
            for (const a of this.analyses) {
              this.analysis_show_graph[a.analysis_id] = false;
            }
          } else if (this.analyses.length > 0) {
            this.analysis_show_graph[this.analyses[0].analysis_id] = true;
          }
        });
      this.achillesService.getParentConcepts(this.concept.concept_id)
        .subscribe(data => {
          this.concept.parents = data;
        });
    } else {
      // Set the default shown graph / table
      if (this.concept.children.length > 0) {
        this.show_source_table = true;
        this.show_source_graph = false;
        this.sourcesCountAnalysis =
          this.achillesService.makeConceptsCountAnalysis(this.concept.children);
        for (const a of this.analyses) {
          this.analysis_show_graph[a.analysis_id] = false;
        }
      } else if (this.analyses.length > 0) {
        this.analysis_show_graph[this.analyses[0].analysis_id] = true;
      }
    }

  } // end of ngOnChanges()

  routeToSurvey(id) {
    const link = [id];
    this.router.navigate(link);
  }

  // Handle graph button toggle click
  toggleGraphs(analysis, item) {
     if (analysis === null) {
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
          this.analysis_show_graph[a.analysis_id] = false;
        }
      }

      return;
    } else {
      for (const a of this.analyses) {
        if (a.analysis_id === analysis.analysis_id) {
          if (this.analysis_show_graph[a.analysis_id] === false) {
            this.analysis_show_graph[a.analysis_id] = true;
            this.show_source_graph = false;
            this.show_source_table = false;
          } else {
            this.analysis_show_graph[a.analysis_id] = false;
          }
        } else {
          this.analysis_show_graph[a.analysis_id] = false;
        }
      }
    }
  }

}
