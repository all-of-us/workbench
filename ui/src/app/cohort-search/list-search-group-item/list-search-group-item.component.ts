import {Component, Input, OnChanges, OnInit, SimpleChanges} from '@angular/core';

import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import {CriteriaType, DomainType, SearchRequest} from 'generated/fetch';

import {selectionsStore, wizardStore} from 'app/cohort-search/search-state.service';
import {domainToTitle, getCodeOptions, listAttributeDisplay, listNameDisplay, listTypeDisplay} from 'app/cohort-search/utils';
import {currentWorkspaceStore} from 'app/utils/navigation';

@Component({
  selector: 'app-list-search-group-item',
  templateUrl: './list-search-group-item.component.html',
  styleUrls: ['./list-search-group-item.component.css'],
})
export class ListSearchGroupItemComponent implements OnChanges, OnInit {
  @Input() role: keyof SearchRequest;
  @Input() groupId: string;
  @Input() item: any;
  @Input() delete: Function;

  count: number;
  error = false;
  loading = true;

  ngOnInit(): void {
    this.getItemCount();
  }

  ngOnChanges(changes: SimpleChanges): void {
    console.log(changes);
    if (changes.item && !changes.item.firstChange) {
      this.getItemCount();
    }
  }

  getItemCount() {
    try {
      const {cdrVersionId} = currentWorkspaceStore.getValue();
      const request = <SearchRequest>{
        includes: [],
        excludes: [],
        [this.role]: [{items: [this.item]}]
      };
      cohortBuilderApi().countParticipants(+cdrVersionId, request).then(count => {
        this.count = count;
        this.loading = false;
      }, (err) => {
        console.error(err);
        this.error = true;
        this.loading = false;
      });
    } catch (error) {
      console.error(error);
      this.error = true;
      this.loading = false;
    }
  }

  get codeType() {
    return domainToTitle(this.item.type);
  }

  get codeTypeDisplay() {
    return `${this.codeType} ${this.pluralizedCode}`;
  }

  get pluralizedCode() {
    return this.parameters.length > 1 ? 'Codes' : 'Code';
  }

  get status() {
    // TODO get actual status from item
    return 'active';
  }

  get parameters() {
    return this.item.searchParameters;
  }

  get codes() {
    const _type = this.item.type;
    const formatter = (param) => {
      let funcs = [listTypeDisplay, listAttributeDisplay];
      if (_type === DomainType.PERSON) {
        funcs = [listTypeDisplay, listNameDisplay, listAttributeDisplay];
      } else if (_type === DomainType.PHYSICALMEASUREMENT
        || _type === DomainType.VISIT
        || _type === DomainType.DRUG
        || _type === DomainType.MEASUREMENT
        || _type === DomainType.SURVEY) {
        funcs = [listNameDisplay];
      }
      return funcs.map(f => f(param)).join(' ').trim();
    };
    const sep = _type === DomainType[DomainType.PERSON] ? '; ' : ', ';
    return this.parameters.map(formatter).join(sep);
  }

  remove() {
    this.setStatus('pending');
    this.item.timeout = setTimeout(() => {
      this.delete(this.item.id);
    }, 10000);
  }

  setStatus(status: string) {
    this.item.status = status;
  }

  undo() {
    clearTimeout(this.item.timeout);
    this.setStatus('active');
  }

  get typeAndStandard() {
    switch (this.item.type) {
      case DomainType.PERSON:
        const type = this.parameters[0].type === CriteriaType.DECEASED
          ? CriteriaType.AGE : this.parameters[0].type;
        return {type, standard: false};
      case DomainType.PHYSICALMEASUREMENT:
        return {type: this.parameters[0].type, standard: false};
      case DomainType.VISIT:
        return {type: this.parameters[0].type, standard: true};
      default:
        return {type: null, standard: null};
    }
  }

  launchWizard() {
    const selections = this.item.searchParameters.map(sp => sp.parameterId);
    selectionsStore.next(selections);
    const codes = getCodeOptions(this.item.type);
    const fullTree = this.item.fullTree;
    const {role, groupId} = this;
    const item = JSON.parse(JSON.stringify(this.item));
    const itemId = this.item.id;
    const domain = this.item.type;
    const {type, standard} = this.typeAndStandard;
    const context = {item, domain, type, standard, role, groupId, itemId, fullTree, codes};
    wizardStore.next(context);
  }
}
