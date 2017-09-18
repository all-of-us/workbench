import {
  Component, OnDestroy, Output, EventEmitter, OnInit, ChangeDetectorRef,
  ViewEncapsulation, Input, ViewChild
} from '@angular/core';
import { BroadcastService } from '../service';
import { SearchCriteria } from '../model';
import { Subscription } from 'rxjs/Subscription';

@Component({
  selector: 'app-wizard-criteria-group',
  templateUrl: 'wizard-criteria-group.component.html',
  styleUrls: ['wizard-criteria-group.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class WizardCriteriaGroupComponent implements OnInit, OnDestroy {

  criteriaList: SearchCriteria[] = [];
  criteriaType: string;
  @ViewChild('groupDiv') groupDiv: any;
  private criteriaSubscription: Subscription;
  private criteriaTypeSubscription: Subscription;

  constructor(private changeDetectorRef: ChangeDetectorRef,
              private broadcastService: BroadcastService) {}

  ngOnInit(): void {
    this.criteriaSubscription = this.broadcastService.selectedCriteria$
      .subscribe(criteria => {
        this.updateCriteriaList(criteria);
        this.updateCriteriaGroupView();
      });
    this.criteriaTypeSubscription = this.broadcastService.selectedCriteriaType$
      .subscribe(criteriaType =>
        this.criteriaType = criteriaType);
  }

  public getTypeDescription(): string {
    if (this.criteriaType === 'icd9'
        || this.criteriaType === 'icd10'
        || this.criteriaType === 'cpt') {
      return this.criteriaType.toUpperCase() + ' Codes';
    }
    return this.criteriaType;
  }

  private updateCriteriaList(criteria: SearchCriteria) {
    const index: number = this.criteriaList.indexOf(criteria);
    if (index !== -1) {
      this.criteriaList[index] = criteria;
    } else {
      this.criteriaList.push(criteria);
    }
    this.broadcastService.setSummaryCriteriaGroup(this.criteriaList);
  }

  private updateCriteriaGroupView() {
    this.changeDetectorRef.detectChanges();
    this.groupDiv.scrollTop = this.groupDiv.scrollHeight;
  }

  removeCriteria(criteria: SearchCriteria) {
    const index: number = this.criteriaList.indexOf(criteria);
    if (index !== -1) {
      this.criteriaList.splice(index, 1);
    }
  }

  ngOnDestroy() {
    this.criteriaSubscription.unsubscribe();
    this.criteriaTypeSubscription.unsubscribe();
  }
}
