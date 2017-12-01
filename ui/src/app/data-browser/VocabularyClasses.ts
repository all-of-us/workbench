export interface IVocabulary {
  domain_id: string;
  vocabulary_id: string;
  vocabulary_name: string;
  vocabulary_reference: string;
  count_value: number;
  standard_concept: string;
  weight: number;
}

export class Vocabulary implements IVocabulary {
  domain_id: string;
  vocabulary_id: string;
  vocabulary_name: string;
  vocabulary_reference: string;
  count_value: number;
  standard_concept: string;
  weight: number;

  constructor(obj?: any) {
    this.domain_id = obj && obj.domain_id || '';
    this.vocabulary_id = obj && obj.vocabulary_id || 0;
    this.vocabulary_name = obj && obj.vocabulary_name || '';
    this.vocabulary_reference = obj && obj.vocabulary_reference || '';
    this.count_value = obj && obj.count_value || '';
    this.standard_concept = obj && obj.standard_concept || '';
    this.weight = obj && obj.weight || '';
  }
}
