import {Component, EventEmitter, Input, OnChanges, OnInit, Output} from '@angular/core';
import { IAnalysis } from '../AnalysisClasses';
import { Concept } from '../ConceptClasses';
import { AchillesService } from '../services/achilles.service';


@Component({
  selector: 'app-my-concepts',
  templateUrl: './my-concepts.component.html',
  styleUrls: ['./my-concepts.component.css']
})
export class MyConceptsComponent implements OnInit, OnChanges {
  @Input() conceptsArray: Concept[] = [];
  @Input() newConcept: Concept; // Last concept added
  @Input() route: string;
  @Output() removalEmit = new EventEmitter();
  @Output() resetConcepts = new EventEmitter();
  @Output() addEmit = new EventEmitter();
  @Output() resetEmit = new EventEmitter();
  analyses;
  countAnalysis: IAnalysis;

  redraw: number[] = []; // flag te redraw analysis , indexed exactly like analyses

  constructor(private achillesService: AchillesService) {
    const aids = [3000, 3101, 3102];
    this.analyses = [];
    this.countAnalysis = null;
    this.achillesService.getSectionAnalyses( aids)
      .then(analyses => {
        // Get the count analysis separate id 3000
        for (const a of analyses) {
          if (a.analysis_id === 3000) {
            this.countAnalysis = a;
          } else {
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
  // We do this by just emitting to the component (ie search page ),
    // so it can add to its array too and graph it
  drawerParentSelected(item: Concept) {
    this.addEmit.emit(item);
  }

  ngOnInit() {
  }

  ngOnChanges() {
    const colors = ['#262262', '#8bc990', '#6cace4', '#f58771', '#f8c954', '#216fb4'];
    // colors we use for our charts
    let b = 0;
    for (let i = 0; i < this.conceptsArray.length; i++) {
      this.conceptsArray[i].color = colors[b];
      b++;
      if (b >= 6) { // forces to loop back through our colors Array.
        b = 0;
      }

    }

    console.log('Count analysis', this.countAnalysis);
    // Run total count analysis for all concepts
    if (this.countAnalysis) {
        if (this.conceptsArray) {
            this.countAnalysis = this.achillesService.makeConceptsCountAnalysis(this.conceptsArray);
        }
    }
  }  // end of onChanges()

  reset() {
    this.resetEmit.emit();
  }

  sendRemove(node) {
    this.removalEmit.emit(node);
    this.removeFromSeries(node);
  }

  removeFromSeries(node) {
    const results = this.countAnalysis.results;
    for (let i = 0; i < results.length; i++) {
      if (results[i].stratum_name[0] === node.concept_name) {
        results.splice(i, 1);
        this.countAnalysis.results = results;
        if (this.countAnalysis.results.length) {
          this.redraw[i] = Math.random();
        }
      }
    }
    this.ngOnChanges();
  }




}
