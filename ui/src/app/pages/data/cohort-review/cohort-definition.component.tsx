import * as React from 'react';
import { RouteComponentProps, withRouter } from 'react-router-dom';

import {
  CohortReview,
  Criteria,
  CriteriaType,
  Domain,
  Modifier,
  ModifierType,
  Operator,
  SearchGroup,
  SearchParameter,
} from 'generated/fetch';

import { domainToTitle } from 'app/cohort-search/utils';
import { cohortBuilderApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { MatchParams } from 'app/utils/stores';

const css = `
  @media print{
    .page-break {
      page-break-inside: avoid;
    }
  }
`;

const styles = reactStyles({
  defTitle: {
    fontSize: '16px',
    fontWeight: 600,
    color: colors.primary,
  },
  wrapper: {
    width: '100%',
    display: 'flex',
    justifyContent: 'start',
  },
  exclude: {
    backgroundColor: colorWithWhiteness(colors.light, -0.5),
    borderRadius: '1rem',
    width: '4.5rem',
    height: '2.2rem',
    lineHeight: '2.2rem',
    textAlign: 'center',
  },
  andCircle: {
    backgroundColor: colorWithWhiteness(colors.light, -0.5),
    borderRadius: '50%',
    width: '2.5rem',
    height: '2.5rem',
    lineHeight: '2.5rem',
    textAlign: 'center',
  },
  groupBackground: {
    backgroundColor: colorWithWhiteness(colors.light, -0.5),
    color: colors.primary,
    padding: '0.3rem 0.6rem',
    margin: '0.7rem 0rem',
    display: 'inline-block',
  },
});

const showParameterType = (domain: string) =>
  domain === Domain.CONDITION.toString() &&
  domain === Domain.DRUG.toString() &&
  domain === Domain.PROCEDURE.toString();

const showParameterParent = (domain: string) =>
  domain !== Domain.DRUG.toString() && domain !== Domain.SURVEY.toString();

const modifierTypeDisplay = (modifierType: ModifierType) => {
  switch (modifierType) {
    case ModifierType.AGEATEVENT:
      return 'Age at Event';
    case ModifierType.ENCOUNTERS:
      return 'During Visit Type - ';
    case ModifierType.EVENTDATE:
      return 'Event Date';
    case ModifierType.NUMOFOCCURRENCES:
      return 'Num of Occurrences';
  }
};

const modifierOperatorDisplay = (operator: Operator) => {
  switch (operator) {
    case Operator.BETWEEN:
      return 'Between';
    case Operator.EQUAL:
      return '=';
    case Operator.GREATERTHANOREQUALTO:
      return '>=';
    case Operator.IN:
      return '';
    case Operator.LESSTHANOREQUALTO:
      return '<=';
  }
};

interface ParamItem {
  display: string;
  domain: Domain;
}

interface Props extends RouteComponentProps<MatchParams> {
  review: CohortReview;
}

interface State {
  definition: any;
  visits: Array<Criteria>;
}

export const CohortDefinition = withRouter(
  class extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props);
      this.state = { definition: undefined, visits: undefined };
    }

    async componentDidMount() {
      const { ns, wsid } = this.props.match.params;
      const visits = await cohortBuilderApi().findCriteriaBy(
        ns,
        wsid,
        Domain.VISIT.toString(),
        CriteriaType.VISIT.toString()
      );
      this.setState({ visits: visits.items }, () => this.mapDefinition());
    }

    mapDefinition() {
      const {
        review: { cohortDefinition },
      } = this.props;
      const def = JSON.parse(cohortDefinition);
      const definition = ['includes', 'excludes'].reduce((acc, role) => {
        if (def[role].length) {
          const roleObj = { role, groups: [] };
          def[role].forEach((group) => {
            roleObj.groups.push(this.mapGroup(group));
          });
          acc.push(roleObj);
        }
        return acc;
      }, []);
      this.setState({ definition });
    }

    mapGroup(group: SearchGroup) {
      const mappedItems = [];
      group.items.forEach(({ modifiers, searchParameters, type }) => {
        const domain = Domain[type];
        if (
          (domain === Domain.CONDITION || domain === Domain.PROCEDURE) &&
          searchParameters.some((param) => param.domain !== domain)
        ) {
          const multipleDomains = searchParameters.reduce((acc, param) => {
            const index = acc.findIndex(
              (list) => list[0].domain === param.domain
            );
            if (index > -1) {
              acc[index].push(param);
            } else {
              acc.push([param]);
            }
            return acc;
          }, []);
          multipleDomains.forEach((params) =>
            mappedItems.push(this.mapParams(params, modifiers))
          );
        } else {
          mappedItems.push(this.mapParams(searchParameters, modifiers));
        }
      });
      return mappedItems;
    }

    mapParams(parameters: Array<SearchParameter>, modifiers: Array<Modifier>) {
      const groupedParameters = parameters.reduce(
        (parameterList, parameter) => {
          const { domain, group, name, type } = parameter;
          const displayName =
            group && showParameterParent(domain) ? `Parent ${name}` : name;
          if (parameterList[type]) {
            parameterList[type].display += `, ${displayName}`;
          } else {
            parameterList[type] = {
              domain,
              display: `${domainToTitle(domain)}${
                showParameterType(domain) ? ` | ${type}` : ''
              } | ${displayName}`,
            };
          }
          return parameterList;
        },
        {} as ParamItem
      );
      return modifiers.length > 0
        ? Object.values(groupedParameters).map((param) => {
            param.display += ` | ${this.getModifierDisplay(modifiers)}`;
            return param;
          })
        : Object.values(groupedParameters);
    }

    getModifierDisplay(modifiers: Array<Modifier>) {
      const modifiersDisplay = modifiers.reduce((modifiersArray, modifier) => {
        const operands =
          modifier.name === ModifierType.ENCOUNTERS
            ? this.state.visits.find(
                (visit) => visit.conceptId.toString() === modifier.operands[0]
              )?.name || ''
            : modifier.operands.join(' & ');
        modifiersArray.push(`${modifierTypeDisplay(
          modifier.name
        )} ${modifierOperatorDisplay(modifier.operator)}
              ${operands}`);
        return modifiersArray;
      }, []);
      return modifiersDisplay.join(', ');
    }

    render() {
      const { definition } = this.state;
      return (
        <div style={{ marginTop: '1rem', marginBottom: '1rem' }}>
          <style>{css}</style>
          <div style={styles.defTitle}>Cohort Definition</div>
          {definition?.map((role, r) => (
            <React.Fragment key={r}>
              {role.role === 'excludes' && (
                <div className='page-break' style={styles.wrapper}>
                  <div style={styles.exclude}>EXCLUDING</div>
                </div>
              )}
              {role.groups.map((group, g) => (
                <React.Fragment key={g}>
                  {g > 0 && (
                    <div className='page-break' style={styles.wrapper}>
                      <div style={styles.andCircle}>AND</div>
                    </div>
                  )}
                  <div style={styles.groupBackground}>
                    {group.map((item, i) => (
                      <React.Fragment key={i}>
                        {i > 0 && <div>OR</div>}
                        {item.map((parameter, p) => (
                          <React.Fragment key={p}>
                            {p > 0 && <div>OR</div>}
                            <div>{parameter.display}</div>
                          </React.Fragment>
                        ))}
                      </React.Fragment>
                    ))}
                  </div>
                </React.Fragment>
              ))}
            </React.Fragment>
          ))}
        </div>
      );
    }
  }
);
