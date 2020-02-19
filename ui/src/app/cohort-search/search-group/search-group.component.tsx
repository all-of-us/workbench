import {Component, Input, OnInit} from '@angular/core';
import {FormControl, FormGroup, Validators} from '@angular/forms';
import * as React from 'react';

import {DOMAIN_TYPES, PROGRAM_TYPES} from 'app/cohort-search/constant';
import {SearchGroupItem} from 'app/cohort-search/search-group-item/search-group-item.component';
import {criteriaMenuOptionsStore, initExisting, searchRequestStore, wizardStore} from 'app/cohort-search/search-state.service';
import {domainToTitle, generateId, mapGroup, typeToTitle} from 'app/cohort-search/utils';
import {integerAndRangeValidator} from 'app/cohort-search/validators';
import {Clickable} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import {reactStyles, ReactWrapperBase, withCurrentWorkspace} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {WorkspaceData} from 'app/utils/workspace-data';
import {DomainType, SearchRequest, TemporalMention, TemporalTime} from 'generated/fetch';
import {Menu} from 'primereact/menu';

const styles = reactStyles({
  menu: {
    minWidth: '5rem',
    maxWidth: '15rem',
    width: 'auto'
  },
});

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

function formatOption(option) {
  switch (option) {
    case TemporalMention.ANYMENTION:
      return 'Any Mention';
    case TemporalMention.FIRSTMENTION:
      return 'First Mention';
    case TemporalMention.LASTMENTION:
      return 'Last Mention';
    case TemporalTime.DURINGSAMEENCOUNTERAS:
      return 'During same encounter as';
    case TemporalTime.XDAYSBEFORE:
      return 'X Days before';
    case TemporalTime.XDAYSAFTER:
      return 'X Days after';
    case TemporalTime.WITHINXDAYSOF:
      return 'Within X Days of';
  }
}

interface Props {
  group: any;
  index: number;
  role: keyof SearchRequest;
  updateRequest: Function;
  workspace: WorkspaceData;
}

interface State {
  apiCallCheck: number;
  count: number;
  criteriaMenuOptions: any;
  demoOpen: boolean;
  demoMenuHover: boolean;
  error: boolean;
  loading: boolean;
  position: string;
  preventInputCalculate: boolean;
  status: string;
}

