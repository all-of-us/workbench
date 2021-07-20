import * as fp from 'lodash/fp';
import {TieredMenu} from 'primereact/tieredmenu';
import * as React from 'react';
import {Subscription} from 'rxjs/Subscription';

import {SearchGroup} from 'app/cohort-search/search-group/search-group.component';
import {criteriaMenuOptionsStore, searchRequestStore} from 'app/cohort-search/search-state.service';
import {domainToTitle, generateId, typeToTitle} from 'app/cohort-search/utils';
import {ClrIcon} from 'app/components/icons';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, withCdrVersions, withCurrentWorkspace} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {WorkspaceData} from 'app/utils/workspace-data';
import {CdrVersionTiersResponse, CriteriaMenu, Domain, SearchRequest} from 'generated/fetch';

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
    background: colors.white,
    border: `1px solid ${colorWithWhiteness(colors.black, 0.75)}`,
    borderRadius: '0.125rem',
    color: colorWithWhiteness(colors.black, 0.45),
    cursor: 'pointer',
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
    cursor: default;
    font-size: 12px;
    font-weight: 600;
    height: auto;
    line-height: 0.75rem;
    padding-left: 0.5rem;
  }
  body .p-menuitem.menuitem-header > .p-menuitem-link:hover {
    background: ${colors.white};
  }
  body .p-tieredmenu .p-menu-separator {
    margin: 0.25rem 0;
  }
  body .p-tieredmenu .p-submenu-list {
    padding: 0.5rem 0;
    width: 10rem;
  }
`;

function mapMenuItem(item: CriteriaMenu) {
  const {category, domainId, group, id, name, sortOrder, type} = item;
  return {
    category,
    children: null,
    domain: domainId,
    group,
    id,
    name,
    sortOrder,
    standard: domainId === Domain.VISIT.toString() ? true : null,
    type
  };
}

interface Props {
  groups: Array<any>;
  setSearchContext: (context: any) => void;
  role: keyof SearchRequest;
  updated: number;
  updateRequest: Function;
  workspace: WorkspaceData;
  cdrVersionTiersResponse: CdrVersionTiersResponse;
}

interface State {
  criteriaMenuOptions: Array<any>;
  index: number;
  loadingMenuOptions: boolean;
}
const SearchGroupList = fp.flow(withCurrentWorkspace(), withCdrVersions())(
  class extends React.Component<Props, State> {
    private criteriaMenu: any;
    private subscription: Subscription;
    constructor(props: Props) {
      super(props);
      this.state = {
        criteriaMenuOptions: [],
        index: 0,
        loadingMenuOptions: false
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
        this.subscription.add(
          searchRequestStore.subscribe(sr => this.setState({index: sr.includes.length + 1})));
      }
    }

    componentWillUnmount(): void {
      this.subscription.unsubscribe();
    }

    getMenuOptions() {
      this.setState({loadingMenuOptions: true});
      const {workspace: {id, cdrVersionId, namespace}} = this.props;
      const criteriaMenuOptions = criteriaMenuOptionsStore.getValue();
      cohortBuilderApi().findCriteriaMenu(namespace, id, 0).then(async res => {
        const menuOptions = await Promise.all(res.items.map(async item => {
          const option = mapMenuItem(item);
          if (option.group) {
            const children = await cohortBuilderApi().findCriteriaMenu(namespace, id, option.id);
            option.children = children.items.map(mapMenuItem);
          }
          return option;
        }));
        criteriaMenuOptions[cdrVersionId] = Object.values(fp.groupBy('category', menuOptions));
        criteriaMenuOptionsStore.next(criteriaMenuOptions);
        this.setState({loadingMenuOptions: false});
      });
    }

    mapCriteriaMenuItem(domain: any, temporalGroup: number) {
      if (!!domain.children) {
        return {label: domain.name, items: domain.children.map((dt) => this.mapCriteriaMenuItem(dt, temporalGroup))};
      }
      return {label: domain.name, command: () => this.launchSearch(domain)};
    }

    get criteriaMenuItems() {
      const {criteriaMenuOptions, loadingMenuOptions} = this.state;
      if (loadingMenuOptions) {
        return [{icon: 'pi pi-spin pi-spinner'}];
      } else {
        let menuItems = [];
        criteriaMenuOptions.forEach((options, index) => {
          menuItems = [
            ...menuItems,
            {label: options[0].category, className: 'menuitem-header'},
            ...options.map((dt) => this.mapCriteriaMenuItem(dt, 0))
          ];
          if (index < options.length - 1) {
            menuItems.push({separator: true});
          }
        });
        return menuItems;
      }
    }

    launchSearch(criteria: any) {
      this.criteriaMenu.hide();
      const {role} = this.props;
      const {domain, type, standard} = criteria;
      const category = `${role === 'includes' ? 'Add' : 'Excludes'} Criteria`;
    // If domain is PERSON, list the type as well as the domain in the label
      const label = domainToTitle(domain) +
      (domain === Domain.PERSON ? ' - ' + typeToTitle(type) : '') +
      ' - Cohort Builder';
      triggerEvent(category, 'Click', `${category} - ${label}`);
      let context: any;
      const itemId = generateId('items');
      const groupId = null;
      const item = initItem(itemId, domain);
      context = {item, domain, type, standard, role, groupId};
      this.props.setSearchContext(context);
    }

    render() {
      const {groups, setSearchContext, role, updated, updateRequest} = this.props;
      const {index} = this.state;
      return <React.Fragment>
      <style>{css}</style>
      <div style={{display: 'flex', alignItems: 'center'}}>
        <h2 style={styles.listHeader}>
          {role === 'excludes' && <span>And</span>} {role.slice(0, -1)} Participants
        </h2>
      </div>
      {groups.map((group, g) => <div key={g} data-test-id={`${role}-search-group`}>
        <SearchGroup group={group}
                     index={g + index}
                     setSearchContext={setSearchContext}
                     role={role}
                     updated={updated}
                     updateRequest={updateRequest}/>
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
  });

export {
  SearchGroupList
};
