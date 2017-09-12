import {Component, OnDestroy, Output, EventEmitter, OnInit, ChangeDetectorRef} from '@angular/core';
import {SearchService} from '../service';
import {Modifier, Criteria} from '../model';
import 'rxjs/add/operator/takeWhile';
import { BroadcastService } from '../service/broadcast.service';

@Component({
  selector: 'app-criteria-group',
  templateUrl: 'criteria-group.component.html',
  styleUrls: ['criteria-group.component.css']
})
export class CriteriaGroupComponent implements OnInit, OnDestroy {

  @Output()
  onRemove = new EventEmitter<Criteria>();
  criteriaList: Criteria[] = [];
  modifierList: Modifier[] = [];
  totalCount: number;

  ageAtEventSelectList: string[] = [];
  eventDateSelectList: string[] = [];
  hasOccurrencesSelectList: string[] = [];
  eventOccurredDuringSelectList: string[] = [];
  daysOrYearsSelectList: string[] = [];
  private alive = true;
  ageAtEvent: Modifier;
  eventDate: Modifier;
  hasOccurrences: Modifier;

  constructor(private changeDetectorRef: ChangeDetectorRef,
              private searchService: SearchService,
              private broadcastService: BroadcastService) {}

  ngOnInit(): void {
    this.broadcastService.selectedCriteria$
      .takeWhile(() => this.alive)
      .subscribe(criteria => {
        this.updateCriteriaList(criteria);
        this.updateCriteriaGroupView();
      });

    this.ageAtEventSelectList = this.searchService.getAgeAtEventSelectList();

    this.eventDateSelectList = this.searchService.getEventDateSelectList();

    this.hasOccurrencesSelectList = this.searchService.getHasOccurrencesSelectList();
    this.daysOrYearsSelectList = this.searchService.getDaysOrYearsSelectList();

    // this.eventOccurredDuringSelectList = this.searchService.getEventOccurredDuringSelectList();

    if (this.modifierList.length === 0) {
      this.ageAtEvent = new Modifier();
      this.ageAtEvent.operator = this.ageAtEventSelectList[0];
      this.ageAtEvent.name = 'ageAtEvent';
      this.modifierList.push(this.ageAtEvent);

      this.eventDate = new Modifier();
      this.eventDate.operator = this.eventDateSelectList[0];
      this.eventDate.name = 'eventDate';
      this.modifierList.push(this.eventDate);

      this.hasOccurrences = new Modifier();
      this.hasOccurrences.operator = this.hasOccurrencesSelectList[0];
      this.hasOccurrences.value3 = this.daysOrYearsSelectList[0];
      this.hasOccurrences.name = 'hasOccurrences';
      this.modifierList.push(this.hasOccurrences);
    } else {
      this.ageAtEvent = this.modifierList[0];
      this.eventDate = this.modifierList[1];
      this.hasOccurrences = this.modifierList[2];
    }
  }

  private updateCriteriaList(criteria: Criteria) {
    const index: number = this.criteriaList.indexOf(criteria);
    if (index !== -1) {
      this.criteriaList[index] = criteria;
    } else {
      this.criteriaList.push(criteria);
    }
  }

  private updateCriteriaGroupView() {
    this.changeDetectorRef.detectChanges();
    const scrollableDiv = window.document.getElementById('scrollable-criteria-group');
    scrollableDiv.scrollTop = scrollableDiv.scrollHeight;
  }

  updateTotal() {
  }

  removeCriteria(criteria: Criteria) {
    this.onRemove.emit(criteria);
  }

  ngOnDestroy() {
    this.alive = false;
  }
}
