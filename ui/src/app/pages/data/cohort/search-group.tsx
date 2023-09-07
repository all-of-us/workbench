import * as React from 'react';
import { InputSwitch } from 'primereact/inputswitch';
import { Menu } from 'primereact/menu';

import {
  CohortDefinition,
  Domain,
  ResourceType,
  TemporalMention,
  TemporalTime,
} from 'generated/fetch';

import { Clickable } from 'app/components/buttons';
import { ClrIcon } from 'app/components/icons';
import { NumberInput } from 'app/components/inputs';
import { TooltipTrigger } from 'app/components/popups';
import { RenameModal } from 'app/components/rename-modal';
import { Spinner } from 'app/components/spinners';
import { CohortCriteriaMenu } from 'app/pages/data/cohort/cohort-criteria-menu';
import { SearchGroupItem } from 'app/pages/data/cohort/search-group-item';
import {
  criteriaMenuOptionsStore,
  searchRequestStore,
} from 'app/pages/data/cohort/search-state.service';
import {
  domainToTitle,
  generateId,
  mapGroup,
  typeToTitle,
} from 'app/pages/data/cohort/utils';
import { cohortBuilderApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles, withCurrentWorkspace } from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { isAbortError } from 'app/utils/errors';
import { currentGroupCountsStore } from 'app/utils/navigation';
import { WorkspaceData } from 'app/utils/workspace-data';

const styles = reactStyles({
  card: {
    background: colors.white,
    borderColor: 'rgba(215, 215, 215, 0.5)',
    borderRadius: '0.3rem',
    boxShadow: `0 0.1875rem 0.1875rem 0 ${colorWithWhiteness(
      colors.black,
      0.85
    )}`,
    margin: '0 0 0.9rem',
  },
  cardBlock: {
    borderBottom: `1px solid ${colors.light}`,
    padding: '0.75rem 0.75rem 0.75rem 1.125rem',
  },
  cardHeader: {
    backgroundColor: colorWithWhiteness(colors.light, -0.3),
    color: colors.primary,
    fontSize: '14px',
    fontWeight: 600,
    minWidth: '100%',
    padding: '0.75rem 1.125rem',
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
    margin: '0.375rem 0',
  },
  itemOr: {
    background: colors.white,
    color: colorWithWhiteness(colors.black, 0.75),
    float: 'right',
    marginRight: '46%',
    padding: '0 10px',
  },
  menu: {
    maxWidth: '22.5rem',
    minWidth: '7.5rem',
    width: 'auto',
  },
  searchItem: {
    borderBottom: `1px solid ${colorWithWhiteness(colors.black, 0.75)}`,
    margin: '0 0.75rem',
    padding: '0.75rem 0.375rem',
  },
  menuButton: {
    background: colors.white,
    border: `1px solid ${colorWithWhiteness(colors.black, 0.75)}`,
    borderRadius: '0.1875rem',
    color: colorWithWhiteness(colors.black, 0.45),
    cursor: 'pointer',
    fontSize: '12px',
    fontWeight: 100,
    height: '2.25rem',
    letterSpacing: '1px',
    lineHeight: '2.25rem',
    padding: '0 0.75rem',
    textTransform: 'uppercase',
    verticalAlign: 'middle',
  },
  row: {
    display: 'flex',
    flexWrap: 'wrap',
    marginLeft: '-0.75rem',
    marginRight: '-0.75rem',
  },
  col6: {
    flex: '0 0 50%',
    maxWidth: '50%',
    padding: '0 0.375rem',
  },
  timeInput: {
    height: '2.1rem',
    marginLeft: '0.75rem',
    padding: '0 0.375rem',
    verticalAlign: 'middle',
    width: '4.5rem',
  },
  inputError: {
    background: colorWithWhiteness(colors.danger, 0.85),
    border: `1px solid ${colorWithWhiteness(colors.danger, 0.6)}`,
    borderRadius: '3px',
    color: colorWithWhiteness(colors.dark, 0.1),
    fontSize: '11px',
    lineHeight: '16px',
    marginBottom: '0.375rem',
    padding: '5px 3px',
  },
});

