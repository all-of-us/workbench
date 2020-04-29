import {Component, Input} from '@angular/core';
import {FormControl, FormGroup} from '@angular/forms';
import Nouislider from 'nouislider-react';
import * as React from 'react';

import {ageCountStore} from 'app/cohort-search/search-state.service';
import {mapParameter, typeToTitle} from 'app/cohort-search/utils';
import {ClrIcon} from 'app/components/icons';
import {Spinner} from 'app/components/spinners';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {currentWorkspaceStore, serverConfigStore} from 'app/utils/navigation';
import {AttrName, CriteriaType, DomainType, Operator} from 'generated/fetch';

const styles = reactStyles({
  ageContainer: {
    border: '1px solid #cccccc',
    borderRadius: '5px',
    margin: '0.5rem 1rem',
    maxHeight: '15rem',
    padding: '0.5rem 0 1.5rem 1rem'
  },
  ageInput: {
    border: `1px solid ${colors.black}`,
    borderRadius: '3px',
    fontSize: '0.5rem',
    fontWeight: 300,
    marginTop: '0.25rem',
    padding: '0 0.5rem',
    width: '1rem',
  },
  ageLabel: {
    fontSize: '14px',
    fontWeight: 600,
    color: colors.primary
  },
  calculateBtn: {
    background: colors.primary,
    border: 'none',
    borderRadius: '0.3rem',
    color: colors.white,
    cursor: 'pointer',
    fontSize: '12px',
    height: '1.5rem',
    letterSpacing: '0.02rem',
    lineHeight: '0.75rem',
    margin: '0.25rem 0.5rem 0.25rem 0',
    padding: '0rem 0.75rem',
    textTransform: 'uppercase',
  },
  count: {
    alignItems: 'center',
    background: colors.accent,
    borderRadius: '10px',
    color: colors.white,
    display: 'inline-flex',
    fontSize: '10px',
    height: '0.625rem',
    justifyContent: 'center',
    lineHeight: 'normal',
    margin: '0 0.25rem',
    minWidth: '0.675rem',
    padding: '0 4px',
    verticalAlign: 'middle'
  },
  countPreview: {
    backgroundColor: colorWithWhiteness(colors.secondary, 0.8),
    padding: '0.5rem',
    margin: '0 2.5%',
    position: 'absolute',
    width: '95%',
    bottom: '0.5rem',
  },
  option: {
    color: colors.black,
    cursor: 'pointer',
    fontSize: '13px',
    fontWeight: 400,
    marginBottom: '0.5rem',
    padding: '0 0.25rem',
    textTransform: 'capitalize',
  },
  resultText: {
    color: colors.primary,
    fontWeight: 500,
  },
  selectIcon: {
    color: colors.select,
    marginRight: '0.25rem'
  },
  selected: {
    cursor: 'not-allowed',
    opacity: 0.4
  },
  selectList: {
    alignItems: 'center',
    display: 'flex',
    marginRight: '1rem',
    maxHeight: '15rem',
    padding: '0.5rem 0 0 1rem'
  },
  slider: {
    flex: 1,
    padding: '0 0.5rem',
    margin: '0 1rem',
  },
  sliderContainer: {
    alignItems: 'center',
    display: 'flex',
    marginRight: '1rem',
    paddingLeft: '1rem',
    width: '96%',
  }
});

const ageNode = {
  hasAncestorData: false,
  attributes: [],
  code: '',
  domainId: DomainType.PERSON,
  group: false,
  name: 'Age',
  parameterId: 'age-param',
  isStandard: true,
  type: CriteriaType.AGE,
  value: ''
};

const defaultMinAge = '18';
const defaultMaxAge = '120';
const enableCBAgeTypeOptions = serverConfigStore.getValue().enableCBAgeTypeOptions;

/*
 * Sorts a plain JS array of plain JS objects first by a 'count' key and then
 * by a 'name' key
 */
function sortByCountThenName(critA, critB) {
  const A = critA.count || 0;
  const B = critB.count || 0;
  const diff = B - A;
  return diff === 0
        ? (critA.name > critB.name ? 1 : -1)
        : diff;
}
interface Props {
  select: Function;
  selectedIds: Array<string>;
  selections: Array<any>;
  wizard: any;
}

interface State {
  ageType: any;
  ageTypeNodes: any;
  calculating: boolean;
  count: number;
  loading: boolean;
  maxAge: string;
  minAge: string;
  nodes: Array<any>;
  sliderStart: Array<number>;
}

