import {Component, EventEmitter, Input, OnChanges, Output} from '@angular/core';
import {IAnalysis} from '../AnalysisClasses';
import { AchillesService } from '../services/achilles.service';

@Component({
  selector: 'app-one-concept',
  templateUrl: './one-concept.component.html',
  styleUrls: ['./one-concept.component.css']
})
export class OneConceptComponent implements OnChanges {

  @Input() concept;
  @Output() removeOneEmit = new EventEmitter();

  show_source_graph = false;
  show_source_table = false;
  analyses: IAnalysis[];
  analysis_show_graph = { }; // flags for toggling analyses graphs off on and on

  constructor(private achillesService: AchillesService) {

  }

  //  makeChartOptions = this.analysis.hcChartOptions.bind(this.analysis);
  ngOnChanges() {
    const aids = [3101, 3102];
    this.show_source_graph = false;
    this.show_source_table = false;

    this.achillesService.getSectionAnalyses(aids)
      .then(analyses => {
        // this.hideTen = true;
        this.analyses = analyses;
        // Initialieze show graph for analyses
        for (const a of this.analyses) {
          this.analysis_show_graph[a.analysis_id] = false;
        }

        // Get any maps to parents and children and add them to the concept
        if (this.concept.vocabulary_id !== 'PPI') {
          this.achillesService.getChildConcepts(this.concept.concept_id)
            .subscribe(data => {
              this.concept.children = data;
            });
          this.achillesService.getParentConcepts(this.concept.concept_id)
            .subscribe(data => {
              this.concept.parents = data;
            });
        }

        this.achillesService.runAnalysis(this.analyses, this.concept);
        //  Initialize first graph to show. If we have children show that. Otherwise first analyses
        if (this.concept.children.length > 0) {
          this.show_source_table = true;
          this.show_source_graph = false;

        } else if (this.analyses.length > 0) {
          this.analysis_show_graph[analyses[0].analysis_id] = true;
        }

      }); // end of .subscribe
  } // end of ngOnChanges()



  sendRemove(node) {
    this.removeOneEmit.emit(node);
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
