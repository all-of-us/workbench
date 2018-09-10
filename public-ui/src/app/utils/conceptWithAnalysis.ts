import {ConceptAnalysis} from '../../publicGenerated/model/conceptAnalysis';

export class ConceptWithAnalysis {
  conceptId: string;
  conceptName: string;
  analyses: ConceptAnalysis;
  chartType: string;

  constructor(conceptId: string, conceptName: string, chartType: string) {
    this.conceptId = conceptId;
    this.conceptName = conceptName;
    this.chartType = chartType;
  }
}


