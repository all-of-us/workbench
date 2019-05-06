import {Component, Input} from '@angular/core';

import {DomainType, SearchRequest, TreeSubType} from 'generated';

import {selectionsStore, wizardStore} from 'app/cohort-search/search-state.service';
import {domainToTitle, getCodeOptions, listAttributeDisplay, listNameDisplay, listTypeDisplay} from 'app/cohort-search/utils';

@Component({
  selector: 'app-list-search-group-item',
  templateUrl: './list-search-group-item.component.html',
  styleUrls: ['./list-search-group-item.component.css'],
})
export class ListSearchGroupItemComponent {
  @Input() role: keyof SearchRequest;
  @Input() groupId: string;
  @Input() item: any;
  @Input() delete: Function;

  error: boolean;

  get codeType() {
    return domainToTitle(this.item.type);
  }

  get codeTypeDisplay() {
    return `${this.codeType} ${this.pluralizedCode}`;
  }

  get pluralizedCode() {
    return this.parameters.length > 1 ? 'Codes' : 'Code';
  }

  get isRequesting() {
    return this.item.isRequesting;
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
      if (_type === DomainType[DomainType.PERSON]) {
        funcs = [listTypeDisplay, listNameDisplay, listAttributeDisplay];
      } else if (_type === DomainType[DomainType.PHYSICALMEASUREMENT]
        || _type === DomainType[DomainType.VISIT]
        || _type === DomainType[DomainType.DRUG]
        || _type === DomainType[DomainType.MEASUREMENT]
        || _type === DomainType[DomainType.SURVEY]) {
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

  get itemSubtype() {
    if (this.item.type === DomainType.PERSON) {
      const subtype = this.parameters[0].subtype;
      return subtype === TreeSubType.DEC ? TreeSubType.AGE : subtype;
    }
    return null;
  }

  launchWizard() {
    const selections = this.item.searchParameters.map(sp => sp.paramId);
    selectionsStore.next(selections);
    const codes = getCodeOptions(this.item.type);
    const fullTree = this.item.fullTree;
    const {role, groupId} = this;
    const itemId = this.item.id;
    const domain = this.item.type;
    const type = this.itemSubtype;
    const context = {item: this.item, domain, type, role, groupId, itemId, fullTree, codes};
    wizardStore.next(context);
  }
}
