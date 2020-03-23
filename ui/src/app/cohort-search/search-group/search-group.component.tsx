import {Component, Input} from '@angular/core';
import * as React from 'react';

import {SearchGroupItem} from 'app/cohort-search/search-group-item/search-group-item.component';
import {criteriaMenuOptionsStore, searchRequestStore, wizardStore} from 'app/cohort-search/search-state.service';
import {domainToTitle, generateId, mapGroup, typeToTitle} from 'app/cohort-search/utils';
import {Clickable} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import {RenameModal} from 'app/components/rename-modal';
import {Spinner} from 'app/components/spinners';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {isAbortError} from 'app/utils/errors';
import {WorkspaceData} from 'app/utils/workspace-data';
import {DomainType, ResourceType, SearchRequest, TemporalMention, TemporalTime} from 'generated/fetch';
import {InputSwitch} from 'primereact/inputswitch';
import {Menu} from 'primereact/menu';
import {TieredMenu} from 'primereact/tieredmenu';

const styles = reactStyles({
  card: {
    background: colors.white,
    borderColor: 'rgba(215, 215, 215, 0.5)',
    borderRadius: '0.2rem',
    boxShadow: `0 0.125rem 0.125rem 0 ${colorWithWhiteness(colors.black, 0.85)}`,
    margin: '0 0 0.6rem'
  },
  cardBlock: {
    borderBottom: `1px solid ${colors.light}`,
    padding: '0.5rem 0.5rem 0.5rem 0.75rem'
  },
  cardHeader: {
    backgroundColor: colorWithWhiteness(colors.light, -0.3),
    color: colors.primary,
    fontSize: '14px',
    fontWeight: 600,
    minWidth: '100%',
    padding: '0.5rem 0.75rem'
  },
  overlay: {
    background: colors.white,
    display: 'table',
    opacity: 0.9,
    position: 'relative',
    textAlign: 'center',
    verticalAlign: 'middle',
  },
  overlayInner: {
    color: colors.warning,
    display: 'table-cell',
    fontSize: '18px',
    verticalAlign: 'middle',
  },
  overlayButton: {
    background: 'transparent',
    border: 0,
    color: colors.accent,
    cursor: 'pointer',
    fontSize: '14px',
    fontWeight: 600,
    letterSpacing: 0,
    margin: '0.25rem 0',
  },
  itemOr: {
    background: colors.white,
    color: colorWithWhiteness(colors.black, 0.75),
    float: 'right',
    marginRight: '46%',
    padding: '0 10px'
  },
  menu: {
    maxWidth: '15rem',
    minWidth: '5rem',
    width: 'auto'
  },
  searchItem: {
    borderBottom: `1px solid ${colorWithWhiteness(colors.black, 0.75)}`,
    margin: '0 0.5rem',
    padding: '0.5rem 0.25rem'
  },
  menuButton: {
    border: `1px solid ${colorWithWhiteness(colors.black, 0.75)}`,
    borderRadius: '0.125rem',
    color: colorWithWhiteness(colors.black, 0.45),
    fontSize: '12px',
    fontWeight: 100,
    height: '1.5rem',
    letterSpacing: '1px',
    lineHeight: '1.5rem',
    padding: '0 0.5rem',
    textTransform: 'uppercase',
    verticalAlign: 'middle'
  },
  row: {
    display: 'flex',
    flexWrap: 'wrap',
    marginLeft: '-0.5rem',
    marginRight: '-0.5rem',
  },
  col6: {
    flex: '0 0 50%',
    maxWidth: '50%',
    padding: '0 0.25rem',
  },
  timeInput: {
    border: `1px solid ${colorWithWhiteness(colors.black, 0.75)}`,
    borderRadius: '0.1rem',
    height: '1.4rem',
    marginLeft: '0.5rem',
    verticalAlign: 'middle',
    width: '3rem'
  },
  inputError: {
    background: colorWithWhiteness(colors.danger, 0.85),
    border: `1px solid ${colorWithWhiteness(colors.danger, 0.6)}`,
    borderRadius: '3px',
    color: colorWithWhiteness(colors.dark, .1),
    fontSize: '11px',
    lineHeight: '16px',
    marginBottom: '0.25rem',
    padding: '5px 3px'
  }
});

