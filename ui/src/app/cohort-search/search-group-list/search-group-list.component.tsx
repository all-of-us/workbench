import {Component, Input} from '@angular/core';
import {TieredMenu} from 'primereact/tieredmenu';
import * as React from 'react';
import {Subscription} from 'rxjs/Subscription';

import {DOMAIN_TYPES, PROGRAM_TYPES} from 'app/cohort-search/constant';
import {SearchGroup} from 'app/cohort-search/search-group/search-group.component';
import {criteriaMenuOptionsStore, searchRequestStore, wizardStore} from 'app/cohort-search/search-state.service';
import {domainToTitle, generateId, typeToTitle} from 'app/cohort-search/utils';
import {ClrIcon} from 'app/components/icons';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {CriteriaType, DomainType, SearchRequest} from 'generated/fetch';

function initItem(id: string, type: string) {
  return {
    id,
    type,
    searchParameters: [],
    modifiers: [],
    temporalGroup: 0,
    isRequesting: false,
    status: 'active'
  };
}

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
  circle: {
    backgroundColor: 'rgb(226, 226, 233)',
    borderRadius: '50%',
    width: '2.5rem',
    height: '2.5rem',
    marginBottom: '0.5rem',
    lineHeight: '2.5rem',
    textAlign: 'center',
  },
  circleWrapper: {
    width: '100%',
    display: 'flex',
    justifyContent: 'center'
  },
  listHeader: {
    fontSize: '16px',
    fontWeight: 'bold',
    margin: 0,
    textTransform: 'capitalize'
  },
  menu: {
    maxWidth: '15rem',
    minWidth: '5rem',
    width: 'auto'
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
});

const css = `
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

interface Props {
  groups: Array<any>;
  role: keyof SearchRequest;
  updated: number;
  updateRequest: Function;
}

interface State {
  criteriaMenuOptions: any;
  index: number;
}

export class SearchGroupList extends React.Component<Props, State> {
  private criteriaMenu: any;
  private subscription: Subscription;
  constructor(props: Props) {
    super(props);
    this.state = {
      criteriaMenuOptions: {programTypes: [], domainTypes: []},
      index: 0,
    };
  }

  componentDidMount(): void {
    const {role} = this.props;
    const {cdrVersionId} = currentWorkspaceStore.getValue();
    this.subscription = criteriaMenuOptionsStore.subscribe(options => {
      if (role === 'includes' && !options[cdrVersionId]) {
        this.getMenuOptions();
      } else if (!!options[cdrVersionId]) {
        this.setState({criteriaMenuOptions: options[cdrVersionId]});
      }
    });
    if (role === 'excludes') {
      this.subscription.add(searchRequestStore.subscribe(sr => this.setState({index: sr.includes.length + 1})));
    }
  }

  componentWillUnmount(): void {
    this.subscription.unsubscribe();
  }

  getMenuOptions() {
    const {cdrVersionId} = currentWorkspaceStore.getValue();
    const criteriaMenuOptions = criteriaMenuOptionsStore.getValue();
    cohortBuilderApi().findCriteriaMenuOptions(+cdrVersionId).then(res => {
      criteriaMenuOptions[cdrVersionId] = res.items.reduce((acc, opt) => {
        const {domain, types} = opt;
        if (PROGRAM_TYPES.includes(DomainType[domain])) {
          const option = {
            name: domainToTitle(domain),
            domain,
            type: types[0].type,
            standard: types[0].standardFlags[0].standard,
            order: PROGRAM_TYPES.indexOf(DomainType[domain])};
          if (domain === DomainType[DomainType.PERSON]) {
            option['children'] = types
            .filter(subopt => subopt.type !== CriteriaType[CriteriaType.DECEASED])
            .map(subopt => ({name: typeToTitle(subopt.type), domain, type: subopt.type}));
          }
          acc.programTypes.push(option);
        }
        if (DOMAIN_TYPES.includes(DomainType[domain])) {
          acc.domainTypes.push({
            name: domainToTitle(domain),
            domain,
            type: types[0].type,
            standard: types[0].standardFlags[0].standard,
            order: DOMAIN_TYPES.indexOf(DomainType[domain])});
        }
        return acc;
      }, {programTypes: [], domainTypes: []});
      criteriaMenuOptions[cdrVersionId].programTypes.sort((a, b) => a.order - b.order);
      criteriaMenuOptions[cdrVersionId].domainTypes.sort((a, b) => a.order - b.order);
      criteriaMenuOptionsStore.next(criteriaMenuOptions);
    });
  }

  mapCriteriaMenuItem(domain: any, temporalGroup: number) {
    if (!!domain.children) {
      return {label: domain.name, items: domain.children.map((dt) => this.mapCriteriaMenuItem(dt, temporalGroup))};
    }
    return {label: domain.name, command: () => this.launchWizard(domain)};
  }

  get criteriaMenuItems() {
    const {criteriaMenuOptions: {domainTypes, programTypes}} = this.state;
    return [
      {label: 'Program Data', className: 'menuitem-header'},
      ...programTypes.map((dt) => this.mapCriteriaMenuItem(dt, 0)),
      {separator: true},
      {label: 'Domains', className: 'menuitem-header'},
      ...domainTypes.map((dt) => this.mapCriteriaMenuItem(dt, 0))
    ];
  }

  launchWizard(criteria: any) {
    this.criteriaMenu.hide();
    const {role} = this.props;
    const {domain, type, standard} = criteria;
    const category = `${role === 'includes' ? 'Add' : 'Excludes'} Criteria`;
    // If domain is PERSON, list the type as well as the domain in the label
    const label = domainToTitle(domain) +
      (domain === DomainType.PERSON ? ' - ' + typeToTitle(type) : '') +
      ' - Cohort Builder';
    triggerEvent(category, 'Click', `${category} - ${label}`);
    let context: any;
    const itemId = generateId('items');
    const groupId = null;
    const item = initItem(itemId, domain);
    context = {item, domain, type, standard, role, groupId, itemId};
    wizardStore.next(context);
  }

  render() {
    const {groups, role, updated, updateRequest} = this.props;
    const {index} = this.state;
    return <React.Fragment>
      <style>{css}</style>
      <div style={{display: 'flex', alignItems: 'center'}}>
        <h2 style={styles.listHeader}>
          {role === 'excludes' && <span>And</span>} {role.slice(0, -1)} Participants
        </h2>
      </div>
      {groups.map((group, g) => <div key={g}>
        <SearchGroup group={group} index={g + index} role={role} updated={updated} updateRequest={updateRequest}/>
        <div style={styles.circleWrapper}>
          <div style={styles.circle}>AND</div>
        </div>
      </div>)}
      <div style={styles.card}>
        {/* Group Header */}
        <div style={styles.cardHeader}>
          <div style={{marginLeft: '1.15rem'}}>Group {groups.length + index + 1}</div>
        </div>
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
      </div>
    </React.Fragment>;
  }
}

@Component({
  selector: 'app-list-search-group-list',
  template: '<div #root></div>'
})
export class SearchGroupListComponent extends ReactWrapperBase {
  @Input('role') role: Props['role'];
  @Input('groups') groups: Props['groups'];
  @Input('updated') updated: Props['updated'];
  @Input('updateRequest') updateRequest: Props['updateRequest'];

  constructor() {
    super(SearchGroupList, ['groups', 'role', 'updated', 'updateRequest']);
  }
}
