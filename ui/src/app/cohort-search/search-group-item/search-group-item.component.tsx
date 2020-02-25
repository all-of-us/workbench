import * as React from 'react';

import {MODIFIERS_MAP} from 'app/cohort-search/constant';
import {encountersStore, initExisting, searchRequestStore, selectionsStore, wizardStore} from 'app/cohort-search/search-state.service';
import {domainToTitle, getTypeAndStandard, mapGroupItem, typeToTitle} from 'app/cohort-search/utils';
import {Button, Clickable} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {RenameModal} from 'app/components/rename-modal';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {CriteriaType, DomainType, Modifier, ModifierType, ResourceType, SearchRequest} from 'generated/fetch';
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
  suppressedItem: {
    color: colorWithWhiteness(colors.warning, -.35),
    flex: 4,
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
    marginTop: '3px',
    transition: 'transform 0.1s ease-out',
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
  },
  disabled: {
    color: colors.accent,
    pointerEvents: 'none',
    opacity: 0.6,
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

class SearchGroupItemParameter extends React.Component<{parameter: any}, {tooltip: boolean}> {
  element: HTMLDivElement;
  overlay: any;
  constructor(props: any) {
    super(props);
    this.state = {tooltip: false};
  }

  componentDidMount(): void {
    const {offsetWidth, scrollWidth} = this.element;
    this.setState({tooltip: scrollWidth > offsetWidth});
  }

  render() {
    const {parameter, parameter: {domainId}} = this.props;
    const {tooltip} = this.state;
    const showCode = [DomainType.CONDITION, DomainType.DRUG, DomainType.MEASUREMENT, DomainType.PROCEDURE].includes(domainId);
    return <div ref={el => this.element = el} style={styles.parameter}>
      <span style={domainId === DomainType.PERSON ? {textTransform: 'capitalize'} : {}}
            onMouseEnter={(e) => tooltip && this.overlay.show(e)} onMouseLeave={() => tooltip && this.overlay.hide()}>
        {showCode && <b>{parameter.code}</b>} {parameter.name}
      </span>
      {tooltip && <OverlayPanel style={{maxWidth: '30%'}} ref={el => this.overlay = el} appendTo={document.body}>
        {parameter.name}
      </OverlayPanel>}
    </div>;
  }
}

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
  renaming: boolean;
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
        error: false,
        loading: true,
        paramListOpen: false,
        renaming: false,
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
        updateGroup(true);
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
      this.updateSearchRequest(true);
    }

    suppress() {
      triggerEvent('Suppress', 'Click', 'Snowman - Suppress Criteria - Cohort Builder');
      this.setState({status: 'hidden'});
      this.props.item.status = 'hidden';
      this.updateSearchRequest(true);
    }

    remove() {
      triggerEvent('Delete', 'Click', 'Snowman - Delete Criteria - Cohort Builder');
      this.setState({status: 'pending'});
      this.props.item.status = 'pending';
      this.updateSearchRequest(true);
      const timeout = setTimeout(() => {
        this.updateSearchRequest(false, true);
      }, 10000);
      this.setState({timeout});
    }

    undo() {
      triggerEvent('Undo', 'Click', 'Undo - Delete Criteria - Cohort Builder');
      clearTimeout(this.state.timeout);
      this.setState({status: 'active'});
      this.props.item.status = 'active';
      this.updateSearchRequest(true);
    }

    rename(newName: string) {
      this.props.item.name = newName;
      this.updateSearchRequest(false);
      this.setState({renaming: false});
    }

    updateSearchRequest(recalculate: boolean, remove?: boolean) {
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
            updateGroup(recalculate);
          }
        }
      }
    }

    launchWizard() {
      triggerEvent('Edit', 'Click', 'Snowman - Edit Criteria - Cohort Builder');
      const {groupId, item, role} = this.props;
      const {count} = this.state;
      const _item = JSON.parse(JSON.stringify(item));
      const {fullTree, id, searchParameters} = _item;
      const selections = searchParameters.map(sp => sp.parameterId);
      selectionsStore.next(selections);
      const domain = _item.type;
      const {type, standard} = getTypeAndStandard(searchParameters, domain);
      const context = {item: _item, domain, type, role, groupId, itemId: id, fullTree, standard, count};
      wizardStore.next(context);
    }

    modifierDisplay(mod: Modifier) {
      const {name, operands, operator} = mod;
      switch (name) {
        case ModifierType.ENCOUNTERS:
          const {encounters} = this.state;
          const visit = !!encounters ? encounters.find(en => en.conceptId.toString() === operands[0]).name : '';
          return <span><b>{MODIFIERS_MAP[name].name}</b> {visit}</span>;
        case ModifierType.NUMOFOCCURRENCES:
          return <span><b>{MODIFIERS_MAP[name].name}</b> {operands[0]} Or More</span>;
        default:
          return <span><b>{MODIFIERS_MAP[name].name}</b> {MODIFIERS_MAP[name].operators[operator]} {operands.join(' and ')}</span>;
      }
    }

    render() {
      const {item: {modifiers, name, searchParameters, type}} = this.props;
      const {count, paramListOpen, error, loading, renaming, status} = this.state;
      const codeDisplay = searchParameters.length > 1 ? 'Codes' : 'Code';
      const titleDisplay = type === DomainType.PERSON ? typeToTitle(searchParameters[0].type) : domainToTitle(type);
      const itemName = !!name ? name : `Contains ${titleDisplay} ${codeDisplay}`;
      const showCount = !loading && status !== 'hidden' && count !== null;
      const actionItems = [
        {label: 'Edit criteria name', command: () => this.setState({renaming: true})},
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
              {itemName}
            </span>
            {status !== 'hidden' && <span style={{...styles.codeText, paddingRight: '10px'}}>|</span>}
            {loading && <span className='spinner spinner-inline'>Loading...</span>}
            {showCount && <span style={styles.codeText}>{count.toLocaleString()}</span>}
            {error && <span><ClrIcon style={{color: colors.warning}} shape='exclamation-triangle' className='is-solid' size={22} /></span>}
          </div>}
          {status === 'pending' && <div style={styles.suppressedItem}>
            <ClrIcon shape='exclamation-triangle' className='is-solid' size={23} />
            <span style={{margin: '0 0 2px 2px'}}>
              This criteria has been deleted
              <Button type='link' style={styles.link} onClick={() => this.undo()}>UNDO</Button>
            </span>
          </div>}
          {status === 'hidden' && <div style={styles.suppressedItem}>
            <ClrIcon shape='eye-hide' className='is-solid' size={23} />
            <span style={{margin: '0 0 2px 2px'}}>
              This criteria has been suppressed
              <Button type='link' style={styles.link} onClick={() => this.enable()}>ENABLE</Button>
            </span>
          </div>}
          <ClrIcon style={{...styles.caret, ...(paramListOpen ? {transform: 'rotate(90deg)'} : {})}}
                   shape={`caret right`} size={18}
                   onClick={() => this.setState({paramListOpen: !paramListOpen})} />
        </div>}
        <div style={{...styles.parameterList, maxHeight: paramListOpen ? '15rem' : 0}}>
          {searchParameters.slice(0, 5).map((param, p) => <SearchGroupItemParameter key={p} parameter={param} />)}
          {searchParameters.length > 5 &&
            <span style={status === 'active' ? styles.viewMore : styles.disabled} onClick={() => this.launchWizard()}>
              View/edit all criteria ({searchParameters.length - 5} more)
            </span>
          }
          {!!modifiers && modifiers.length > 0 && <React.Fragment>
            <h3 style={{fontSize: '14px', marginTop: '0.25rem'}}>Modifiers</h3>
            {modifiers.map((mod, m) => <div key={m} style={styles.parameter}>{this.modifierDisplay(mod)}</div>)}
          </React.Fragment>}
        </div>
        {renaming && <RenameModal existingNames={[]} oldName={name || 'this item'} hideDescription={true}
          onCancel={() => this.setState({renaming: false})}
          onRename={(v) => this.rename(v)} resourceType={ResourceType.COHORTSEARCHITEM} />}
      </React.Fragment>;
    }
  }
);
