import { IVocabulary } from './VocabularyClasses'

export interface IDomain{
  domain_id:string
  domain_display:string
  domain_desc:string
  domain_parent:string
  domain_route:string
  domain_vocab:IVocabulary[]
}

export class DomainClass {
  domain_id:string
  domain_display:string
  domain_desc:string
  domain_parent:string
  domain_route:string
  domain_vocab:IVocabulary[]


  constructor(obj?:any) {
    this.domain_id = obj && obj.domain_id
    this.domain_display = obj && obj.domain_display
    this.domain_desc = obj && obj.domain_desc
    this.domain_route = obj && obj.domain_route
    this.domain_parent = obj && obj.domain_parent
    this.domain_vocab = obj && obj.domain_vocab
  }
}
