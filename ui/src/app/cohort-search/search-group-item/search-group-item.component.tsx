import {Component, Input} from '@angular/core';
import * as React from 'react';

import {MODIFIERS_MAP} from 'app/cohort-search/constant';
import {encountersStore, initExisting, searchRequestStore, selectionsStore, wizardStore} from 'app/cohort-search/search-state.service';
import {domainToTitle, getTypeAndStandard, mapGroupItem} from 'app/cohort-search/utils';
import {Button, Clickable} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {CriteriaType, DomainType, Modifier, ModifierType, SearchRequest} from 'generated/fetch';
import {Menu} from 'primereact/menu';
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
  caret: {
    cursor: 'pointer',
    float: 'right',
    transition: 'transform 0.2s ease-out',
  },
  parameterList: {
    height: 'auto',
    overflow: 'hidden',
    transition: 'max-height 0.4s ease-out'
  },
  parameter: {
    fontSize: '12px',
    color: colors.dark,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap'
  },
  viewMore: {
    color: colors.accent,
    cursor: 'pointer',
    fontSize: '12px',
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
  workspace: WorkspaceData;
}

interface State {
  count: number;
  encounters: any;
  error: boolean;
  loading: boolean;
  paramListOpen: boolean;
  status: string;
  timeout: Timeout;
}

export const SearchGroupItem = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    actionsMenu: any;
    constructor(props: Props) {
      super(props);
      this.state = {
        count: null,
        encounters: encountersStore.getValue(),
        paramListOpen: false,
        error: false,
        loading: true,
        status: props.item.status,
        timeout: null
      };
    }

    componentDidMount(): void {
      const {item: {modifiers}, workspace: {cdrVersionId}} = this.props;
      const {encounters} = this.state;
      this.getItemCount();
      if (!!modifiers && modifiers.some(mod => mod.name === ModifierType.ENCOUNTERS) && !encounters) {
        cohortBuilderApi().getCriteriaBy(+cdrVersionId, DomainType[DomainType.VISIT], CriteriaType[CriteriaType.VISIT]).then(res => {
          encountersStore.next(res.items);
          this.setState({encounters: res.items});
        });
      }
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

    modifiersDisplay(mod: Modifier) {
      const {name, operands, operator} = mod;
      switch (name) {
        case ModifierType.AGEATEVENT:
          return <span><b>{MODIFIERS_MAP[name].name}</b> {MODIFIERS_MAP[name].operators[operator]} {operands.join(' and ')}</span>;
        case ModifierType.ENCOUNTERS:
          const {encounters} = this.state;
          const visit = !!encounters ? encounters.find(en => en.conceptId.toString() === operands[0]).name : '';
          return <span><b>{MODIFIERS_MAP[name].name}</b> {visit}</span>;
        case ModifierType.EVENTDATE:
          return <span><b>{MODIFIERS_MAP[name].name}</b> {MODIFIERS_MAP[name].operators[operator]} {operands.join(' and ')}</span>;
        case ModifierType.NUMOFOCCURRENCES:
          return <span><b>{MODIFIERS_MAP[name].name}</b> {operands[0]} Or More</span>;
      }
    }

    render() {
      const {item: {modifiers, searchParameters, type}} = this.props;
      const {count, paramListOpen, error, loading, status} = this.state;
      const codeDisplay = searchParameters.length > 1 ? 'Codes' : 'Code';
      const showCode = [DomainType.CONDITION, DomainType.DRUG, DomainType.MEASUREMENT, DomainType.PROCEDURE].includes(type);
      const showCount = !loading && status !== 'hidden' && count !== null;
      const actionItems = [
        {label: 'Edit criteria', command: () => this.launchWizard()},
        {label: 'Suppress criteria from total count', command: () => this.suppress()},
        {label: 'Delete criteria', command: () => this.remove()},
      ];
      return <React.Fragment>
        <style>{itemStyles}</style>
        {status !== 'deleted' && <div style={{display: 'flex', fontSize: '12px'}}>
          {(status === 'active' || !status) && <div style={styles.lineItem}>
            <Menu style={styles.menu} appendTo={document.body} model={actionItems} popup={true} ref={el => this.actionsMenu = el} />
            <Clickable style={{display: 'inline-block', paddingRight: '0.5rem'}} onClick={(event) => this.actionsMenu.toggle(event)}>
              <ClrIcon shape='ellipsis-vertical' />
            </Clickable>
            <span className='item-title' style={{...styles.codeText, paddingRight: '10px'}} onClick={() => this.launchWizard()}>
              Contains {domainToTitle(type)} {codeDisplay}
            </span>
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
          <ClrIcon style={{...styles.caret, ...(paramListOpen ? {transform: 'rotate(90deg)'} : {})}}
                   shape={`caret right`} size={24}
                   onClick={() => this.setState({paramListOpen: !paramListOpen})} />
        </div>}
        <div style={{...styles.parameterList, maxHeight: paramListOpen ? '15rem' : 0}}>
          {searchParameters.slice(0, 5).map((param, p) => <div key={p} style={styles.parameter}>
            {showCode && <b>{param.code}</b>} {param.name}
          </div>)}
          {searchParameters.length > 5 && <span style={styles.viewMore} onClick={() => this.launchWizard()}>
            View/edit all criteria ({searchParameters.length - 5} more)
          </span>}
          {!!modifiers && <React.Fragment>
            <h3 style={{fontSize: '14px', marginTop: '0.25rem'}}>Modifiers</h3>
            {modifiers.map((mod, m) => <div key={m} style={styles.parameter}>{this.modifiersDisplay(mod)}</div>)}
          </React.Fragment>}
        </div>
      </React.Fragment>;
    }
  }
);

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
