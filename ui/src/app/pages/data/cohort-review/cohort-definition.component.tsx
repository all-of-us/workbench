import { domainToTitle } from 'app/cohort-search/utils';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import { CohortReview, CriteriaType, Domain } from 'generated/fetch';
import * as React from 'react';

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

export class CohortDefinition extends React.Component<
  { review: CohortReview },
  { definition: any }
> {
  constructor(props: any) {
    super(props);
    this.state = { definition: null };
  }

  componentDidMount() {
    this.mapDefinition();
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

  mapGroup(group: any) {
    const mappedItems = [];
    group.items.forEach(({ modifiers, searchParameters, type }) => {
      const domain = type as Domain;
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

  mapParams(params: Array<any>, mod) {
    const groupedData = this.getGroupedData(params, 'type');
    let paramList;
    if (mod.length) {
      paramList = this.getModifierFormattedData(groupedData, params, mod);
    } else {
      paramList = this.getOtherTreeFormattedData(groupedData, params);
    }
    return paramList;
  }

  getModifierFormattedData(groupedData, params, modifiers) {
    const modArray = params.map(({ domain, type }) => {
      const typeMatched = groupedData.find((matched) => matched.group === type);
      const modifierName = modifiers.reduce((acc, m) => {
        const concatOperand = m.operands.reduce((final, o) => {
          return final !== '' ? `${final} & ${o}` : `${final} ${o}`;
        }, '');
        return acc !== ''
          ? `${acc} ,  ${this.operatorConversion(m.name)}
            ${this.operatorConversion(m.operator)}
            ${concatOperand}`
          : `${this.operatorConversion(m.name)} ${this.operatorConversion(
              m.operator
            )}
            ${concatOperand}`;
      }, '');
      return domain === Domain.CONDITION || domain === Domain.PROCEDURE
        ? {
            items: `${domainToTitle(domain)} | ${type} | ${
              typeMatched.customString
            } | ${modifierName}`,
            domain,
          }
        : {
            items: `${domainToTitle(domain)} | ${
              typeMatched.customString
            } | ${modifierName}`,
            domain,
          };
    });
    return this.removeDuplicates(modArray);
  }

  getOtherTreeFormattedData(groupedData, params) {
    const noModArray = params.map(({ domain, name, type }) => {
      const typeMatched = groupedData.find((matched) => matched.group === type);
      if (domain === Domain.PERSON) {
        return {
          items:
            type === CriteriaType.DECEASED
              ? `${domainToTitle(domain)}
                      | ${name}`
              : `${domainToTitle(domain)}
                      | ${this.operatorConversion(type)} | ${name}`,
          domain: domain,
        };
      } else if (domain === Domain.VISIT) {
        return {
          items: `${domainToTitle(domain)} | ${typeMatched.customString}`,
          domain,
        };
      } else {
        return domain === Domain.CONDITION || domain === Domain.PROCEDURE
          ? {
              items: `${domainToTitle(domain)} | ${type} | ${
                typeMatched.customString
              }`,
              domain,
            }
          : {
              items: `${domainToTitle(domain)} | ${typeMatched.customString}`,
              domain,
            };
      }
    });
    return this.removeDuplicates(noModArray);
  }

  // utils
  getGroupedData(p, t) {
    const groupedData = p.reduce((acc, i) => {
      const key = i[t];
      acc[key] = acc[key] || { data: [] };
      acc[key].data.push(i);
      return acc;
    }, {});
    return Object.keys(groupedData).map((k) => {
      return Object.assign(
        {},
        {
          group: k,
          data: groupedData[k].data,
          customString: groupedData[k].data.reduce((acc, d) => {
            return this.getFormattedString(acc, d);
          }, ''),
        }
      );
    });
  }

  getFormattedString(acc, d) {
    if (d.domain === Domain.DRUG) {
      if (d.group === false) {
        return acc === '' ? `RXNORM | ${d.name}` : `${acc}, ${d.name}`;
      } else {
        return acc === '' ? `ATC | ${d.name}` : `${acc}, ${d.name}`;
      }
    } else if (
      d.domain === Domain.PHYSICALMEASUREMENT ||
      d.domain === Domain.VISIT
    ) {
      return acc === '' ? `${d.name}` : `${acc}, ${d.name}`;
    } else if (d.domain === Domain.SURVEY) {
      if (!d.group) {
        return acc === '' ? `${d.name}` : `${acc}, ${d.name}`;
      } else if (d.group && !d.conceptId) {
        return acc === '' ? `Survey - ${d.name}` : `${acc}, Survey - ${d.name}`;
      } else {
        return acc === '' ? `${d.name}` : `${acc}, ${d.name}`;
      }
    } else {
      if (d.group === false) {
        return acc === '' ? d.name : `${acc}, ${d.name}`;
      } else {
        return acc === '' ? `Parent ${d.name}` : `${acc}, Parent ${d.name}`;
      }
    }
  }

  removeDuplicates(arr) {
    return arr.filter(
      (thing, index, self) =>
        index ===
        self.findIndex(
          (t) => t.items === thing.items && t.domain === thing.domain
        )
    );
  }

  operatorConversion(operator) {
    switch (operator) {
      case 'GREATER_THAN_OR_EQUAL_TO':
        return '>=';
      case 'LESS_THAN_OR_EQUAL_TO':
        return '<=';
      case 'EQUAL':
        return '=';
      case 'BETWEEN':
        return 'Between';
      case 'ETHNICITY':
        return 'Ethnicity';
      case 'RACE':
        return 'Race';
      case 'AGE':
        return 'Age';
      case 'GENDER':
        return 'Gender';
      case 'AGE_AT_EVENT':
        return 'Age at Event';
      case 'NUM_OF_OCCURRENCES':
        return 'Num of Occurrences';
    }
  }

  render() {
    const { definition } = this.state;
    return (
      <div style={{ marginTop: '1rem', marginBottom: '1rem' }}>
        <style>{css}</style>
        <div style={styles.defTitle}>Cohort Definition</div>
        {definition?.map((group, g) => (
          <React.Fragment key={g}>
            {group.role === 'excludes' && (
              <div className='page-break' style={styles.wrapper}>
                <div style={styles.exclude}>EXCLUDING</div>
              </div>
            )}
            {group.groups.map((item, i) => (
              <React.Fragment key={i}>
                {i > 0 && (
                  <div className='page-break' style={styles.wrapper}>
                    <div style={styles.andCircle}>AND</div>
                  </div>
                )}
                <div style={styles.groupBackground}>
                  {item.map((param, p) => (
                    <React.Fragment key={p}>
                      {p > 0 && <div>OR</div>}
                      <div>
                        {param.map((crit, c) => (
                          <React.Fragment key={c}>
                            {c > 0 && (
                              <React.Fragment>
                                {crit.domain === Domain.PERSON ? (
                                  <div>AND</div>
                                ) : (
                                  <div>OR</div>
                                )}
                              </React.Fragment>
                            )}
                            <div>{crit.items}</div>
                          </React.Fragment>
                        ))}
                      </div>
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
