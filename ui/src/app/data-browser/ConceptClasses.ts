
  // Concept Classes
import {Criteria} from '../../generated/model/criteria';

  export interface IConcept {
  concept_name: string;
  prevalence: string;
  concept_id: number;
  concept_class_id: string;
  concept_code: string;
  vocabulary_id: string;
  domain_id: string;
  count_value: number;
  children: IConcept [];
  parents: IConcept [];
  id: number; // icd concepts have id for tree info
  is_group: boolean;
  is_selectable: boolean;
  color: string;
  moreInfo(): string;
}


export class Concept implements IConcept {
  concept_name: string;
  prevalence: string;
  concept_id: number;
  concept_class_id: string;
  concept_code: string;
  vocabulary_id: string;
  domain_id: string;
  count_value: number;
  children: IConcept[];
  parents: IConcept[];
  id: number ; // icd concepts have id for tree info
  is_group: boolean;
  is_selectable: boolean;
  color: string;

  static conceptFromCriteria(obj: Criteria):  Concept {
        const concept = new Concept();
        concept.concept_name = obj.name;
        concept.concept_id = obj.conceptId;
        concept.count_value = obj.count;
        concept.is_group = obj.group;
        concept.is_selectable = obj.selectable;
        concept.domain_id = obj.domainId;
        concept.id = obj.id;
        concept.concept_code = obj.code;
        concept.vocabulary_id = obj.type;
        return concept;
    }
  constructor(obj?: any) {
    this.concept_name = obj && obj.conceptName || '';
    this.concept_class_id = obj && obj.conceptClassId || '';
    this.prevalence = obj && obj.prevalence || 0;
    this.concept_id = obj && obj.conceptId || 0;
    this.concept_code = obj && obj.conceptCode || '';
    this.vocabulary_id = obj && obj.vocabularyId || '';
    this.domain_id = obj && obj.domainId || '';
    this.count_value = obj && obj.countValue || 0;
    this.children = [];
    this.parents = [];
    this.id = obj && obj.id || 0;
    this.is_group = obj && obj.isGroup || false;
    this.is_selectable = obj && obj.isSelectable || false;
  }

    moreInfo(): string {
        return 'More information about concept class ' + this.constructor.name;
    }




}

export class ConceptPpi extends Concept {
  children: Array<any>;
  // survey: Survey;
  // Return more info about survey and whatever for this concept

  moreInfo(): string {
    return 'ConceptPPI More info on Survey and such';
  }
 // constructing from codebook ppi
 // Todo:
  constructor(obj?: any) {
    super(obj);
    this.concept_name = obj && obj.display;
    this.vocabulary_id = 'PPI';
    this.count_value = 0;
    this.children = [];
    this.parents = [];
    // create new object for each children
    // set on this.children

    // for eaech child make a random number between 1000-10000
    // in ppi drawer show chard of children's counts
    if (obj.children) {
      for (const c of obj.children) {
        c.count_value = ranNum();
        c.domain_id = obj.domain_id;
        this.children.push(new ConceptPpi(c));
      }
    }
    if (obj.parents) {
      for (const c of obj.parents) {
        c.count_value = ranNum();
        c.domain_id = obj.domain_id;
        this.parents.push(new ConceptPpi(c));
      }
    }
    function ranNum(min = 1000, max = 10000) {
      min = Math.ceil(min);
      max = Math.floor(max);
      return Math.floor(Math.random() * (max - min)) + min;
    }
  } // End constructor
}


export class Survey {
  title: string;
  link: string;

  constructor(obj?: any) {
    this.title = obj && obj.title || 'Untitled Survey Object';
  }
}