const css = `
  .p-inputswitch.p-disabled > .p-inputswitch-slider {
    cursor: not-allowed;
  }
`;

const temporalMentions = [
  TemporalMention.ANY_MENTION,
  TemporalMention.FIRST_MENTION,
  TemporalMention.LAST_MENTION,
];

const temporalTimes = [
  TemporalTime.DURING_SAME_ENCOUNTER_AS,
  TemporalTime.X_DAYS_AFTER,
  TemporalTime.X_DAYS_BEFORE,
  TemporalTime.WITHIN_X_DAYS_OF,
];

function temporalEnumToText(option) {
  switch (option) {
    case TemporalMention.ANY_MENTION:
      return 'Any mention of';
    case TemporalMention.FIRST_MENTION:
      return 'First mention of';
    case TemporalMention.LAST_MENTION:
      return 'Last mention of';
    case TemporalTime.DURING_SAME_ENCOUNTER_AS:
      return 'During same encounter as';
    case TemporalTime.X_DAYS_BEFORE:
      return 'X or more days before';
    case TemporalTime.X_DAYS_AFTER:
      return 'X or more days after';
    case TemporalTime.WITHIN_X_DAYS_OF:
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
    status: 'active',
  };
}

interface Props {
  group: any;
  groupIndex: number;
  role: keyof CohortDefinition;
  roleIndex: number;
  setSearchContext: (context: any) => void;
  updated: number;
  updateRequest: Function;
  workspace: WorkspaceData;
}

interface State {
  count: number;
  criteriaMenuOptions: Array<any>;
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
    private deleteTimeout: NodeJS.Timeout;
    private groupMenu: any;
    private mentionMenu: any;
    private timeMenu: any;

    constructor(props: any) {
      super(props);
      this.state = {
        count: undefined,
        criteriaMenuOptions: [],
        error: false,
        initializing: true,
        inputError: false,
        inputTouched: false,
        loading: false,
        overlayStyle: undefined,
        renaming: false,
      };
    }

    componentDidMount(): void {
      const {
        group: { id, temporal },
        updateRequest,
        workspace: { cdrVersionId },
      } = this.props;
      criteriaMenuOptionsStore.subscribe((options) => {
        if (!!options[cdrVersionId]) {
          this.setState({ criteriaMenuOptions: options[cdrVersionId] });
        }
      });
      if (typeof ResizeObserver === 'function') {
        const groupDiv = document.getElementById(id);
        // check that groupDiv is of type Element
        if (groupDiv?.tagName) {
          // create observer to reposition overlays on div resize
          const ro = new ResizeObserver(() => {
            const { status } = this.props.group;
            if (status === 'hidden' || status === 'pending') {
              this.setOverlayPosition();
            }
          });
          ro.observe(groupDiv);
        }
      }
      updateRequest();
      if (this.hasActiveItems && (!temporal || !this.temporalError)) {
        this.getGroupCount();
      }
    }

    componentWillUnmount(): void {
      this.aborter.abort();
      clearTimeout(this.deleteTimeout);
    }

