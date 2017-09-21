import { Component, Input, EventEmitter, Output, ChangeDetectorRef } from '@angular/core';
import { BroadcastService } from '../service';
import { SearchResult, SearchGroup, Subject, CriteriaType } from '../model';

@Component({
  selector: 'app-search-group',
  templateUrl: 'search-group.component.html',
  styleUrls: ['search-group.component.css']
})
export class SearchGroupComponent {

  /**
   * The search group for this component.
   *
   * @type {SearchGroup}
   * @memberof SearchGroupComponent
   */
  @Input()
  public searchGroup: SearchGroup;

  /**
   * The group set for this component.
   *
   * @type {Subject[]}
   * @memberof SearchGroupComponent
   */
  public groupSet: Subject[] = [];

  /**
   * Event emitter when the delete search group button is pushed.
   *
   * @type {EventEmitter<SearchGroup>}
   * @memberof SearchGroupComponent
   */
  @Output()
  public onRemove = new EventEmitter<SearchGroup>();

  @Input()
  public index: number;

  constructor(private changeDetectorRef: ChangeDetectorRef,
              private broadcastService: BroadcastService) {}

  /**
   * Remove the specified SearchGroup.
   *
   * @public
   * @param {SearchGroup}
   * @memberof SearchGroupComponent
   */
  public removeSearchGroup(searchGroup: SearchGroup) {
    this.onRemove.emit(searchGroup);
  }

  /**
   * Remove the specified SearchGroup.
   *
   * @public
   * @param {SearchGroup}
   * @memberof SearchGroupComponent
   */
  public removeExclusionGroup(searchGroup: SearchGroup) {
    this.onRemove.emit(searchGroup);
  }

  /**
   * Remove the specified SearchResult.
   *
   * @public
   * @param {SearchResult}
   * @memberof SearchGroupComponent
   */
  public removeSearchResult(searchResult: SearchResult) {
    const index: number = this.searchGroup.results.indexOf(searchResult);
    if (index !== -1) {
      this.searchGroup.results.splice(index, 1);
    }
    this.changeDetectorRef.detectChanges();
    this.broadcastService.removeSearchResult(this.searchGroup);
  }

  selectSearchGroup() {
    this.broadcastService.selectSearchGroup(this.searchGroup);
  }

}
