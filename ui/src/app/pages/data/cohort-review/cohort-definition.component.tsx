import {domainToTitle} from 'app/cohort-search/utils';
import {cohortReviewStore} from 'app/services/review-state.service';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {CriteriaType, DomainType} from 'generated/fetch';
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
    color: colors.primary
  },
  wrapper: {
    width: '100%',
    display: 'flex',
    justifyContent: 'start'
  },
  exclude: {
    backgroundColor: colorWithWhiteness(colors.light, -.5),
    borderRadius: '1rem',
    width: '4.5rem',
    height: '2.2rem',
    lineHeight: '2.2rem',
    textAlign: 'center'
  },
  andCircle: {
    backgroundColor: colorWithWhiteness(colors.light, -.5),
    borderRadius: '50%',
    width: '2.5rem',
    height: '2.5rem',
    lineHeight: '2.5rem',
    textAlign: 'center'
  },
  groupBackground: {
    backgroundColor: colorWithWhiteness(colors.light, -.5),
    color: colors.primary,
    padding: '0.3rem 0.6rem',
    margin: '0.7rem 0rem',
    display: 'inline-block'
  }
});

export class CohortDefinition extends React.Component<{}, {definition: any}> {
  constructor(props: any) {
    super(props);
    this.state = {definition: null};
  }

  componentDidMount() {
    this.mapDefinition();
  }

  mapDefinition() {
    const review = cohortReviewStore.getValue();
    const def = JSON.parse(review.cohortDefinition);
    const definition = ['includes', 'excludes'].reduce((acc, role) => {
      if (def[role].length) {
        const roleObj = {role, groups: []};
        def[role].forEach(group => {
          roleObj.groups.push(this.mapGroup(group));
        });
        acc.push(roleObj);
      }
      return acc;
    }, []);
    this.setState({definition});
  }

  mapGroup(group: any) {
    return group.items.map(item => {
      return this.mapParams(item.type, item.searchParameters, item.modifiers);
    });
  }

  mapParams(domain: string, params: Array<any>, mod) {
    const groupedData = domain === DomainType[DomainType.DRUG]
      ? this.getGroupedData(params, 'group')
      : this.getGroupedData(params, 'domain');
    if (mod.length) {
      return this.getModifierFormattedData(groupedData, params, mod, domain);
    } else {
      return this.getOtherTreeFormattedData(groupedData, params, domain);
    }
  }

  getModifierFormattedData(groupedData, params, mod, domain) {
    let typeMatched;
    const modArray =  params.map(eachParam => {
      if (eachParam.domain === DomainType.DRUG) {
        typeMatched = groupedData.find( matched => matched.group === eachParam.group.toString());
      } else {
        typeMatched = groupedData.find( matched => matched.group === eachParam.domain);
      }
      let name;
      name = mod.reduce((acc, m) => {
        const concatOperand = m.operands.reduce((final, o) => {
          return final !== '' ? `${final} & ${o}` : `${final} ${o}`;
        } , '');
        return acc !== '' ?
          `${acc} ,  ${this.operatorConversion(m.name)}
            ${this.operatorConversion(m.operator)}
            ${concatOperand}`
          :
          `${this.operatorConversion(m.name)} ${this.operatorConversion(m.operator)}
            ${concatOperand}`;
      }, '');
      return {
        items: `${domainToTitle(domain)} | ${eachParam.domain} |
          ${typeMatched.customString} | ${name}`,
        domain: eachParam.domain
      };
    });
    return this.removeDuplicates(modArray);
  }

