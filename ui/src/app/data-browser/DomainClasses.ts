import { IVocabulary } from './VocabularyClasses';

export interface IDomain {
  domain_id: string;
  domain_display: string;
  domain_desc: string;
  domain_parent: string;
  domain_route: string;
  domain_vocab: IVocabulary[];
}

export class DomainClass {
  domain_id: string;
  domain_display: string;
  domain_desc: string;
  domain_parent: string;
  domain_route: string;
  domain_vocab: IVocabulary[];


  constructor(obj?: any) {
    this.domain_id = obj && obj.domainId;
    this.domain_display = obj && obj.domainDisplay;
    this.domain_desc = obj && obj.domainDesc;
    this.domain_route = obj && obj.domainRoute;
    this.domain_parent = obj && obj.domainParent;
    this.domain_vocab = obj && obj.domainVocab;
  }
}
