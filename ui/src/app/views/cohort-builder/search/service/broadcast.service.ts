import { Injectable, ComponentRef } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { ReplaySubject } from 'rxjs/ReplaySubject';
import { SearchCriteria, SearchGroup, SearchResult, Modifier } from '../model';

@Injectable()
export class BroadcastService {

  /**
   * Represents the selected criteria in the
   * criteria tree.
   */
  private selectedCriteria = new Subject<SearchCriteria>();

  /**
   * Represents the selected criteria type.
   */
  private selectedCriteriaType = new ReplaySubject<string>(1);

  /**
   * Represents the selected search group for the
   * active modal.
   */
  private selectedSearchGroup = new ReplaySubject<SearchGroup>(1);

  /**
   * Represents the selected search result for the
   * active modal if editing.
   */
  private selectedSearchResult = new ReplaySubject<SearchResult>(1);

  /**
   * Represents the counts need updating.
   */
  private updatedCounts = new Subject<any>();

  /**
   * Represents the counts need updating.
   */
  private removedSearchResult = new Subject<any>();

  /**
   * Represents the charts that need updating.
   */
  private updatedCharts = new Subject<any>();

  /**
   * Represents the summary page that needs updating.
   */
  private summaryCriteriaGroup = new Subject<SearchCriteria[]>();

  /**
   * Represents the summary page that needs updating.
   */
  private summaryModifierList = new Subject<Modifier[]>();

  /**
   * The Observable that allows other components
   * to subscribe to this criteria subject.
   */
  public selectedCriteria$ = this.selectedCriteria.asObservable();

  /**
   * The Observable that allows other components
   * to subscribe to this criteria type subject.
   */
  public selectedCriteriaType$ = this.selectedCriteriaType.asObservable();

  /**
   * The Observable that allows other components
   * to subscribe to this search group subject.
   */
  public selectedSearchGroup$ = this.selectedSearchGroup.asObservable();

  /**
   * The Observable that allows other components
   * to subscribe to this search result subject.
   */
  public selectedSearchResult$ = this.selectedSearchResult.asObservable();

  /**
   * The Observable that allows other components
   * to subscribe to this updated counts subject.
   */
  public updatedCounts$ = this.updatedCounts.asObservable();

  /**
   * The Observable that allows other components
   * to subscribe to this updated counts subject.
   */
  public removedSearchResult$ = this.removedSearchResult.asObservable();

  /**
   * The Observable that allows other components
   * to subscribe to this updated charts subject.
   */
  public updatedCharts$ = this.updatedCharts.asObservable();

  /**
   * The Observable that allows other components
   * to subscribe to this event.
   */
  public summaryCriteriaGroup$ = this.summaryCriteriaGroup.asObservable();

  /**
   * The Observable that allows other components
   * to subscribe to this event.
   */
  public summaryModifierList$ = this.summaryModifierList.asObservable();

  /**
   * Add the specified criteria to the subject.
   *
   * @param criteria
   */
  selectCriteria(criteria: SearchCriteria) {
    this.selectedCriteria.next(criteria);
  }

  /**
   * Add the specified criteria type to the subject.
   *
   * @param criteriaType
   */
  selectCriteriaType(criteriaType: string) {
    this.selectedCriteriaType.next(criteriaType);
  }

  /**
   * Add the specified search group to the subject.
   *
   * @param searchGroup
   */
  selectSearchGroup(searchGroup: SearchGroup) {
    this.selectedSearchGroup.next(searchGroup);
  }

  /**
   * Add the specified search result to the subject.
   *
   * @param searchResult
   */
  selectSearchResult(searchResult: SearchResult) {
    this.selectedSearchResult.next(searchResult);
  }

  /**
   * Add the specified searchGroup to the subject,
   * so observers know to update the counts.
   *
   * @param searchGroup
   */
  updateCounts(searchGroup: SearchGroup, searchResult: SearchResult) {
    this.updatedCounts.next({'searchGroup': searchGroup, 'searchResult': searchResult});
  }

  /**
   * Add the specified searchGroup to the subject,
   * so observers know to update the counts.
   *
   * @param searchGroup
   */
  removeSearchResult(searchGroup: SearchGroup) {
    this.removedSearchResult.next({'searchGroup': searchGroup});
  }

  /**
   * Add the specified data to the subject,
   * so observers know to update the charts.
   *
   * @param gender
   * @param race
   */
  updateCharts(gender: any[], race: any[]) {
    this.updatedCharts.next({'gender': gender, 'race': race});
  }

  setSummaryCriteriaGroup(criteriaList: SearchCriteria[]) {
    this.summaryCriteriaGroup.next(criteriaList);
  }

  setSummaryModifierList(modifierList: Modifier[]) {
    this.summaryModifierList.next(modifierList);
  }
}
