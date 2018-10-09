import {ConceptAnalysis} from '../../publicGenerated/model/conceptAnalysis';

export class ConceptWithAnalysis {
  conceptId: string;
  conceptName: string;
  analyses: ConceptAnalysis;
  chartType: string;
  unitNames: string[];

  constructor(conceptId: string, conceptName: string, chartType: string, unitNames: string[]) {
    this.conceptId = conceptId;
    this.conceptName = conceptName;
    this.chartType = chartType;
    this.unitNames = unitNames;
  }
}


