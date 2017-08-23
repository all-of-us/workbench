import { Component, OnInit, ViewChild } from '@angular/core';
import { Modifier } from '../model';
import { SearchService } from '../service';
import {Wizard} from 'clarity-angular/wizard/wizard';

@Component({
  selector: 'app-wizard-modal',
  templateUrl: './wizard-modal.component.html',
  styleUrls: ['./wizard-modal.component.css']
})
export class WizardModalComponent implements OnInit {

  @ViewChild('wizard') wizard: Wizard;
  modifierList: Modifier[] = [];
  ageAtEventSelectList: string[] = [];
  eventDateSelectList: string[] = [];
  hasOccurrencesSelectList: string[] = [];
  eventOccurredDuringSelectList: string[] = [];
  daysOrYearsSelectList: string[] = [];
  private alive = true;
  ageAtEvent: Modifier;
  eventDate: Modifier;
  hasOccurrences: Modifier;
  criteriaType: string;

  constructor(private searchService: SearchService) { }

  ngOnInit() {
    this.ageAtEventSelectList = this.searchService.getAgeAtEventSelectList();

    this.eventDateSelectList = this.searchService.getEventDateSelectList();

    this.hasOccurrencesSelectList = this.searchService.getHasOccurrencesSelectList();
    this.daysOrYearsSelectList = this.searchService.getDaysOrYearsSelectList();

    this.eventOccurredDuringSelectList = this.searchService.getEventOccurredDuringSelectList();

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

}