export class Demographics extends React.Component<Props, State> {
  ageWrapper: HTMLDivElement;
  ageTypes = [
    {label: 'Current Age', type: AttrName.AGE.toString()},
    {label: 'Age at Consent', type: AttrName.AGEATCONSENT.toString()},
    {label: 'Age at CDR Date', type: AttrName.AGEATCDR.toString()}
  ];

    /* The Demographics form controls and associated convenience lenses */
  demoForm = new FormGroup({
    ageMin: new FormControl(18),
    ageMax: new FormControl(120),
    ageRange: new FormControl([]),
    ageType: new FormControl(AttrName.AGE.toString()),
  });

  constructor(props: Props) {
    super(props);
    this.state = {
      ageType: AttrName.AGE.toString(),
      ageTypeNodes: undefined,
      calculating: false,
      count: null,
      loading: true,
      maxAge: defaultMaxAge,
      minAge: defaultMinAge,
      nodes: undefined,
      sliderStart: [+defaultMinAge, +defaultMaxAge],
    };
  }

  componentDidMount(): void {
    if (this.props.wizard.type === CriteriaType.AGE) {
      this.initAgeRange();
      if (enableCBAgeTypeOptions) {
        this.loadAgeNodesFromApi();
      } else {
        this.setState({loading: false});
      }
    } else {
      this.loadNodesFromApi();
    }
  }

  componentDidUpdate(prevProps: Readonly<Props>): void {
    const {selections, wizard} = this.props;
    if (selections !== prevProps.selections && wizard.type !== CriteriaType.AGE) {
      this.calculate();
    }
  }

  loadNodesFromApi() {
    const {selections, wizard} = this.props;
    const {cdrVersionId} = currentWorkspaceStore.getValue();
    this.setState({loading: true});
    cohortBuilderApi().findCriteriaBy(+cdrVersionId, DomainType.PERSON.toString(), wizard.type).then(response => {
      const nodes = response.items
        .filter(item => item.parentId !== 0)
        .sort(sortByCountThenName)
        .map(node => ({...node, parameterId: `param${node.conceptId || node.code}`}));
      if (selections.length) {
        this.calculate(true);
      }
      this.setState({loading: false, nodes});
    });
  }

  loadAgeNodesFromApi() {
    const {cdrVersionId} = currentWorkspaceStore.getValue();
    const initialValue = {[AttrName.AGE.toString()]: [], 'AGE_AT_CONSENT': [], 'AGE_AT_CDR': []};
    cohortBuilderApi().findAgeTypeCounts(+cdrVersionId).then(response => {
      const ageTypeNodes = response.items.reduce((acc, item) => {
        acc[item.ageType].push(item);
        return acc;
      }, initialValue);
      this.setState({ageTypeNodes}, () => this.calculateAge());
    });
  }

  onMaxBlur() {
    const {minAge} = this.state;
    let {maxAge} = this.state;
    if (+maxAge < +minAge) {
      maxAge = minAge;
    } else if (+maxAge > +defaultMaxAge || maxAge === '') {
      maxAge = defaultMaxAge;
    }
    this.setState({maxAge, sliderStart: [+minAge, +maxAge]}, () => this.updateAgeSelection());
  }

  onMinBlur() {
    const {maxAge} = this.state;
    let {minAge} = this.state;
    if (+minAge > +maxAge) {
      minAge = maxAge;
    } else if (+minAge < +defaultMinAge || minAge === '') {
      minAge = defaultMinAge;
    }
    this.setState({minAge, sliderStart: [+minAge, +maxAge]}, () => this.updateAgeSelection());
  }

  updateAgeSelection() {
    const {maxAge, minAge} = this.state;
    const selectedNode = {
      ...ageNode,
      name: `Age In Range ${minAge} - ${maxAge}`,
      attributes: [{
        name: AttrName.AGE,
        operator: Operator.BETWEEN,
        operands: [minAge, maxAge]
      }],
    };
    this.props.select(selectedNode);
  }

  initAgeRange() {
    const {selections, wizard} = this.props;
    if (selections.length) {
      const {attributes} = selections[0];
      const {operands} = attributes[0];
      this.setState({count: wizard.count, minAge: operands[0], maxAge: operands[1], sliderStart: [+operands[0], +operands[1]]});
      if (enableCBAgeTypeOptions) {
        this.setState({ageType: attributes[0].name});
      }
    } else {
      // timeout prevents Angular 'ExpressionChangedAfterItHasBeenCheckedError' in CB modal component
      setTimeout(() => this.updateAgeSelection());
    }
    if (!enableCBAgeTypeOptions) {
      const {cdrVersionId} = currentWorkspaceStore.getValue();
      if (!ageCountStore.getValue()[cdrVersionId]) {
        // Get total age count for this cdr version if it doesn't exist in the store yet
        this.calculateAge(true);
      } else if (this.setTotalAge) {
        this.setState({count:  ageCountStore.getValue()[cdrVersionId]});
      }
    }
  }

