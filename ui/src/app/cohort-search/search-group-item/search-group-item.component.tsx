import {Component, Input} from '@angular/core';
import * as React from 'react';

import {initExisting, searchRequestStore, selectionsStore, wizardStore} from 'app/cohort-search/search-state.service';
import {attributeDisplay, domainToTitle, mapGroupItem, nameDisplay, typeDisplay} from 'app/cohort-search/utils';
import {Button, Clickable} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {CriteriaType, DomainType, SearchRequest} from 'generated/fetch';
import {Menu} from 'primereact/menu';
import {OverlayPanel} from 'primereact/overlaypanel';
import Timeout = NodeJS.Timeout;

const styles = reactStyles({
  menu: {
    minWidth: '5rem',
    maxWidth: '15rem',
    width: 'auto'
  },
  lineItem: {
    minWidth: 0,
    flex: 4,
    overflow: 'hidden',
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis'
  },
  codeText: {
    fontWeight: 300,
    color: colors.dark
  },
  link: {
    height: 'auto',
    minWidth: '2rem',
    marginLeft: '0.25rem',
    padding: 0,
    fontSize: '12px',
    fontWeight: 600
  }
});

const itemStyles = `
  body .p-menu .p-menu-list {
    padding: 0.5rem 0;
  }
  body .p-menu .p-menuitem-link {
   padding: 0 1rem;
   line-height: 1.25rem;
  }
  .item-title:hover {
    cursor: pointer;
    color: ${colors.accent}!important
  }
`;

interface Props {
  role: keyof SearchRequest;
  groupId: string;
  item: any;
  index: number;
  updateGroup: Function;
}

interface State {
  count: number;
  error: boolean;
  loading: boolean;
  status: string;
  timeout: Timeout;
}

export class SearchGroupItem extends React.Component<Props, State> {
  dropdown: any;
  op: any;
  constructor(props: Props) {
    super(props);
    this.state = {count: null, error: false, loading: true, status: props.item.status, timeout: null};
  }

  componentDidMount(): void {
    this.getItemCount();
  }

  getItemCount() {
    const {index, item, role, updateGroup} = this.props;
    // prevent multiple group count calls when initializing multiple items simultaneously
    // (on cohort edit or clone)
    const init = initExisting.getValue();
    if (!init || (init && index === 0)) {
      updateGroup();
    }
    try {
      const {cdrVersionId} = currentWorkspaceStore.getValue();
      const mappedItem = mapGroupItem(item, false);
      const request = {
        includes: [],
        excludes: [],
        [role]: [{items: [mappedItem], temporal: false}]
      };
      cohortBuilderApi().countParticipants(+cdrVersionId, request).then(count => {
        this.setState({count, loading: false});
      }, (err) => {
        console.error(err);
        this.setState({error: true, loading: false});
      });
    } catch (error) {
      console.error(error);
      this.setState({error: true, loading: false});
    }
  }

  get codeTypeDisplay() {
    const {item: {type}} = this.props;
    return `${domainToTitle(type)} ${this.pluralizedCode}`;
  }

  get pluralizedCode() {
    const {item: {searchParameters}} = this.props;
    return searchParameters.length > 1 ? 'Codes' : 'Code';
  }

  get codes() {
    const {item: {searchParameters, type}} = this.props;
    const formatter = (param) => {
      let funcs = [typeDisplay, attributeDisplay];
      if (type === DomainType.PERSON) {
        funcs = [typeDisplay, nameDisplay, attributeDisplay];
      } else if (type === DomainType.PHYSICALMEASUREMENT
        || type === DomainType.VISIT
        || type === DomainType.DRUG
        || type === DomainType.MEASUREMENT
        || type === DomainType.SURVEY) {
        funcs = [nameDisplay];
      }
      return funcs.map(f => f(param)).join(' ').trim();
    };
    const sep = type === DomainType[DomainType.PERSON] ? '; ' : ', ';
    return searchParameters.map(formatter).join(sep);
  }

  enable() {
    triggerEvent('Enable', 'Click', 'Enable - Suppress Criteria - Cohort Builder');
    this.setState({status: 'active'});
    this.props.item.status = 'active';
    this.updateSearchRequest();
  }

  suppress() {
    triggerEvent('Suppress', 'Click', 'Snowman - Suppress Criteria - Cohort Builder');
    this.setState({status: 'hidden'});
    this.props.item.status = 'hidden';
    this.updateSearchRequest();
  }

  remove() {
    triggerEvent('Delete', 'Click', 'Snowman - Delete Criteria - Cohort Builder');
    this.setState({status: 'pending'});
    this.props.item.status = 'pending';
    this.updateSearchRequest();
    const timeout = setTimeout(() => {
      this.updateSearchRequest(true);
    }, 10000);
    this.setState({timeout});
  }

  undo() {
    triggerEvent('Undo', 'Click', 'Undo - Delete Criteria - Cohort Builder');
    clearTimeout(this.state.timeout);
    this.setState({status: 'active'});
    this.props.item.status = 'active';
    this.updateSearchRequest();
  }

