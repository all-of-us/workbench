import * as React from 'react';
import * as fp from 'lodash/fp';

import { CohortDefinition, CriteriaMenu, Domain } from 'generated/fetch';

import { CohortCriteriaMenu } from 'app/pages/data/cohort/cohort-criteria-menu';
import { SearchGroup } from 'app/pages/data/cohort/search-group';
import {
  criteriaMenuOptionsStore,
  searchRequestStore,
} from 'app/pages/data/cohort/search-state.service';
import {
  domainToTitle,
  generateId,
  typeToTitle,
} from 'app/pages/data/cohort/utils';
import { cohortBuilderApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles, withCurrentWorkspace } from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { currentWorkspaceStore } from 'app/utils/navigation';
import { WorkspaceData } from 'app/utils/workspace-data';
import { Subscription } from 'rxjs/Subscription';

export function initItem(id: string, type: string) {
  return {
    id,
    type,
    searchParameters: [],
    modifiers: [],
    temporalGroup: 0,
    isRequesting: false,
    status: 'active',
  };
}

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
  circle: {
    backgroundColor: 'rgb(226, 226, 233)',
    borderRadius: '50%',
    width: '3.75rem',
    height: '3.75rem',
    marginBottom: '0.75rem',
    lineHeight: '3.75rem',
    textAlign: 'center',
  },
  circleWrapper: {
    width: '100%',
    display: 'flex',
    justifyContent: 'center',
  },
  listHeader: {
    fontSize: '16px',
    fontWeight: 'bold',
    margin: 0,
    textTransform: 'capitalize',
  },
});

function mapMenuItem(item: CriteriaMenu) {
  const { category, domainId, group, id, name, parentId, sortOrder, type } =
    item;
  return {
    category,
    children: null,
    domain: domainId,
    group,
    id,
    name,
    selectedSurvey:
      domainId === Domain.SURVEY.toString() && parentId !== 0 ? name : null,
    sortOrder,
    standard: domainId === Domain.VISIT.toString() ? true : null,
    type,
  };
}

interface Props {
  groups: Array<any>;
  setSearchContext: (context: any) => void;
  role: keyof CohortDefinition;
  updated: number;
  updateRequest: Function;
  workspace: WorkspaceData;
}

interface State {
  criteriaMenuOptions: Array<any>;
  index: number;
}
const SearchGroupList = fp.flow(withCurrentWorkspace())(
  class extends React.Component<Props, State> {
    private subscription: Subscription;
    constructor(props: Props) {
      super(props);
      this.state = {
        criteriaMenuOptions: [],
        index: 0,
      };
    }

    componentDidMount(): void {
      const { role } = this.props;
      const { cdrVersionId } = currentWorkspaceStore.getValue();
      this.subscription = criteriaMenuOptionsStore.subscribe((options) => {
        if (role === 'includes' && !options[cdrVersionId]) {
          this.getMenuOptions();
        } else if (!!options[cdrVersionId]) {
          this.setState({ criteriaMenuOptions: options[cdrVersionId] });
        }
      });
      if (role === 'excludes') {
        this.subscription.add(
          searchRequestStore.subscribe((sr) =>
            this.setState({ index: sr.includes.length + 1 })
          )
        );
      }
    }

    componentWillUnmount(): void {
      this.subscription.unsubscribe();
    }

    getMenuOptions() {
      const {
        workspace: { cdrVersionId, id, namespace },
      } = this.props;
      const criteriaMenuOptions = criteriaMenuOptionsStore.getValue();
      cohortBuilderApi()
        .findCriteriaMenu(namespace, id, 0)
        .then(async (res) => {
          const menuOptions = await Promise.all(
            res.items.map(async (item) => {
              const option = mapMenuItem(item);
              if (option.group) {
                const children = await cohortBuilderApi().findCriteriaMenu(
                  namespace,
                  id,
                  option.id
                );
                option.children = children.items.map(mapMenuItem);
              }
              return option;
            })
          );
          criteriaMenuOptions[cdrVersionId] = Object.values(
            fp.groupBy('category', menuOptions)
          );
          criteriaMenuOptionsStore.next(criteriaMenuOptions);
        });
    }

    launchSearch(criteria: any, searchTerms?: string) {
      const { role } = this.props;
      const { domain, selectedSurvey, type, standard } = criteria;
      // If domain is PERSON, list the type as well as the domain in the label
      const label = `Enter ${domainToTitle(domain)}${
        domain === Domain.PERSON ? ' - ' + typeToTitle(type) : ''
      } search - ${role === 'includes' ? 'Include' : 'Exclude'} Criteria`;
      AnalyticsTracker.CohortBuilder.LaunchSearch(label);

      const itemId = generateId('items');
      const groupId = null;
      const item = initItem(itemId, domain);
      const context = {
        item,
        domain,
        type,
        searchTerms,
        standard,
        role,
        groupId,
        selectedSurvey,
      };
      this.props.setSearchContext(context);
    }

    render() {
      const { groups, setSearchContext, role, updated, updateRequest } =
        this.props;
      const { criteriaMenuOptions, index } = this.state;
      return (
        <React.Fragment>
          <div style={{ display: 'flex', alignItems: 'center' }}>
            <h2 style={styles.listHeader}>
              {role === 'excludes' && <span>And</span>} {role.slice(0, -1)}{' '}
              Participants
            </h2>
          </div>
          {groups.map((group, g) => (
            <div key={g} data-test-id={`${role}-search-group`}>
              <SearchGroup
                group={group}
                groupIndex={g + index}
                role={role}
                roleIndex={g}
                setSearchContext={setSearchContext}
                updated={updated}
                updateRequest={updateRequest}
              />
              <div style={styles.circleWrapper}>
                <div style={styles.circle}>AND</div>
              </div>
            </div>
          ))}
          <div style={styles.card}>
            {/* Group Header */}
            <div style={styles.cardHeader}>
              <div style={{ marginLeft: '1.725rem' }}>
                Group {groups.length + index + 1}
              </div>
            </div>
            <CohortCriteriaMenu
              launchSearch={(criteria, searchTerms) =>
                this.launchSearch(criteria, searchTerms)
              }
              menuOptions={criteriaMenuOptions}
            />
          </div>
        </React.Fragment>
      );
    }
  }
);

export { SearchGroupList };
