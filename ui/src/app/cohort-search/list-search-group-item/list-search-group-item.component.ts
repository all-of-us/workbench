import {Component, Input, OnInit} from '@angular/core';

import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import {CriteriaType, DomainType, SearchRequest} from 'generated/fetch';

import {
  searchRequestStore,
  selectionsStore,
  wizardStore
} from 'app/cohort-search/search-state.service';
import {domainToTitle, getCodeOptions, listAttributeDisplay, listNameDisplay, listTypeDisplay, mapGroupItem} from 'app/cohort-search/utils';
import {currentWorkspaceStore} from 'app/utils/navigation';

@Component({
  selector: 'app-list-search-group-item',
  templateUrl: './list-search-group-item.component.html',
  styleUrls: ['./list-search-group-item.component.css'],
})
export class ListSearchGroupItemComponent implements OnInit {
  @Input() role: keyof SearchRequest;
  @Input() groupId: string;
  @Input() item: any;
  @Input() updateGroup: Function;

  count: number;
  error = false;
  loading = true;

  ngOnInit(): void {
    this.getItemCount();
  }

  getItemCount() {
    this.updateGroup();
    try {
      const {cdrVersionId} = currentWorkspaceStore.getValue();
      const item = mapGroupItem(this.item);
      const request = <SearchRequest>{
        includes: [],
        excludes: [],
        [this.role]: [{items: [item], temporal: false}]
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
    return this.item.status;
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

  enable() {
    this.item.status = 'active';
    this.updateSearchRequest();
  }

  suppress() {
    this.item.status = 'hidden';
    this.updateSearchRequest();
  }

  remove() {
    this.item.status = 'pending';
    this.updateSearchRequest();
    this.item.timeout = setTimeout(() => {
      this.updateSearchRequest(true);
    }, 10000);
  }

  undo() {
    clearTimeout(this.item.timeout);
    this.item.status = 'active';
  }

  updateSearchRequest(remove?: boolean) {
    const sr = searchRequestStore.getValue();
    const {item, groupId, role} = this;
    const groupIndex = sr[role].findIndex(grp => grp.id === groupId);
    if (groupIndex > -1) {
      const itemIndex = sr[role][groupIndex].items.findIndex(it => it.id === item.id);
      if (itemIndex > -1) {
        if (remove) {
          sr[role][groupIndex].items = sr[role][groupIndex].items.filter(it => it.id !== item.id);
        } else {
          sr[role][groupIndex].items[itemIndex] = item;
        }
        searchRequestStore.next(sr);
        this.updateGroup();
      }
    }
  }

  get typeAndStandard() {
    switch (this.item.type) {
      case DomainType.PERSON:
        const type = this.parameters[0].type === CriteriaType.DECEASED
          ? CriteriaType.AGE : this.parameters[0].type;
        return {type, standard: false};
      case DomainType.PHYSICALMEASUREMENT:
        return {type: this.parameters[0].type, standard: false};
      case DomainType.SURVEY:
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