export const SearchGroup = withCurrentWorkspace()(
  class extends React.Component<Props, State> {
    groupMenu: any;
    mentionMenu: any;
    name = temporalMentions[0];
    timeForm = new FormGroup({
      inputTimeValue: new FormControl([Validators.required],
        [integerAndRangeValidator('Form', 0, 9999)]),
    });

    constructor(props: any) {
      super(props);
      this.state = {
        apiCallCheck: 0,
        count: undefined,
        criteriaMenuOptions: {programTypes: [], domainTypes: []},
        demoOpen: false,
        demoMenuHover: false,
        error: false,
        loading: false,
        position: 'bottom-left',
        preventInputCalculate: false,
        status: undefined
      };
    }

    componentDidMount(): void {
      this.timeForm.valueChanges
        .debounceTime(500)
        .distinctUntilChanged((p: any, n: any) => JSON.stringify(p) === JSON.stringify(n))
        .map(({inputTimeValue}) => inputTimeValue)
        .subscribe(this.getTimeValue);

      const {workspace: {cdrVersionId}} = this.props;
      criteriaMenuOptionsStore.filter(options => !!options[cdrVersionId]).subscribe(options => {
        this.criteriaMenuOptions = options[cdrVersionId];
        setTimeout(() => this.setDemoMenuHover());
      });
    }

    setDemoMenuHover() {
      const {group, index} = this.props;
      const {status} = this.state;
      if (typeof ResizeObserver === 'function') {
        const groupDiv = document.getElementById(group.id);
        // check that groupDiv is of type Element
        if (groupDiv && groupDiv.tagName) {
          // create observer to reposition overlays on div resize
          const ro = new ResizeObserver(() => {
            if (status === 'hidden' || status === 'pending') {
              this.setOverlayPosition();
            }
          });
          ro.observe(groupDiv);
        }
      }
      const demoItem = document.getElementById('DEMO-' + index);
      if (demoItem) {
        demoItem.addEventListener('mouseenter', () => {
          this.setState({demoOpen: true});
          setTimeout(() => {
            const demoMenu = document.getElementById('demo-menu-' + index);
            demoMenu.addEventListener('mouseenter', () => this.setState({demoMenuHover: true}));
            demoMenu.addEventListener('mouseleave', () => this.setState({demoMenuHover: false}));
          });
        });
        demoItem.addEventListener('mouseleave', () => setTimeout(() => this.setState({demoOpen: false})));
      }
    }

    getGroupCount() {
      try {
        const {group, role, workspace: {cdrVersionId}} = this.props;
        this.setState({apiCallCheck: this.state.apiCallCheck + 1});
        const localCheck = this.state.apiCallCheck;
        const mappedGroup = mapGroup(group);
        const request = {
          includes: [],
          excludes: [],
          [role]: [mappedGroup]
        };
        cohortBuilderApi().countParticipants(+cdrVersionId, request).then(count => {
          if (localCheck === this.state.apiCallCheck) {
            this.setState({count, loading: false});
          }
        }, (err) => {
          console.error(err);
          this.setState({error: true, loading: false});
        });
      } catch (error) {
        console.error(error);
        this.setState({error: true, loading: false});
      }
    }

    update = (recalculate: boolean) => {
      const {index, updateRequest} = this.props;
      // timeout prevents Angular 'value changed after checked' error
      setTimeout(() => {
        // prevent multiple total count calls when initializing multiple groups simultaneously
        // (on cohort edit or clone)
        const init = initExisting.getValue();
        if (!init || (init && index === 0)) {
          updateRequest(recalculate);
          if (init) {
            this.setState({preventInputCalculate: true});
            initExisting.next(false);
          }
        }
        if (recalculate && this.activeItems && (!this.temporal || !this.temporalError)) {
          this.setState({error: false, loading: true});
          this.getGroupCount();
        }
      });
    }

    get activeItems() {
      return this.props.group.items.some(it => it.status === 'active');
    }

    get items() {
      const {group: {items, temporal}} = this.props;
      return !temporal ? items : items.filter(it => it.temporalGroup === 0);
    }

    get temporalItems() {
      const {group: {items, temporal}} = this.props;
      return !temporal ? [] : items.filter(it => it.temporalGroup === 1);
    }

    get disableTemporal() {
      return this.items.some(it => [DomainType.PHYSICALMEASUREMENT, DomainType.PERSON, DomainType.SURVEY].includes(it.type));
    }

    get temporal() {
      return this.props.group.temporal;
    }

    get groupId() {
      return this.props.group.id;
    }

    get status() {
      return this.props.group.status;
    }

    remove() {
      triggerEvent('Delete', 'Click', 'Snowman - Delete Group - Cohort Builder');
      this.hide('pending');
      const timeoutId = setTimeout(() => {
        this.removeGroup();
      }, 10000);
      this.setGroupProperty('timeout', timeoutId);
    }

    hide(status: string) {
      triggerEvent('Suppress', 'Click', 'Snowman - Suppress Group - Cohort Builder');
      setTimeout(() => this.setOverlayPosition());
      this.setGroupProperty('status', status);
    }

    enable() {
      triggerEvent('Enable', 'Click', 'Enable - Suppress Group - Cohort Builder');
      this.setGroupProperty('status', 'active');
    }

    undo() {
      triggerEvent('Undo', 'Click', 'Undo - Delete Group - Cohort Builder');
      clearTimeout(this.props.group.timeout);
      this.enable();
    }

    removeGroup(): void {
      const {group, role} = this.props;
      const searchRequest = searchRequestStore.getValue();
      searchRequest[role] = searchRequest[role].filter(grp => grp.id !== group.id);
      searchRequestStore.next(searchRequest);
    }

    setOverlayPosition() {
      const {group} = this.props;
      const groupCard = document.getElementById(group.id);
      if (groupCard) {
        const {marginBottom, width, height} = window.getComputedStyle(groupCard);
        const overlay = document.getElementById('overlay_' + group.id);
        const styles = 'width:' + width + '; height:' + height + '; margin: -'
          + (parseFloat(height) + parseFloat(marginBottom)) + 'px 0 ' + marginBottom + ';';
        overlay.setAttribute('style', styles);
      }
    }

    get mention() {
      return this.props.group.mention;
    }

    get time() {
      return this.props.group.time;
    }

    get timeValue() {
      return this.props.group.timeValue;
    }

    launchWizard(criteria: any, tempGroup?: number) {
      const {group, role} = this.props;
      const {domain, type, standard} = criteria;
      if (tempGroup !== undefined) {
        triggerEvent('Temporal', 'Click', `${domainToTitle(domain)} - Temporal - Cohort Builder`);
      } else {
        const category = `${role === 'includes' ? 'Add' : 'Excludes'} Criteria`;
        // If domain is PERSON, list the type as well as the domain in the label
        const label = domainToTitle(domain) +
          (domain === DomainType.PERSON ? ' - ' + typeToTitle(type) : '') +
          ' - Cohort Builder';
        triggerEvent(category, 'Click', `${category} - ${label}`);
      }
      const itemId = generateId('items');
      tempGroup = tempGroup || 0;
      const item = this.initItem(itemId, domain, tempGroup);
      const fullTree = criteria.fullTree || false;
      const groupId = group.id;
      const context = {item, domain, type, standard, role, groupId, itemId, fullTree, tempGroup};
      wizardStore.next(context);
    }

    initItem(id: string, type: string, tempGroup: number) {
      return {
        id,
        type,
        searchParameters: [],
        modifiers: [],
        count: null,
        temporalGroup: tempGroup,
        status: 'active'
      };
    }

    setGroupProperty(property: string, value: any) {
      const {group, role, updateRequest} = this.props;
      const searchRequest = searchRequestStore.getValue();
      searchRequest[role] = searchRequest[role].map(grp => {
        if (grp.id === group.id) {
          grp[property] = value;
        }
        return grp;
      });
      searchRequestStore.next(searchRequest);
      updateRequest();
    }

    getTemporal(e) {
      const {checked} = e.target;
      triggerEvent('Temporal', 'Click', 'Turn On Off - Temporal - Cohort Builder');
      this.setGroupProperty('temporal', checked);
      if ((!checked && this.activeItems) || (checked && !this.temporalError)) {
        this.setState({error: false, loading: true});
        this.getGroupCount();
      }
    }

    getMentionTitle(mentionName) {
      if (mentionName !== this.props.group.mention) {
        triggerEvent(
          'Temporal',
          'Click',
          `${formatOption(mentionName)} - Temporal - Cohort Builder`
        );
        this.setGroupProperty('mention', mentionName);
        this.calculateTemporal();
      }
    }

    getTimeTitle(timeName) {
      const {group} = this.props;
      if (timeName !== this.props.group.time) {
        triggerEvent(
          'Temporal',
          'Click',
          `${formatOption(timeName)} - Temporal - Cohort Builder`
        );
        // prevents duplicate group count calls if switching from TemporalTime.DURINGSAMEENCOUNTERAS
        this.setState({preventInputCalculate: group.time === TemporalTime.DURINGSAMEENCOUNTERAS});
        this.setGroupProperty('time', timeName);
        this.calculateTemporal();
      }
    }

    getTimeValue = (value: number) => {
      // prevents duplicate group count calls if changes is triggered by rendering of input
      if (!this.state.preventInputCalculate) {
        this.setGroupProperty('timeValue', value);
        this.calculateTemporal();
      } else {
        this.setState({preventInputCalculate: false});
      }
    }

    calculateTemporal() {
      if (!this.temporalError) {
        this.setState({error: false, loading: true});
        this.getGroupCount();
      }
    }

    get groupDisableFlag() {
      return !this.temporal && !this.props.group.items.length;
    }

    get temporalError() {
      const {group: {items, time, timeValue}} = this.props;
      const counts = items.reduce((acc, it) => {
        if (it.status === 'active') {
          acc[it.temporalGroup]++;
        }
        return acc;
      }, [0, 0]);
      const inputError = time !== TemporalTime.DURINGSAMEENCOUNTERAS && (timeValue === null || timeValue < 0);
      return counts.includes(0) || inputError;
    }

    get validateInput() {
      const {group: {temporal, time}} = this.props;
      return temporal && time !== TemporalTime.DURINGSAMEENCOUNTERAS;
    }

    setMenuPosition() {
      const {index, role} = this.props;
      const id = role + index + '-button';
      const dropdown = document.getElementById(id).getBoundingClientRect();
      const position = (window.innerHeight - dropdown.bottom < 315) ? 'top-left' : 'bottom-left';
      this.setState({position});
    }

    render() {
      const {group: {id, mention, status, temporal}, index, role} = this.props;
      const groupMenuItems = [
        {label: 'Suppress group from total count', command: () => this.hide('hidden')},
        {label: 'Delete group', command: () => this.remove()},
      ];
      const mentionMenuItems = temporalMentions.map(tm => ({label: formatOption(tm), command: () => this.getMentionTitle(tm)}));
      const timeMenuItems = temporalTimes.map(tt => ({label: formatOption(tt), command: () => this.getTimeTitle(tt)}));
      return <React.Fragment>
        <div id={id} className='card bg-faded'>
          <div className='card-header'>
            <Menu style={styles.menu} appendTo={document.body} model={groupMenuItems} popup ref={el => this.groupMenu = el} />
            <Clickable style={{display: 'inline-block', paddingRight: '0.5rem'}} onClick={(event) => this.groupMenu.toggle(event)}>
              <ClrIcon shape='ellipsis-vertical' />
            </Clickable>
            Group {index + 1}
          </div>
          {temporal && <div className='card-block'>
            <Menu style={styles.menu} appendTo={document.body} model={mentionMenuItems} popup ref={el => this.mentionMenu = el} />
            <button className='time-selection-button'>
              {formatOption(mention)}
              <ClrIcon shape='caret down' />
            </button>
            <span className='of-text'> of </span>
          </div>}
          {this.items.map((item, i) => <div key={i} className='card-block search-item'>
            <SearchGroupItem role={role} groupId={id} item={item} index={i} updateGroup={() => this.update()}/>
            {status === 'active' && <div className='item-or'>OR</div>}
          </div>)}
          <div className='card-block'></div>
        </div>
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
  @Input('updateRequest') updateRequest: Props['updateRequest'];

  constructor() {
    super(SearchGroup, ['group', 'index', 'role', 'updateRequest']);
  }
}