const css = `
  .p-inputswitch.p-disabled > .p-inputswitch-slider {
    cursor: not-allowed;
  }
  body .p-menuitem > .p-menuitem-link {
    height: 1.25rem;
    line-height: 1.25rem;
    padding: 0 1rem;
  }
  body .p-menuitem.menuitem-header > .p-menuitem-link {
    font-size: 12px;
    font-weight: 600;
    height: auto;
    line-height: 0.75rem;
    padding-left: 0.5rem;
  }
  body .p-tieredmenu .p-menu-separator {
    margin: 0.25rem 0;
  }
  body .p-tieredmenu .p-submenu-list {
    padding: 0.5rem 0;
    width: 10rem;
  }
`;

const temporalMentions = [
  TemporalMention.ANYMENTION,
  TemporalMention.FIRSTMENTION,
  TemporalMention.LASTMENTION
];

const temporalTimes = [
  TemporalTime.DURINGSAMEENCOUNTERAS,
  TemporalTime.XDAYSAFTER,
  TemporalTime.XDAYSBEFORE,
  TemporalTime.WITHINXDAYSOF
];

function temporalEnumToText(option) {
  switch (option) {
    case TemporalMention.ANYMENTION:
      return 'Any mention of';
    case TemporalMention.FIRSTMENTION:
      return 'First mention of';
    case TemporalMention.LASTMENTION:
      return 'Last mention of';
    case TemporalTime.DURINGSAMEENCOUNTERAS:
      return 'During same encounter as';
    case TemporalTime.XDAYSBEFORE:
      return 'X or more days before';
    case TemporalTime.XDAYSAFTER:
      return 'X or more days after';
    case TemporalTime.WITHINXDAYSOF:
      return 'On or within X days of';
  }
}

function initItem(id: string, type: string, tempGroup: number) {
  return {
    id,
    type,
    searchParameters: [],
    modifiers: [],
    temporalGroup: tempGroup,
    status: 'active'
  };
}

interface Props {
  group: any;
  index: number;
  role: keyof SearchRequest;
  updated: number;
  updateRequest: Function;
  workspace: WorkspaceData;
}

interface State {
  count: number;
  criteriaMenuOptions: any;
  error: boolean;
  initializing: boolean;
  inputError: boolean;
  inputTouched: boolean;
  loading: boolean;
  overlayStyle: any;
  renaming: boolean;
}