  updateSearchRequest(remove?: boolean) {
    const sr = searchRequestStore.getValue();
    const {item, groupId, role, updateGroup} = this.props;
    const groupIndex = sr[role].findIndex(grp => grp.id === groupId);
    if (groupIndex > -1) {
      const itemIndex = sr[role][groupIndex].items.findIndex(it => it.id === item.id);
      if (itemIndex > -1) {
        if (remove) {
          sr[role][groupIndex].items = sr[role][groupIndex].items.filter(it => it.id !== item.id);
          searchRequestStore.next(sr);
        } else {
          sr[role][groupIndex].items[itemIndex] = item;
          searchRequestStore.next(sr);
          updateGroup();
        }
      }
    }
  }

  get typeAndStandard() {
    const {item: {searchParameters, type}} = this.props;
    switch (type) {
      case DomainType.PERSON:
        const _type = searchParameters[0].type === CriteriaType.DECEASED
          ? CriteriaType.AGE : searchParameters[0].type;
        return {_type, standard: false};
      case DomainType.PHYSICALMEASUREMENT:
        return {type: searchParameters[0].type, standard: false};
      case DomainType.SURVEY:
        return {type: searchParameters[0].type, standard: false};
      case DomainType.VISIT:
        return {type: searchParameters[0].type, standard: true};
      default:
        return {type: null, standard: null};
    }
  }

  launchWizard() {
    triggerEvent('Edit', 'Click', 'Snowman - Edit Criteria - Cohort Builder');
    const {groupId, item, item: {fullTree, searchParameters}, role} = this.props;
    const selections = searchParameters.map(sp => sp.parameterId);
    selectionsStore.next(selections);
    const _item = JSON.parse(JSON.stringify(item));
    const itemId = _item.id;
    const domain = _item.type;
    let isStandard;
    if ([DomainType.CONDITION, DomainType.PROCEDURE].includes(domain)) {
      isStandard = item.searchParameters[0].isStandard;
    }
    const {type, standard} = this.typeAndStandard;
    const context = {item: _item, domain, type, isStandard, role, groupId, itemId, fullTree, standard};
    wizardStore.next(context);
  }

  render() {
    const {count, error, loading, status} = this.state;
    const showCount = !loading && status !== 'hidden' && count !== null;
    const items = [
      {label: 'Edit criteria', command: () => this.launchWizard()},
      {label: 'Suppress criteria from total count', command: () => this.suppress()},
      {label: 'Delete criteria', command: () => this.remove()},
    ];
    return <React.Fragment>
      <style>{itemStyles}</style>
      {status !== 'deleted' && <div style={{display: 'flex', fontSize: '12px'}}>
        {(status === 'active' || !status) && <div style={styles.lineItem}>
          <Menu style={styles.menu} appendTo={document.body} model={items} popup={true} ref={el => this.dropdown = el} />
          <Clickable style={{display: 'inline-block', paddingRight: '0.5rem'}} onClick={(event) => this.dropdown.toggle(event)}>
            <ClrIcon shape='ellipsis-vertical' />
          </Clickable>
          <span style={{paddingRight: '10px'}}
                onClick={() => this.launchWizard()}
                onMouseEnter={(e) => this.op.toggle(e)}
                onMouseLeave={(e) => this.op.toggle(e)}>
            <span className='item-title' style={styles.codeText}>Contains {this.codeTypeDisplay}</span>
          </span>
          <OverlayPanel ref={(el) => this.op = el} appendTo={document.body} style={{maxWidth: '15rem'}}>
            <h3 style={{margin: 0}}>{this.codeTypeDisplay}</h3>
            {this.codes}
          </OverlayPanel>
          {status !== 'hidden' && <span style={{...styles.codeText, paddingRight: '10px'}}>|</span>}
          {loading && <span className='spinner spinner-inline'>Loading...</span>}
          {showCount && <span style={styles.codeText}>{count.toLocaleString()}</span>}
          {error && <span><ClrIcon style={{color: colors.warning}} shape='exclamation-triangle' className='is-solid' size={22} /></span>}
        </div>}
        {status === 'pending' && <div style={{color: colorWithWhiteness(colors.warning, -.35)}}>
          <ClrIcon shape='exclamation-triangle' className='is-solid' size={23} />
          <span style={{margin: '0 0 2px 2px'}}>
            This criteria has been deleted
            <Button type='link' style={styles.link} onClick={() => this.undo()}>UNDO</Button>
          </span>
        </div>}
        {status === 'hidden' && <div style={{color: colorWithWhiteness(colors.warning, -.35)}}>
          <ClrIcon shape='eye-hide' className='is-solid' size={23} />
          <span style={{margin: '0 0 2px 2px'}}>
            This criteria has been suppressed
            <Button type='link' style={styles.link} onClick={() => this.enable()}>ENABLE</Button>
          </span>
        </div>}
      </div>}
    </React.Fragment>;
  }
}

@Component({
  selector: 'app-list-search-group-item',
  template: '<div #root></div>'
})
export class SearchGroupItemComponent extends ReactWrapperBase {
  @Input('role') role: Props['role'];
  @Input('groupId') groupId: Props['groupId'];
  @Input('item') item: Props['item'];
  @Input('index') index: Props['index'];
  @Input('updateGroup') updateGroup: Props['updateGroup'];

  constructor() {
    super(SearchGroupItem, ['role', 'groupId', 'item', 'index', 'updateGroup']);
  }
}