  centerAgeCount() {
    if (enableCBAgeTypeOptions) {
      const {maxAge, minAge} = this.state;
      // get width as a % by dividing the selected age range by the full slider range
      const width = (+maxAge - +minAge) / (+defaultMaxAge - +defaultMinAge) * 100;
      // get left margin as a % by dividing the change in minAge by the full slider range
      const marginLeft = (+minAge - +defaultMinAge) / (+defaultMaxAge - +defaultMinAge) * 100;
      const wrapper = document.getElementById('count-wrapper');
      if (!!wrapper) {
        wrapper.setAttribute('style', `margin-left: ${marginLeft}%; width: ${width}%; text-align: center;`);
        // set style properties also for cross-browser compatibility
        wrapper.style.marginLeft = `${marginLeft}%`;
        wrapper.style.width = `${width}%`;
        wrapper.style.textAlign = 'center';
      }
    }
  }

  selectOption(opt: any) {
    triggerEvent('Cohort Builder Search', 'Click', `Demo - ${typeToTitle(opt.type)} - ${opt.name}`);
    this.props.select({...opt, name: `${typeToTitle(opt.type)} - ${opt.name}`});
  }

  calculate(init?: boolean) {
    let count = 0;
    this.props.selections.forEach(sp => {
      if (init) {
        const node = this.state.nodes.find(n => n.conceptId === sp.conceptId);
        if (node) {
          sp.count = node.count;
        }
      }
      count += sp.count;
    });
    this.setState({count});
  }

  calculateAge(init?: boolean) {
    const {ageType, maxAge, minAge} = this.state;
    if (enableCBAgeTypeOptions) {
      const count = this.state.ageTypeNodes[ageType]
        .filter(node => node.age >= minAge && node.age <= maxAge)
        .reduce((acc, node) => acc + node.count, 0);
      this.setState({count, loading: false}, () => this.centerAgeCount());
    } else {
      if (!init || this.setTotalAge) {
        this.setState({calculating: true});
      }
      const {cdrVersionId} = currentWorkspaceStore.getValue();
      const min = init ? defaultMinAge : minAge;
      const max = init ? defaultMaxAge : maxAge;
      const parameter = {
        ...ageNode,
        name: `Age In Range ${min} - ${max}`,
        attributes: [{
          name: AttrName.AGE,
          operator: Operator.BETWEEN,
          operands: [min, max]
        }],
      };
      const request = {
        excludes: [],
        includes: [{
          items: [{
            type: DomainType.PERSON.toString(),
            searchParameters: [mapParameter(parameter)],
            modifiers: []
          }],
          temporal: false
        }],
        dataFilters: []
      };
      cohortBuilderApi().countParticipants(+cdrVersionId, request).then(response => {
        if (init) {
          const ageCounts = ageCountStore.getValue();
          ageCounts[cdrVersionId] = response;
          ageCountStore.next(ageCounts);
          if (this.setTotalAge) {
            this.setState({count: response});
          }
        } else {
          this.setState({count: response});
        }
        this.setState({calculating: false});
      }, (err) => {
        console.error(err);
        this.setState({calculating: false});
      });
    }
  }

  get showPreview() {
    const {selections, wizard} = this.props;
    return !this.state.loading
      && (selections && selections.length > 0)
      && !(wizard.type === CriteriaType.AGE && enableCBAgeTypeOptions);
  }

  // Checks if form is in its initial state and if a count already exists before setting the total age count
  get setTotalAge() {
    const {count, maxAge, minAge} = this.state;
    return minAge === defaultMinAge && maxAge === defaultMaxAge && !count;
  }

