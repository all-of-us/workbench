import { Injectable, OnInit, OnDestroy } from '@angular/core';
import { Observable } from 'rxjs/Observable';
import 'rxjs/add/operator/map';
import { Criteria, CriteriaType, SearchRequest, SearchResponse } from '../model';
import { Http } from '@angular/http';

const CRITERIA: CriteriaType[] = [
  { id: 1, name: 'Demographics', type: 'demo' },
  { id: 2, name: 'ICD9 Codes', type: 'icd9' },
  { id: 3, name: 'ICD10 Codes', type: 'icd10' },
  { id: 4, name: 'PheCodes', type: 'phecodes' },
  { id: 5, name: 'CPT Codes', type: 'cpt' },
  { id: 6, name: 'Medications', type: 'meds' },
  { id: 7, name: 'Labs', type: 'labs' },
  { id: 8, name: 'Vitals', type: 'vitals' },
  { id: 8, name: 'Temporal', type: 'temporal' }
];

const AGE_AT_EVENT: string[] = ['Any', 'GTE >=', 'LTE <=', 'Between'];

const EVENT_DATE: string[] = ['Any', 'Within x year(s)', 'GTE >=', 'LTE <=', 'Between'];

const VISIT_TYPE: string[] = ['Any', 'Inpatient visit', 'Outpatient visit'];

const DAYS_OR_YEARS: string[] = ['Days', 'Years'];

const HAS_OCCURRENCES: string[] = ['Any', '1 or more', 'within x days/years', 'x days/years apart'];

@Injectable()
export class SearchService {

  private baseUrl = 'http://localhost:8080';

  constructor(private http: Http) {}

  getCriteriaTypes(): CriteriaType[] {
    return CRITERIA;
  }

  getAgeAtEventSelectList(): string[] {
    return AGE_AT_EVENT;
  }

  getEventDateSelectList(): string[] {
    return EVENT_DATE;
  }

  getHasOccurrencesSelectList(): string[] {
    return HAS_OCCURRENCES;
  }

  getVisitTypeSelectList(): string[] {
    return VISIT_TYPE;
  }

  getDaysOrYearsSelectList(): string[] {
    return DAYS_OR_YEARS;
  }

  getParentNodes(type: string): Observable<Criteria[]> {
    return this.http.get('/api/' + type.toLowerCase() + '/0')
      .map(res => res.json());
  }

  getChildNodes(criteria: any): Observable<Criteria[]> {
    return this.http.get('/api/' + criteria.type.toLowerCase() + '/' + criteria.id)
      .map(res => res.json());
  }

  getResults(searchRequest: SearchRequest): Observable<SearchResponse> {
    return this.http.post('/api/searchrequest', searchRequest)
      .map(
        res => res.json()
      );
  }

}