    getGroupCount() {
      this.abortPendingCalls();
      this.setState({ error: false, loading: true });
      const {
        group,
        groupIndex,
        role,
        workspace: { id, namespace },
      } = this.props;
      const mappedGroup = mapGroup(group);
      const request = {
        includes: [],
        excludes: [],
        dataFilters: [],
        [role]: [mappedGroup],
      };
      cohortBuilderApi()
        .countParticipants(namespace, id, request, {
          signal: this.aborter.signal,
        })
        .then((count) => {
          this.setState({ count, initializing: false, loading: false });
          const currentGroupCounts = currentGroupCountsStore.getValue();
          const groupCountIndex = currentGroupCounts.findIndex(
            ({ groupId }) => groupId === group.id
          );
          if (groupCountIndex > -1) {
            currentGroupCounts[groupCountIndex].groupCount = count;
          } else {
            const groupName =
              group.name ??
              `Group ${
                groupIndex +
                1 +
                (role === 'excludes'
                  ? searchRequestStore.getValue().includes.length + 1
                  : 0)
              }`;
            currentGroupCounts.push({
              groupId: group.id,
              groupName,
              groupCount: count,
              role,
              status: 'active',
            });
          }
          currentGroupCountsStore.next(currentGroupCounts);
        })
        .catch((error) => {
          if (!isAbortError(error)) {
            console.error(error);
            this.setState({ error: true, loading: false });
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
      const {
        group: { temporal },
        updateRequest,
      } = this.props;
      // Prevent multiple group count calls when loading an existing cohort
      if (!this.state.initializing) {
        updateRequest();
        if (this.hasActiveItems && (!temporal || !this.temporalError)) {
          this.getGroupCount();
        }
      }
    }

    get hasActiveItems() {
      return this.props.group.items.some((it) => it.status === 'active');
    }

    get items() {
      const {
        group: { items, temporal },
      } = this.props;
      return !temporal ? items : items.filter((it) => it.temporalGroup === 0);
    }

    get temporalItems() {
      const {
        group: { items },
      } = this.props;
      return items.filter((it) => it.temporalGroup === 1);
    }

    get disableTemporal() {
      return this.items.some((it) =>
        [
          Domain.PHYSICAL_MEASUREMENT,
          Domain.PERSON,
          Domain.SURVEY,
          Domain.FITBIT,
          Domain.WHOLE_GENOME_VARIANT,
          Domain.LR_WHOLE_GENOME_VARIANT,
          Domain.ARRAY_DATA,
          Domain.STRUCTURAL_VARIANT_DATA,
        ].includes(it.type)
      );
    }

    remove() {
      AnalyticsTracker.CohortBuilder.SearchGroupMenu('Delete group');
      this.hide('pending');
      this.deleteTimeout = global.setTimeout(() => {
        this.removeGroup();
      }, 10000);
    }

    hide(status: string) {
      AnalyticsTracker.CohortBuilder.SearchGroupMenu('Suppress group');
      this.setGroupProperty('status', status);
      setTimeout(() => this.setOverlayPosition());
      const currentGroupCounts = currentGroupCountsStore.getValue();
      const groupCountIndex = currentGroupCounts.findIndex(
        ({ groupId }) => groupId === this.props.group.id
      );
      if (groupCountIndex > -1) {
        currentGroupCounts[groupCountIndex].status = status;
        currentGroupCountsStore.next(currentGroupCounts);
      }
    }

    enable() {
      AnalyticsTracker.CohortBuilder.SearchGroupMenu('Enable suppressed group');
      this.setGroupProperty('status', 'active');
      this.setState({ overlayStyle: undefined });
      const currentGroupCounts = currentGroupCountsStore.getValue();
      const groupCountIndex = currentGroupCounts.findIndex(
        ({ groupId }) => groupId === this.props.group.id
      );
      if (groupCountIndex > -1) {
        currentGroupCounts[groupCountIndex].status = 'active';
        currentGroupCountsStore.next(currentGroupCounts);
      }
    }

    undo() {
      AnalyticsTracker.CohortBuilder.SearchGroupMenu('Undo group delete');
      clearTimeout(this.deleteTimeout);
      this.enable();
    }

    removeGroup() {
      const { group, role } = this.props;
      const searchRequest = searchRequestStore.getValue();
      searchRequest[role] = searchRequest[role].filter(
        (grp) => grp.id !== group.id
      );
      searchRequestStore.next(searchRequest);
      const currentGroupCounts = currentGroupCountsStore.getValue();
      const groupCountIndex = currentGroupCounts.findIndex(
        ({ groupId }) => groupId === group.id
      );
      if (groupCountIndex > -1) {
        currentGroupCounts.splice(groupCountIndex, 1);
        currentGroupCountsStore.next(currentGroupCounts);
      }
    }

    rename(newName: string) {
      const { group, roleIndex, role } = this.props;
      const searchRequest = searchRequestStore.getValue();
      searchRequest[role][roleIndex] = { ...group, name: newName };
      searchRequestStore.next(searchRequest);
      this.setState({ renaming: false });
      const currentGroupCounts = currentGroupCountsStore.getValue();
      const groupCountIndex = currentGroupCounts.findIndex(
        ({ groupId }) => groupId === this.props.group.id
      );
      if (groupCountIndex > -1) {
        currentGroupCounts[groupCountIndex].groupName = newName;
        currentGroupCountsStore.next(currentGroupCounts);
      }
    }

    setOverlayPosition() {
      const { group } = this.props;
      const groupCard = document.getElementById(group.id);
      if (groupCard) {
        const { marginBottom, width, height } =
          window.getComputedStyle(groupCard);
        const margin = `-${
          parseFloat(height) + parseFloat(marginBottom)
        }px 0 ${marginBottom}`;
        this.setState({ overlayStyle: { height, margin, width } });
      }
    }

    launchSearch(criteria: any, temporalGroup: number, searchTerms?: string) {
      const { group, setSearchContext, role } = this.props;
      const { domain, selectedSurvey, type, standard, name } = criteria;
      // If domain is PERSON, list the type as well as the domain in the label
      const label = `Enter ${domainToTitle(domain)}${
        domain === Domain.PERSON ? ' - ' + typeToTitle(type) : ''
      } search - ${role === 'includes' ? 'Include' : 'Exclude'} Criteria
      ${temporalGroup === 1 ? ' - Temporal' : ''}`;
      AnalyticsTracker.CohortBuilder.LaunchSearch(label);
      const itemId = generateId('items');
      const item = initItem(itemId, domain, temporalGroup);
      const groupId = group.id;
      const context = {
        item,
        domain,
        type,
        searchTerms,
        standard,
        role,
        groupId,
        temporalGroup,
        selectedSurvey,
        name,
      };
      setSearchContext(context);
    }

    setGroupProperty(property: string, value: any) {
      const { group, role, updateRequest } = this.props;
      const searchRequest = searchRequestStore.getValue();
      const groupIndex = searchRequest[role].findIndex(
        (grp) => grp.id === group.id
      );
      if (groupIndex > -1) {
        searchRequest[role][groupIndex][property] = value;
        searchRequestStore.next(searchRequest);
        updateRequest();
      }
    }

    handleTemporalChange(e: any) {
      const { value } = e.target;
      AnalyticsTracker.CohortBuilder.ToggleTemporal(
        `Turn temporal ${value ? 'on' : 'off'}`
      );
      this.setGroupProperty('temporal', value);
      if ((!value && this.hasActiveItems) || (value && !this.temporalError)) {
        this.getGroupCount();
      }
    }

    setMention(mention: TemporalMention) {
      if (mention !== this.props.group.mention) {
        AnalyticsTracker.CohortBuilder.TemporalMenu(
          `Mention - ${temporalEnumToText(mention)}`
        );
        this.setGroupProperty('mention', mention);
        if (!this.temporalError) {
          this.getGroupCount();
        }
      }
    }

    setTime(time: TemporalTime) {
      AnalyticsTracker.CohortBuilder.TemporalMenu(
        `Time - ${temporalEnumToText(time)}`
      );
      this.setGroupProperty('time', time);
      if (!this.temporalError) {
        this.getGroupCount();
      }
    }

    setTimeValue(timeValue: string) {
      const timeValueInt = parseInt(timeValue, 10);
      this.setState({
        inputError: isNaN(timeValueInt) || timeValueInt < 0,
        inputTouched: true,
      });
      this.setGroupProperty('timeValue', timeValue);
      if (!this.temporalError) {
        this.getGroupCount();
      }
    }

    get temporalError() {
      const {
        group: { items, time, timeValue },
      } = this.props;
      const counts = items.reduce(
        (acc, it) => {
          if (it.status === 'active') {
            acc[it.temporalGroup]++;
          }
          return acc;
        },
        [0, 0]
      );
      const inputError =
        time !== TemporalTime.DURING_SAME_ENCOUNTER_AS &&
        isNaN(parseInt(timeValue, 10));
      return counts.includes(0) || inputError;
    }

    get mentionMenuItems() {
      return temporalMentions.map((tm) => ({
        label: temporalEnumToText(tm),
        command: () => this.setMention(tm),
      }));
    }

    get timeMenuItems() {
      return temporalTimes.map((tt) => ({
        label: temporalEnumToText(tt),
        command: () => this.setTime(tt),
      }));
    }

    get groupMenuItems() {
      return [
        {
          label: 'Edit group name',
          command: () => this.setState({ renaming: true }),
        },
        {
          label: 'Suppress group from total count',
          command: () => this.hide('hidden'),
        },
        { label: 'Delete group', command: () => this.remove() },
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
      const { group, role } = this.props;
      const searchRequest = searchRequestStore.getValue();
      return searchRequest[role]
        .filter((grp) => grp.id !== group.id && !!grp.name)
        .map((grp) => grp.name);
    }

    render() {
      const {
        group: { id, items, mention, name, status, temporal, time, timeValue },
        groupIndex,
        setSearchContext,
        role,
      } = this.props;
      const {
        count,
        criteriaMenuOptions,
        error,
        inputError,
        inputTouched,
        loading,
        overlayStyle,
        renaming,
      } = this.state;
      const groupName = !!name ? name : `Group ${groupIndex + 1}`;
      const showGroupCount =
        !loading &&
        !error &&
        this.hasActiveItems &&
        (!temporal || !this.temporalError) &&
        count !== undefined;
      const showGroupError =
        error || !this.hasActiveItems || (temporal && this.temporalError);
      return (
        <React.Fragment>
          <style>{css}</style>
          <div id={id} style={styles.card}>
            {/* Group Header */}
            <div style={styles.cardHeader}>
              <Menu
                style={styles.menu}
                appendTo={document.body}
                model={this.groupMenuItems}
                popup
                ref={(el) => (this.groupMenu = el)}
              />
              <Clickable
                style={{ display: 'inline-block', paddingRight: '0.75rem' }}
                onClick={(e) => this.groupMenu.toggle(e)}
              >
                <ClrIcon
                  style={{ color: colors.accent }}
                  shape='ellipsis-vertical'
                />
              </Clickable>
              {groupName}
            </div>
            {/* Temporal mention dropdown */}
            {temporal && (
              <div style={styles.cardBlock}>
                <Menu
                  style={styles.menu}
                  appendTo={document.body}
                  model={this.mentionMenuItems}
                  popup
                  ref={(el) => (this.mentionMenu = el)}
                />
                <button
                  style={styles.menuButton}
                  onClick={(e) => this.mentionMenu.toggle(e)}
                >
                  {temporalEnumToText(mention)}{' '}
                  <ClrIcon shape='caret down' size={12} />
                </button>
              </div>
            )}
            {/* Main search item list/temporal group 0 items */}
            {this.items.map((item, i) => (
              <div key={i} data-test-id='item-list' style={styles.searchItem}>
                <SearchGroupItem
                  role={role}
                  groupId={id}
                  item={item}
                  index={i}
                  setSearchContext={setSearchContext}
                  updateGroup={() => this.update()}
                />
                {status === 'active' && <div style={styles.itemOr}>OR</div>}
              </div>
            ))}
            {/* Criteria menu for main search item list/temporal group 0 items */}
            <CohortCriteriaMenu
              launchSearch={(criteria, temporalGroup, searchTerms) =>
                this.launchSearch(criteria, temporalGroup, searchTerms)
              }
              menuOptions={criteriaMenuOptions}
              temporalGroup={0}
              isTemporal={temporal}
            />
            {temporal && (
              <React.Fragment>
                {/* Temporal time dropdown */}
                <div style={styles.cardBlock}>
                  {time !== TemporalTime.DURING_SAME_ENCOUNTER_AS &&
                    inputError &&
                    inputTouched && (
                      <div style={styles.inputError}>
                        Please enter a positive number
                      </div>
                    )}
                  <Menu
                    style={styles.menu}
                    appendTo={document.body}
                    model={this.timeMenuItems}
                    popup
                    ref={(el) => (this.timeMenu = el)}
                  />
                  <button
                    style={styles.menuButton}
                    onClick={(e) => this.timeMenu.toggle(e)}
                  >
                    {temporalEnumToText(time)}{' '}
                    <ClrIcon shape='caret down' size={12} />
                  </button>
                  {time !== TemporalTime.DURING_SAME_ENCOUNTER_AS && (
                    <NumberInput
                      style={styles.timeInput}
                      value={timeValue}
                      min={0}
                      onChange={(v) => this.setTimeValue(v)}
                    />
                  )}
                </div>
                {/* Temporal group 1 items */}
                {this.temporalItems.map((item, i) => (
                  <div
                    key={i}
                    style={styles.searchItem}
                    data-test-id='temporal-item-list'
                  >
                    <SearchGroupItem
                      role={role}
                      groupId={id}
                      item={item}
                      index={i}
                      setSearchContext={setSearchContext}
                      updateGroup={() => this.update()}
                    />
                    {status === 'active' && <div style={styles.itemOr}>OR</div>}
                  </div>
                ))}
                {/* Criteria menu for temporal group 1 items */}
                <CohortCriteriaMenu
                  launchSearch={(criteria, temporalGroup, searchTerms) =>
                    this.launchSearch(criteria, temporalGroup, searchTerms)
                  }
                  menuOptions={criteriaMenuOptions}
                  temporalGroup={1}
                  isTemporal={temporal}
                />
              </React.Fragment>
            )}
            {/* Group footer */}
            {!!items.length && (
              <div style={styles.cardHeader}>
                <div
                  style={
                    this.disableTemporal
                      ? { ...styles.row, cursor: 'not-allowed' }
                      : styles.row
                  }
                >
                  <div style={{ ...styles.col6, display: 'flex' }}>
                    <InputSwitch
                      checked={temporal}
                      disabled={this.disableTemporal}
                      onChange={(e) => this.handleTemporalChange(e)}
                    />
                    <div style={{ paddingLeft: '0.75rem' }}>Temporal</div>
                  </div>
                  <div style={{ ...styles.col6, textAlign: 'right' }}>
                    <div>
                      Group Count:&nbsp;
                      {loading && (!temporal || !this.temporalError) && (
                        <Spinner size={16} />
                      )}
                      {showGroupCount && <span>{count.toLocaleString()}</span>}
                      {!temporal && error && (
                        <ClrIcon
                          className='is-solid'
                          style={{ color: colors.white }}
                          shape='exclamation-triangle'
                          size={22}
                        />
                      )}
                      {showGroupError && (
                        <span>
                          -- &nbsp;
                          <TooltipTrigger content={this.groupErrorText}>
                            <ClrIcon
                              style={{ color: colors.warning }}
                              shape='warning-standard'
                              size={18}
                            />
                          </TooltipTrigger>
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
          {/* Overlay for deleted and suppressed groups */}
          {!!overlayStyle && (
            <div
              style={{ ...styles.overlay, ...overlayStyle }}
              data-test-id='disabled-overlay'
            >
              <div style={styles.overlayInner}>
                {status === 'pending' && (
                  <React.Fragment>
                    <ClrIcon
                      className='is-solid'
                      shape='exclamation-triangle'
                      size={56}
                    />
                    <span>
                      This group has been deleted
                      <button
                        style={styles.overlayButton}
                        onClick={() => this.undo()}
                      >
                        UNDO
                      </button>
                    </span>
                  </React.Fragment>
                )}
                {status === 'hidden' && (
                  <React.Fragment>
                    <ClrIcon className='is-solid' shape='eye-hide' size={56} />
                    <span>
                      This group has been suppressed
                      <button
                        style={styles.overlayButton}
                        onClick={() => this.enable()}
                      >
                        ENABLE
                      </button>
                    </span>
                  </React.Fragment>
                )}
              </div>
            </div>
          )}
          {renaming && (
            <RenameModal
              existingNames={this.existingGroupNames}
              oldName={name || 'this group'}
              hideDescription={true}
              onCancel={() => this.setState({ renaming: false })}
              onRename={(v) => this.rename(v)}
              resourceType={ResourceType.COHORT_SEARCH_GROUP}
            />
          )}
        </React.Fragment>
      );
    }
  }
);
