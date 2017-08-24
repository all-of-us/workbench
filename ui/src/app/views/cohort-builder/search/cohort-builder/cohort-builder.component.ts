import { Component, OnInit, ChangeDetectorRef, OnDestroy, Inject } from '@angular/core';
import { Router } from '@angular/router';
import {DOCUMENT} from '@angular/platform-browser';
import { SearchGroup, Subject, SearchResult } from '../model';
import 'rxjs/add/operator/takeWhile';
import { BroadcastService } from '../service/broadcast.service';
import { intersection, complement, union } from 'set-manipulator';

@Component({
  selector: 'app-search',
  templateUrl: 'cohort-builder.component.html',
  styleUrls: ['cohort-builder.component.css']
})
export class CohortBuilderComponent implements OnInit, OnDestroy {

  /**
   * The search groups that make up the inclusion group.
   *
   * @public
   * @type {SearchGroup[]}
   * @memberof CohortBuilderComponent
   */
  public searchGroups: SearchGroup[];

  /**
   * The search groups that make up the exclusion group.
   *
   * @public
   * @type {SearchGroup[]}
   * @memberof CohortBuilderComponent
   */
  public exclusionGroups: SearchGroup[];

  private alive = true;

  totalSet: Subject[] = [];

  constructor(private changeDetectorRef: ChangeDetectorRef,
              private broadcastService: BroadcastService,
              private router: Router,
              @Inject(DOCUMENT) private document: any) {}

  ngOnInit() {
    this.searchGroups = [new SearchGroup()];
    this.exclusionGroups = [new SearchGroup('Exclude participants where:')];

    this.broadcastService.updatedCounts$
      .takeWhile(() => this.alive)
      .subscribe(change => {
        this.updateGroupSet(change.searchGroup, change.searchResult);
        this.updateTotalSet();
        this.updateCharts();
      });
    this.broadcastService.removedSearchResult$
      .takeWhile(() => this.alive)
      .subscribe(change => {
        this.updateGroupSetRemovedSearchResult(change.searchGroup);
        this.updateTotalSet();
        this.updateCharts();
      });
  }

  addSearchGroup() {
    this.searchGroups.push(new SearchGroup());
    this.changeDetectorRef.detectChanges();
    const scrollableDiv = window.document.getElementById('scrollable-groups');
    scrollableDiv.scrollTop = scrollableDiv.scrollHeight;
  }

  addExclusionGroup() {
    this.exclusionGroups.push(new SearchGroup('Exclude participants where:'));
    this.changeDetectorRef.detectChanges();
    const scrollableDiv = window.document.getElementById('scrollable-groups');
    scrollableDiv.scrollTop = scrollableDiv.scrollHeight;
  }

  removeSearchGroup(searchGroup: SearchGroup) {
    const index: number = this.searchGroups.indexOf(searchGroup);
    if (index !== -1) {
      this.searchGroups.splice(index, 1);
    }
    this.changeDetectorRef.detectChanges();
    this.updateTotalSet();
    this.updateCharts();
  }

  removeExclusionGroup(searchGroup: SearchGroup) {
    const index: number = this.exclusionGroups.indexOf(searchGroup);
    if (index !== -1) {
      this.exclusionGroups.splice(index, 1);
    }
    this.changeDetectorRef.detectChanges();
    this.updateTotalSet();
    this.updateCharts();
  }

  private updateGroupSet(searchGroup: SearchGroup, searchResult: SearchResult) {
    if (searchGroup.groupSet.length === 0) {
      searchGroup.groupSet = searchResult.resultSet;
    } else {
      searchGroup.groupSet = union(searchGroup.groupSet,
          searchResult.resultSet,
          (object) => object.val);
    }
  }

  private updateGroupSetRemovedSearchResult(searchGroup: SearchGroup) {
    searchGroup.groupSet = [];
    searchGroup.results.forEach((result, index) => {
      if (index === 0) {
        searchGroup.groupSet = result.resultSet;
      } else if (searchGroup.groupSet.length !== 0) {
        searchGroup.groupSet = union(searchGroup.groupSet,
            result.resultSet,
            (object) => object.val);
      }
    });
  }

  /* Update any changes to the overall set. */
  private updateTotalSet() {
    const includedSets = this.performIntersection(this.searchGroups);

    const excludedSets = this.performIntersection(this.exclusionGroups);

    this.totalSet = includedSets;
    if (excludedSets.length > 0) {
      this.totalSet = complement(includedSets,
          excludedSets,
          (object) => object.val);
    }
  }

  private performIntersection(groups: SearchGroup[]) {
    let set = [];
    groups.forEach((group, index) => {
      if (index === 0) {
        set = group.groupSet;
      } else if (group.groupSet.length !== 0) {
        set = intersection(set,
            group.groupSet,
            (object) => object.val);
      }
    });
    return set;
  }

  private updateCharts() {
    let female = 0;
    let male = 0;
    let unknown = 0;
    let aa = 0;
    let ap = 0;
    let c = 0;
    let na = 0;
    let h = 0;
    let o = 0;
    let u = 0;
    this.totalSet.forEach((subject, index) => {
      const values = subject.val.split(',');
      if (values[1] === 'M') {
        male++;
      } else if (values[1] === 'F') {
        female++;
      } else {
        unknown++;
      }
      if (values[2] === 'B') {
        aa++;
      } else if (values[2] === 'A') {
        ap++;
      } else if (values[2] === 'W') {
        c++;
      } else if (values[2] === 'I') {
        na++;
      } else if (values[2] === 'H') {
        h++;
      } else if (values[2] === 'N') {
        o++;
      } else {
        u++;
      }
    });
    const genderData = [
      ['Gender', 'Count', { role: 'style' }],
      ['Female', female, 'blue'],
      ['Male', male, 'red'],
      ['Unknown', unknown, 'gray']];
    const raceData = [
      ['Race', 'Count Per'],
      ['African American', aa],
      ['Asian/Pacific', ap],
      ['Caucasian', c],
      ['Native American', na],
      ['Hispanic', h],
      ['Other', o],
      ['Unknown', u]];
    this.broadcastService.updateCharts(genderData, raceData);
  }

  ngOnDestroy() {
    this.alive = false;
  }

}