export const SearchGroup = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    private aborter = new AbortController();
    private criteriaMenu: any;
    private deleteTimeout: NodeJS.Timeout;
    private groupMenu: any;
    private mentionMenu: any;
    private temporalCriteriaMenu: any;
    private timeMenu: any;

    constructor(props: any) {
      super(props);
      this.state = {
        count: undefined,
        criteriaMenuOptions: {programTypes: [], domainTypes: []},
        error: false,
        initializing: true,
        inputError: false,
        inputTouched: false,
        loading: false,
        overlayStyle: undefined,
        renaming: false
      };
    }

    componentDidMount(): void {
      const {group: {id}, updateRequest, workspace: {cdrVersionId}} = this.props;
      criteriaMenuOptionsStore.subscribe(options => {
        if (!!options[cdrVersionId]) {
          this.setState({criteriaMenuOptions: options[cdrVersionId]});
        }
      });
      if (typeof ResizeObserver === 'function') {
        const groupDiv = document.getElementById(id);
        // check that groupDiv is of type Element
        if (groupDiv && groupDiv.tagName) {
          // create observer to reposition overlays on div resize
          const ro = new ResizeObserver(() => {
            const {status} = this.props.group;
            if (status === 'hidden' || status === 'pending') {
              this.setOverlayPosition();
            }
          });
          ro.observe(groupDiv);
        }
      }
      updateRequest();
      this.getGroupCount();
    }

    componentWillUnmount(): void {
      this.aborter.abort();
      clearTimeout(this.deleteTimeout);
    }

    getGroupCount() {
      this.abortPendingCalls();
      this.setState({error: false, loading: true});
      const {group, role, workspace: {cdrVersionId}} = this.props;
      const mappedGroup = mapGroup(group);
      const request = {
        includes: [],
        excludes: [],
        dataFilters: [],
        [role]: [mappedGroup]
      };
      cohortBuilderApi().countParticipants(+cdrVersionId, request, {signal: this.aborter.signal})
        .then(count => this.setState({count, initializing: false, loading: false}))
        .catch(error => {
          if (!isAbortError(error)) {
            console.error(error);
            this.setState({error: true, loading: false});
          }
        });
    }

    abortPendingCalls() {
      if (this.state.loading) {
        this.aborter.abort();
        this.aborter = new AbortController();
      }
    }

    update() {
      const {group: {temporal}, updateRequest} = this.props;
      // Prevent multiple group count calls when loading an existing cohort
      if (!this.state.initializing) {
        updateRequest();
        if (this.hasActiveItems && (!temporal || !this.temporalError)) {
          this.getGroupCount();
        }
      }
    }

    get hasActiveItems() {
      return this.props.group.items.some(it => it.status === 'active');
    }

    get items() {
      const {group: {items, temporal}} = this.props;
      return !temporal ? items : items.filter(it => it.temporalGroup === 0);
    }

    get temporalItems() {
      const {group: {items}} = this.props;
      return items.filter(it => it.temporalGroup === 1);
    }

    get disableTemporal() {
      return this.items.some(it => [DomainType.PHYSICALMEASUREMENT, DomainType.PERSON, DomainType.SURVEY].includes(it.type));
    }

    remove() {
      triggerEvent('Delete', 'Click', 'Snowman - Delete Group - Cohort Builder');
      this.hide('pending');
      this.deleteTimeout = setTimeout(() => {
        this.removeGroup();
      }, 10000);
    }

    hide(status: string) {
      triggerEvent('Suppress', 'Click', 'Snowman - Suppress Group - Cohort Builder');
      this.setGroupProperty('status', status);
      setTimeout(() => this.setOverlayPosition());
    }

    enable() {
      triggerEvent('Enable', 'Click', 'Enable - Suppress Group - Cohort Builder');
      this.setGroupProperty('status', 'active');
      this.setState({overlayStyle: undefined});
    }

    undo() {
      triggerEvent('Undo', 'Click', 'Undo - Delete Group - Cohort Builder');
      clearTimeout(this.deleteTimeout);
      this.enable();
    }

    removeGroup() {
      const {group, role} = this.props;
      const searchRequest = searchRequestStore.getValue();
      searchRequest[role] = searchRequest[role].filter(grp => grp.id !== group.id);
      searchRequestStore.next(searchRequest);
    }

    rename(newName: string) {
      const {group, index, role} = this.props;
      const searchRequest = searchRequestStore.getValue();
      searchRequest[role][index] = {...group, name: newName};
      searchRequestStore.next(searchRequest);
      this.setState({renaming: false});
    }

    setOverlayPosition() {
      const {group} = this.props;
      const groupCard = document.getElementById(group.id);
      if (groupCard) {
        const {marginBottom, width, height} = window.getComputedStyle(groupCard);
        const margin = `-${(parseFloat(height) + parseFloat(marginBottom))}px 0 ${marginBottom}`;
        this.setState({overlayStyle: {height, margin, width}});
      }
    }

    launchWizard(criteria: any, temporalGroup: number) {
      this.criteriaMenu.hide();
      const {group, role} = this.props;
      const {domain, type, standard} = criteria;
      if (temporalGroup === 1) {
        triggerEvent('Temporal', 'Click', `${domainToTitle(domain)} - Temporal - Cohort Builder`);
      } else {
        const category = `${role === 'includes' ? 'Add' : 'Excludes'} Criteria`;
        // If domain is PERSON, list the type as well as the domain in the label
        const label = `${domainToTitle(domain)} ${(domain === DomainType.PERSON ? `- ${typeToTitle(type)}` : '')} - Cohort Builder`;
        triggerEvent(category, 'Click', `${category} - ${label}`);
      }
      const itemId = generateId('items');
      const item = initItem(itemId, domain, temporalGroup);
      const fullTree = criteria.fullTree || false;
      const groupId = group.id;
      const context = {item, domain, type, standard, role, groupId, itemId, fullTree, temporalGroup};
      wizardStore.next(context);
    }

    setGroupProperty(property: string, value: any) {
      const {group, role, updateRequest} = this.props;
      const searchRequest = searchRequestStore.getValue();
      const groupIndex = searchRequest[role].findIndex(grp => grp.id === group.id);
      if (groupIndex > -1) {
        searchRequest[role][groupIndex][property] = value;
        searchRequestStore.next(searchRequest);
        updateRequest();
      }
    }

    handleTemporalChange(e: any) {
      const {value} = e.target;
      triggerEvent('Temporal', 'Click', 'Turn On Off - Temporal - Cohort Builder');
      this.setGroupProperty('temporal', value);
      if ((!value && this.hasActiveItems) || (value && !this.temporalError)) {
        this.getGroupCount();
      }
    }

    setMention(mention: TemporalMention) {
      if (mention !== this.props.group.mention) {
        triggerEvent('Temporal', 'Click', `${temporalEnumToText(mention)} - Temporal - Cohort Builder`);
        this.setGroupProperty('mention', mention);
        if (!this.temporalError) {
          this.getGroupCount();
        }
      }
    }

    setTime(time: TemporalTime) {
      triggerEvent('Temporal', 'Click', `${temporalEnumToText(time)} - Temporal - Cohort Builder`);
      this.setGroupProperty('time', time);
      if (!this.temporalError) {
        this.getGroupCount();
      }
    }

    setTimeValue(timeValue: string) {
      const timeValueInt = parseInt(timeValue, 10);
      this.setState({inputError: isNaN(timeValueInt) || timeValueInt < 0, inputTouched: true});
      this.setGroupProperty('timeValue', timeValue);
      if (!this.temporalError) {
        this.getGroupCount();
      }
    }

    get temporalError() {
      const {group: {items, time, timeValue}} = this.props;
      const counts = items.reduce((acc, it) => {
        if (it.status === 'active') {
          acc[it.temporalGroup]++;
        }
        return acc;
      }, [0, 0]);
      const inputError = time !== TemporalTime.DURINGSAMEENCOUNTERAS && isNaN(parseInt(timeValue, 10));
      return counts.includes(0) || inputError;
    }

    mapCriteriaMenuItem(domain: any, temporalGroup: number) {
      if (!!domain.children) {
        return {label: domain.name, items: domain.children.map((dt) => this.mapCriteriaMenuItem(dt, temporalGroup))};
      }
      return {label: domain.name, command: () => this.launchWizard(domain, temporalGroup)};
    }

    get criteriaMenuItems() {
      const {criteriaMenuOptions: {domainTypes, programTypes}} = this.state;
      return this.props.group.temporal
        ? domainTypes.map((dt) => this.mapCriteriaMenuItem(dt, 0))
        : [
          {label: 'Program Data', className: 'menuitem-header'},
          ...programTypes.map((dt) => this.mapCriteriaMenuItem(dt, 0)),
          {separator: true},
          {label: 'Domains', className: 'menuitem-header'},
          ...domainTypes.map((dt) => this.mapCriteriaMenuItem(dt, 0))
        ];
    }

    get temporalCriteriaMenuItems() {
      return this.state.criteriaMenuOptions.domainTypes.map((dt) => this.mapCriteriaMenuItem(dt, 1));
    }

    get mentionMenuItems() {
      return temporalMentions.map(tm => ({label: temporalEnumToText(tm), command: () => this.setMention(tm)}));
    }

    get timeMenuItems() {
      return temporalTimes.map(tt => ({label: temporalEnumToText(tt), command: () => this.setTime(tt)}));
    }

    get groupMenuItems() {
      return [
        {label: 'Edit group name', command: () => this.setState({renaming: true})},
        {label: 'Suppress group from total count', command: () => this.hide('hidden')},
        {label: 'Delete group', command: () => this.remove()},
      ];
    }

    get groupErrorText() {
      if (this.state.error) {
        return 'Sorry, the request cannot be completed. Please try again or contact Support in the left hand navigation.';
      }
      if (!this.hasActiveItems) {
        return 'All criteria in this group are suppressed. Un-suppress criteria to update the group count based on the visible criteria.';
      }
      if (this.props.group.temporal && this.temporalError) {
        return 'Please complete criteria selections before saving temporal relationship.';
      }
      return '';
    }

    get existingGroupNames() {
      const {group, role} = this.props;
      const searchRequest = searchRequestStore.getValue();
      return searchRequest[role]
        .filter(grp => grp.id !== group.id && !!grp.name)
        .map(grp => grp.name);
    }

    render() {
      const {group: {id, items, mention, name, status, temporal, time, timeValue}, index, role} = this.props;
      const {count, error, inputError, inputTouched, loading, overlayStyle, renaming} = this.state;
      const groupName = !!name ? name : `Group ${index + 1}`;
      const showGroupCount = !loading && !error && this.hasActiveItems && (!temporal || !this.temporalError) && count !== undefined;
      const showGroupError = error || !this.hasActiveItems || (temporal && this.temporalError);
      return <React.Fragment>
        <style>{css}</style>
        <div id={id} style={styles.card}>
          {/* Group Header */}
          <div style={styles.cardHeader}>
            <Menu style={styles.menu}
              appendTo={document.body}
              model={this.groupMenuItems}
              popup
              ref={el => this.groupMenu = el} />
            <Clickable style={{display: 'inline-block', paddingRight: '0.5rem'}}onClick={(e) => this.groupMenu.toggle(e)}>
              <ClrIcon style={{color: colors.accent}} shape='ellipsis-vertical'/>
            </Clickable>
            {groupName}
          </div>
          {/* Temporal mention dropdown */}
          {temporal && <div style={styles.cardBlock}>
            <Menu style={styles.menu}
              appendTo={document.body}
              model={this.mentionMenuItems}
              popup
              ref={el => this.mentionMenu = el} />
            <button style={styles.menuButton} onClick={(e) => this.mentionMenu.toggle(e)}>
              {temporalEnumToText(mention)} <ClrIcon shape='caret down' size={12}/>
            </button>
          </div>}
          {/* Main search item list/temporal group 0 items */}
          {this.items.map((item, i) => <div key={i} data-test-id='item-list' style={styles.searchItem}>
            <SearchGroupItem role={role}
              groupId={id}
              item={item}
              index={i}
              updateGroup={() => this.update()}/>
            {status === 'active' && <div style={styles.itemOr}>OR</div>}
          </div>)}
          {/* Criteria menu for main search item list/temporal group 0 items */}
          <div style={styles.cardBlock}>
            <TieredMenu style={{...styles.menu, padding: '0.5rem 0'}}
              appendTo={document.body}
              model={this.criteriaMenuItems}
              popup
              ref={el => this.criteriaMenu = el} />
            <button style={styles.menuButton} onClick={(e) => this.criteriaMenu.toggle(e)}>
              Add Criteria <ClrIcon shape='caret down' size={12}/>
            </button>
          </div>
          {temporal && <React.Fragment>
            {/* Temporal time dropdown */}
            <div style={styles.cardBlock}>
              {time !== TemporalTime.DURINGSAMEENCOUNTERAS && inputError && inputTouched && <div style={styles.inputError}>
                Please enter a positive number
              </div>}
              <Menu style={styles.menu}
                appendTo={document.body}
                model={this.timeMenuItems}
                popup
                ref={el => this.timeMenu = el} />
              <button style={styles.menuButton} onClick={(e) => this.timeMenu.toggle(e)}>
                {temporalEnumToText(time)} <ClrIcon shape='caret down' size={12}/>
              </button>
              {time !== TemporalTime.DURINGSAMEENCOUNTERAS &&
                <input style={styles.timeInput} type='number' value={timeValue} min={0}
                  onChange={(v) => this.setTimeValue(v.target.value)}/>
              }
            </div>
            {/* Temporal group 1 items */}
            {this.temporalItems.map((item, i) => <div key={i} style={styles.searchItem} data-test-id='temporal-item-list'>
              <SearchGroupItem
                role={role}
                groupId={id}
                item={item}
                index={i}
                updateGroup={() => this.update()}/>
              {status === 'active' && <div style={styles.itemOr}>OR</div>}
            </div>)}
            {/* Criteria menu for temporal group 1 items */}
            <div style={styles.cardBlock}>
              <Menu style={styles.menu} appendTo={document.body} model={this.temporalCriteriaMenuItems}
                popup ref={el => this.temporalCriteriaMenu = el} />
              <button style={styles.menuButton} onClick={(e) => this.temporalCriteriaMenu.toggle(e)}>
                Add Criteria <ClrIcon shape='caret down' size={12}/>
              </button>
            </div>
          </React.Fragment>}
          {/* Group footer */}
          {!!items.length && <div style={styles.cardHeader}>
            <div style={this.disableTemporal ? {...styles.row, cursor: 'not-allowed'} : styles.row}>
              <div style={{...styles.col6, display: 'flex'}}>
                <InputSwitch
                  checked={temporal}
                  disabled={this.disableTemporal}
                  onChange={(e) => this.handleTemporalChange(e)}/>
                <div style={{paddingLeft: '0.5rem'}}>Temporal</div>
              </div>
              <div style={{...styles.col6, textAlign: 'right'}}>
                <div>
                  Group Count:&nbsp;
                  {loading && (!temporal || !this.temporalError) && <Spinner size={16}/>}
                  {showGroupCount && <span>
                    {count.toLocaleString()}
                  </span>}
                  {!temporal && error &&
                    <ClrIcon className='is-solid' style={{color: colors.white}} shape='exclamation-triangle' size={22}/>
                  }
                  {showGroupError && <span>
                    -- &nbsp;
                    <TooltipTrigger content={this.groupErrorText}>
                      <ClrIcon style={{color: colors.warning}} shape='warning-standard' size={18}/>
                    </TooltipTrigger>
                  </span>}
                </div>
              </div>
            </div>
          </div>}
        </div>
        {/* Overlay for deleted and suppressed groups */}
        {!!overlayStyle && <div style={{...styles.overlay, ...overlayStyle}} data-test-id='disabled-overlay'>
          <div style={styles.overlayInner}>
            {status === 'pending' && <React.Fragment>
              <ClrIcon className='is-solid' shape='exclamation-triangle' size={56}/>
              <span>
                This group has been deleted
                <button style={styles.overlayButton} onClick={() => this.undo()}>UNDO</button>
              </span>
            </React.Fragment>}
            {status === 'hidden' && <React.Fragment>
              <ClrIcon className='is-solid' shape='eye-hide' size={56}/>
              <span>
                This group has been suppressed
                <button style={styles.overlayButton} onClick={() => this.enable()}>ENABLE</button>
              </span>
            </React.Fragment>}
          </div>
        </div>}
        {renaming && <RenameModal existingNames={this.existingGroupNames}
          oldName={name || 'this group'}
          hideDescription={true}
          onCancel={() => this.setState({renaming: false})}
          onRename={(v) => this.rename(v)} resourceType={ResourceType.COHORTSEARCHGROUP} />}
      </React.Fragment>;
    }
  }
);

@Component({
  selector: 'app-list-search-group',
  template: '<div #root></div>'
})
export class SearchGroupComponent extends ReactWrapperBase {
  @Input('group') group: Props['group'];
  @Input('index') index: Props['index'];
  @Input('role') role: Props['role'];
  @Input('updated') updated: Props['updated'];
  @Input('updateRequest') updateRequest: Props['updateRequest'];

  constructor() {
    super(SearchGroup, ['group', 'index', 'role', 'updated', 'updateRequest']);
  }
}