  getOtherTreeFormattedData(groupedData, params, domain) {
    let typeMatched;
    const noModArray = params.map(param => {
      if (param.domain === DomainType.DRUG) {
        typeMatched = groupedData.find( matched => matched.group === param.group.toString());
      } else {
        typeMatched = groupedData.find( matched => matched.group === param.domain);
      }
      if (param.domain === DomainType.PERSON) {
        return {items: param.type === CriteriaType.DECEASED ? `${domainToTitle(domain)}
                      | ${param.name}` :
            `${domainToTitle(domain)}
                      | ${this.operatorConversion(param.type)} | ${param.name}`,
          domain: param.domain};
      } else if (param.domain === DomainType.VISIT) {
        return {items: `${domainToTitle(domain)} | ${typeMatched.customString}`,
          domain: param.domain};
      } else {
        return domain === DomainType.CONDITION || domain === DomainType.PROCEDURE ?
        {items: `${domainToTitle(domain)} | ${param.type} | ${typeMatched.customString}`,
          domain: param.domain} :
        {items: `${domainToTitle(domain)} | ${typeMatched.customString}`,
          domain: param.domain};
      }
    });
    return this.removeDuplicates(noModArray);
  }


// utils
  getGroupedData(p, t) {
    const groupedData = p.reduce((acc, i) => {
      const key = i[t];
      acc[key] = acc[key] || { data: []};
      acc[key].data.push(i);
      return acc;
    }, {});
    return Object.keys(groupedData).map(k => {
      return Object.assign({}, {
        group: k,
        data: groupedData[k].data,
        customString : groupedData[k].data.reduce((acc, d) => {
          return this.getFormattedString(acc, d);
        }, '')
      });
    });
  }

  getFormattedString(acc, d) {
    if (d.domain === DomainType.DRUG) {
      if (d.group === false) {
        return acc === '' ? `RXNORM | ${d.value}` : `${acc}, ${d.value}`;
      } else {
        return acc === '' ? `ATC | ${d.value}` : `${acc}, ${d.value}`;
      }
    } else if (d.domain === DomainType.PHYSICALMEASUREMENT || d.domain === DomainType.VISIT) {
      return acc === '' ? `${d.name}` : `${acc}, ${d.name}`;
    } else if (d.domain === DomainType.SURVEY) {
      if (!d.group) {
        return  acc === '' ? `${d.name}` :
          `${acc}, ${d.name}`;
      } else if (d.group && !d.conceptId) {
        return acc === '' ? `Survey - ${d.name}` :
          `${acc}, Survey - ${d.name}`;
      } else {
        return acc === '' ? `${d.name}` :
          `${acc}, ${d.name}`;
      }
    } else {
      if (d.group === false) {
        return acc === '' ? d.value : `${acc}, ${d.value}`;
      } else {
        return acc === '' ? `Parent ${d.value}` : `${acc}, Parent ${d.value}`;
      }
    }
  }

  removeDuplicates(arr) {
    return arr.filter((thing, index, self) =>
      index === self.findIndex((t) => (
        t.items === thing.items && t.domain === thing.domain
      ))
    );
  }

  operatorConversion(operator) {
    switch (operator) {
      case 'GREATER_THAN_OR_EQUAL_TO' :
        return '>=';
      case 'LESS_THAN_OR_EQUAL_TO' :
        return '<=';
      case 'EQUAL' :
        return '=';
      case 'BETWEEN' :
        return 'Between';
      case 'ETHNICITY' :
        return 'Ethnicity';
      case 'RACE' :
        return 'Race';
      case 'AGE' :
        return 'Age';
      case 'GENDER' :
        return 'Gender';
      case 'AGE_AT_EVENT' :
        return 'Age at Event';
      case 'NUM_OF_OCCURRENCES' :
        return 'Num of Occurrences';
    }
  }

  render() {
    const {definition} = this.state;
    return <div style={{marginTop: '1rem', marginBottom: '1rem'}}>
      <style>{css}</style>
      <div style={styles.defTitle}>
        Cohort Definition
      </div>
      {definition && definition.map((group, g) => (
        <React.Fragment key={g}>
          {group.role === 'excludes' && <div className='page-break' style={styles.wrapper}>
            <div style={styles.exclude}>EXCLUDING</div>
          </div>}
          {group.groups.map((item, i) => (
            <React.Fragment key={i}>
              {i > 0 && <div className='page-break' style={styles.wrapper}>
                <div style={styles.andCircle}>AND</div>
              </div>}
              <div style={styles.groupBackground}>
                {item.map((param, p) => (
                  <React.Fragment key={p}>
                    {p > 0 && <div>OR</div>}
                    <div>
                      {param.map((crit, c) => (
                        <React.Fragment key={c}>
                          {c > 0 && <React.Fragment>
                            {crit.domain === DomainType.PERSON && <div>AND</div>}
                            {crit.domain !== DomainType.PERSON && <div>OR</div>}
                          </React.Fragment>}
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
    </div>;
  }
}