  render() {
    const {selectedIds, wizard} = this.props;
    const {ageType, calculating, count, loading, maxAge, minAge, nodes, sliderStart} = this.state;
    const isAge = wizard.type === CriteriaType.AGE;
    return loading
      ? <div style={{textAlign: 'center'}}><Spinner style={{marginTop: '3rem'}}/></div>
      : <React.Fragment>
        {isAge
          ? <div style={styles.ageContainer}>
            <div style={styles.ageLabel}>
              Age Range
            </div>
            <div style={styles.sliderContainer}>
              <input style={styles.ageInput}
                type='number'
                id='min-age'
                min={defaultMinAge} max={maxAge}
                value={minAge}
                onBlur={() => this.onMinBlur()}
                onChange={(e) => this.setState({minAge: e.target.value}, () => {
                  if (enableCBAgeTypeOptions) {
                    this.calculateAge();
                  }
                })}/>
              <div style={enableCBAgeTypeOptions ? {...styles.slider, marginBottom: '0.75rem'} : styles.slider}>
                {enableCBAgeTypeOptions && <div ref={(el) => this.ageWrapper = el} id='count-wrapper'>
                  {calculating
                    ? <Spinner size={16}/>
                    : <span style={styles.count} id='age-count'>
                      {count.toLocaleString()}
                    </span>
                  }
                </div>}
                <Nouislider range={{min: +defaultMinAge, max: +defaultMaxAge}}
                  instanceRef={() => {
                    this.centerAgeCount();
                  }}
                  onChange={() => this.updateAgeSelection()}
                  onUpdate={(v) => this.setState({maxAge: v[1].split('.')[0], minAge: v[0].split('.')[0]})}
                  start={sliderStart}
                  step={1}
                  connect
                  onSlide={() => this.calculateAge()}
                  behaviour='drag'/>
              </div>
              <input style={styles.ageInput}
                type='number'
                id='max-age'
                min={minAge} max={defaultMaxAge}
                value={maxAge}
                onBlur={() => this.onMaxBlur()}
                onChange={(e) => this.setState({maxAge: e.target.value}, () => {
                  if (enableCBAgeTypeOptions) {
                    this.calculateAge();
                  }
                })}/>
            </div>
            {enableCBAgeTypeOptions && <div style={{marginLeft: '1rem'}}>
              {this.ageTypes.map((ageTypeRadio, a) => <div key={a} style={{display: 'inline-block', marginRight: '0.5rem'}}>
                <input type='radio' name='ageType'
                  style={{marginRight: '0.25rem'}}
                  onChange={() => this.setState({ageType: ageTypeRadio.type}, () => this.calculateAge())}
                  checked={ageTypeRadio.type === ageType}/>
                <label>{ageTypeRadio.label}</label>
              </div>)}
            </div>}
          </div>
          : <div style={styles.selectList}>
            <div style={{margin: '0.25rem 0', overflow: 'auto', width: '100%'}}>
              {nodes.map((opt, o) => <div key={o} style={styles.option} onClick={() => this.selectOption(opt)}>
                {selectedIds.includes(opt.parameterId)
                  ? <ClrIcon shape='check-circle' size='20' style={{...styles.selectIcon, ...styles.selected}}/>
                  : <ClrIcon shape='plus-circle'  size='20' style={styles.selectIcon}/>
                }
                {opt.name}
                {!!opt.count && <span style={styles.count}>
                  {opt.count.toLocaleString()}
                </span>}
              </div>)}
            </div>
          </div>
        }
        {this.showPreview && <div style={isAge
          ? {...styles.countPreview, minWidth: '50%', padding: '0.25rem 1rem', width: 'auto'}
          : styles.countPreview}>
          {isAge && <div style={{float: 'left', marginRight: '0.25rem'}}>
            <button style={styles.calculateBtn} disabled={calculating || count !== null} onClick={() => this.calculateAge()}>
              {calculating && <Spinner size={16}/>}
              Calculate
            </button>
          </div>}
          {(count !== null || isAge) && <div style={{float: 'left', fontSize: '14px'}}>
            <div style={{color: colors.primary, fontWeight: 500}}>
              Results
            </div>
            <div>
              Number Participants:
              <b style={{color: colors.dark}}>
                &nbsp;{count !== null ? count.toLocaleString() : ' -- '}
              </b>
            </div>
          </div>}
        </div>}
      </React.Fragment>;
  }
}

@Component({
  selector: 'crit-demographics',
  template: '<div #root></div>'
})
export class DemographicsComponent extends ReactWrapperBase {
  @Input('select') select: Props['select'];
  @Input('selectedIds') selectedIds: Props['selectedIds'];
  @Input('selections') selections: Props['selections'];
  @Input('wizard') wizard: Props['wizard'];

  constructor() {
    super(Demographics, ['select', 'selectedIds', 'selections', 'wizard']);
  }
}
