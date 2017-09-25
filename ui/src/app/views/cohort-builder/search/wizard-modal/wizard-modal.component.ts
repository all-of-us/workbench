import {
  Component, ComponentRef,
  OnDestroy, OnInit,
  ViewChild, ViewEncapsulation
} from '@angular/core';
import {Subscription} from 'rxjs/Subscription';
import {Wizard} from 'clarity-angular/wizard/wizard';

import {BroadcastService} from '../broadcast.service';
import {SearchGroup, SearchResult} from '../model';

import {CohortBuilderService, Criteria, Modifier} from 'generated';


@Component({
  selector: 'app-wizard-modal',
  templateUrl: './wizard-modal.component.html',
  styleUrls: ['./wizard-modal.component.css'],
  encapsulation: ViewEncapsulation.None
})
export class WizardModalComponent implements OnInit, OnDestroy {

  @ViewChild('wizard') wizard: Wizard;
  private selectedSearchGroup: SearchGroup;
  private selectedSearchResult: SearchResult;
  private criteriaList: Criteria[] = [];
  private modifierList: Modifier[] = [];
  criteriaType: string;
  wizardModalRef: ComponentRef<WizardModalComponent>;
  private criteriaTypeSubscription: Subscription;
  private searchGroupSubscription: Subscription;
  private criteriaListSubscription: Subscription;
  private modifierListSubscription: Subscription;

  constructor(private broadcastService: BroadcastService,
              private cohortBuilderService: CohortBuilderService) { }

  ngOnInit() {
    this.criteriaTypeSubscription = this.broadcastService.selectedCriteriaType$
      .subscribe(criteriaType => this.criteriaType = criteriaType);
    this.searchGroupSubscription = this.broadcastService.selectedSearchGroup$
      .subscribe(searchGroup => this.selectedSearchGroup = searchGroup);
    this.criteriaListSubscription = this.broadcastService.summaryCriteriaGroup$
      .subscribe(criteriaList => this.criteriaList = criteriaList);
    this.modifierListSubscription = this.broadcastService.summaryModifierList$
      .subscribe(modifierList => this.modifierList = modifierList);
  }

  public doCustomClick(type: string): void {
    if ('finish' === type) {
      this.wizard.finish();
      this.updateSearchResults();
    }
  }

  public doCancel() {
    this.wizard.finish();
    this.wizardModalRef.destroy();
  }

  updateSearchResults() {
    this.updateOrCreateSearchResult();

    this.searchGroupSubscription = this.cohortBuilderService
      .searchSubjects({
        type: this.criteriaType.toUpperCase(),
        searchParameters: this.selectedSearchResult.values,
        modifiers: this.selectedSearchResult.modifierList
      })
      .subscribe((response) => {
        this.selectedSearchResult.updateWithResponse(response);
        this.broadcastService.updateCounts(this.selectedSearchGroup, this.selectedSearchResult);
        this.wizardModalRef.destroy();
      });
  }

  updateOrCreateSearchResult() {
    if (this.selectedSearchResult) {
      this.selectedSearchResult.update(
          this.criteriaType.toUpperCase() + ' Group', this.criteriaList, this.modifierList);
    } else {
      this.selectedSearchResult = new SearchResult(
          this.criteriaType.toUpperCase() + ' Group', this.criteriaList, this.modifierList);
      this.selectedSearchGroup.results.push(this.selectedSearchResult);
    }
  }

  ngOnDestroy() {
    this.criteriaTypeSubscription.unsubscribe();
    this.searchGroupSubscription.unsubscribe();
    this.criteriaListSubscription.unsubscribe();
    this.modifierListSubscription.unsubscribe();
  }
}
