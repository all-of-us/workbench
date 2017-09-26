import {Component, OnInit, ChangeDetectorRef, OnDestroy, Inject} from '@angular/core';
import {Router, ActivatedRoute} from '@angular/router';
import {DOCUMENT} from '@angular/platform-browser';
import {intersection, complement, union} from 'set-manipulator';
import 'rxjs/add/operator/takeWhile';

import {BroadcastService} from '../broadcast.service';
import {SearchGroup, SearchResult} from '../model';

import {Subject} from 'generated';

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

  adding = false;

  constructor(private changeDetectorRef: ChangeDetectorRef,
              private broadcastService: BroadcastService,
              private router: Router,
              private route: ActivatedRoute,
              @Inject(DOCUMENT) private document: any) {}

  ngOnInit() {
    if (this.route.snapshot.url[5] === undefined) {
      this.adding = true;
    }

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

  save(): void {
    if (this.adding) {
      this.router.navigate(['../create'], {relativeTo : this.route});
    } else {
      this.router.navigate(['../edit'], {relativeTo : this.route});
    }
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
    /*
     * TODO:
     * Determine exactly how and what data is to be bundled with each Subject
     * and whether decoding that data should take place in browser or in SQL.
     * Until the exact encoding is finalized, this hack should generate some
     * nice charts for a visual response to searches.
     */
    const genders = {
      'Male': 0,
      'Female': 0,
      'Unknown': 0,
    };
    const races = {
      'African American': 0,
      'Asian/Pacific': 0,
      'Caucasian': 0,
      'Native American': 0,
      'Hispanic': 0,
      'Other': 0,
      'Unknown': 0,
    };

    this.totalSet.forEach((subject, index) => {
      const [uid, gender, race] = subject.val.split(',');
      switch (gender) {
          case '1': genders.Male++    ; break;
          case '2': genders.Female++  ; break;
          default : genders.Unknown++;
      }
      switch (race) {
          case '1': races.Caucasian++           ; break;
          case '2': races['African American']++ ; break;
          default : races.Unknown++;
      }
    });

    const genderData = [
      ['Gender', 'Count', { role: 'style' }],
      ['Female', genders.Female, 'blue'],
      ['Male', genders.Male, 'red'],
      ['Unknown', genders.Unknown, 'gray']];

    // ethnicity_source_value
    // '5': Hispanic or Latino: 38003563
    // '1', '2', '3': Not Hispanic: 38003564
    //
    // race_source_value:
    // '2': 8516: "black or african american" j
    // '1': 8527: "white"
    const raceData = [
      ['Race', 'Count Per'],
      ['African American', races['African American']],
      ['Caucasian', races.Caucasian],
      ['Unknown', races.Unknown]];

    this.broadcastService.updateCharts(genderData, raceData);
  }

  ngOnDestroy() {
    this.alive = false;
  }
}
