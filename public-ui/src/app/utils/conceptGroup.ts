import {ConceptWithAnalysis} from './conceptWithAnalysis';

export class ConceptGroup {
  group: string;
  groupName: string;
  concepts: ConceptWithAnalysis[] = [];

  constructor(group: string, groupName: string) {
    this.group = group;
    this.groupName = groupName;
  }
}
