import {Component, Input} from '@angular/core';
import * as React from 'react';

import {ENCOUNTERS_MAP, MODIFIERS_TEXT} from 'app/cohort-search/constant';
import {initExisting, searchRequestStore, selectionsStore, wizardStore} from 'app/cohort-search/search-state.service';
import {domainToTitle, getTypeAndStandard, mapGroupItem} from 'app/cohort-search/utils';
import {Button, Clickable} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {DomainType, Modifier, ModifierType, SearchRequest} from 'generated/fetch';
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
  },
  parameter: {
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap'
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

function modifiersDisplay(mod: Modifier) {
  const {name, operands, operator} = mod;
  switch (name) {
    case ModifierType.AGEATEVENT:
      return <span><b>{MODIFIERS_TEXT[name].name}</b> {MODIFIERS_TEXT[name].operators[operator]} {operands.join(' and ')}</span>;
    case ModifierType.ENCOUNTERS:
      return <span><b>{MODIFIERS_TEXT[name].name}</b> {ENCOUNTERS_MAP[operands[0]]}</span>;
    case ModifierType.EVENTDATE:
      return <span><b>{MODIFIERS_TEXT[name].name}</b> {MODIFIERS_TEXT[name].operators[operator]} {operands.join(' and ')}</span>;
    case ModifierType.NUMOFOCCURRENCES:
      return <span><b>{MODIFIERS_TEXT[name].name}</b> {operands[0]} Or More</span>;
  }
}

export class SearchGroupItemName extends React.Component<{editItem: Function, item: any}> {
  op: any;
  constructor(props: any) {
    super(props);
  }

  render() {
    const {editItem, item: {modifiers, searchParameters, type}} = this.props;
    const codeDisplay = searchParameters.length > 1 ? 'Codes' : 'Code';
    const showCode = [DomainType.CONDITION, DomainType.DRUG, DomainType.MEASUREMENT, DomainType.PROCEDURE].includes(type);
    return <React.Fragment>
      <span style={{paddingRight: '10px'}}
        onClick={() => editItem()}
        onMouseEnter={(e) => this.op.show(e)}
        onMouseLeave={() => this.op.hide()}>
        <span className='item-title' style={styles.codeText}>Contains {domainToTitle(type)} {codeDisplay}</span>
      </span>
      <OverlayPanel ref={(el) => this.op = el} appendTo={document.body} style={{maxWidth: '30%'}}>
        <h3 style={{margin: 0}}>{domainToTitle(type)}</h3>
        {searchParameters.map((param, p) => <div key={p} style={styles.parameter}>{showCode && <b>{param.code}</b>} {param.name}</div>)}
        {!!modifiers.length && <React.Fragment>
          <h3>Modifiers</h3>
          {modifiers.map((mod, m) => <div key={m}>{modifiersDisplay(mod)}</div>)}
        </React.Fragment>}
      </OverlayPanel>
    </React.Fragment>;
  }
}

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
  constructor(props: Props) {
    super(props);
    this.state = {count: null, error: false, loading: true, status: props.item.status, timeout: null};
  }

  componentDidMount(): void {
    this.getItemCount();
  }

  async getItemCount() {
    const {index, item, role, updateGroup} = this.props;
    // prevent multiple group count calls when initializing multiple items simultaneously (on cohort edit or clone)
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
      await cohortBuilderApi().countParticipants(+cdrVersionId, request).then(count => this.setState({count}));
    } catch (error) {
      console.error(error);
      this.setState({error: true});
    } finally {
      this.setState({loading: false});
    }
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

  launchWizard() {
    triggerEvent('Edit', 'Click', 'Snowman - Edit Criteria - Cohort Builder');
    const {groupId, item, role} = this.props;
    const _item = JSON.parse(JSON.stringify(item));
    const {fullTree, id, searchParameters} = _item;
    const selections = searchParameters.map(sp => sp.parameterId);
    selectionsStore.next(selections);
    const domain = _item.type;
    const {type, standard} = getTypeAndStandard(searchParameters, domain);
    const context = {item: _item, domain, type, role, groupId, itemId: id, fullTree, standard};
    wizardStore.next(context);
  }

  render() {
    const {item} = this.props;
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
          <SearchGroupItemName editItem={() => this.launchWizard()} item={item} />
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
