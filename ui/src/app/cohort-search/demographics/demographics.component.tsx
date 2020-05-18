import {Component, Input} from '@angular/core';
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
    border: `1px solid ${colorWithWhiteness(colors.black, 0.8)}`,
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
  agePreview: {
    minWidth: '50%',
    padding: '0.25rem 1rem',
    width: 'auto'
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
// Template node used for age selections
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

const ageTypes = [
  {label: 'Current Age', type: AttrName.AGE},
  {label: 'Age at Consent', type: AttrName.AGEATCONSENT},
  {label: 'Age at CDR Date', type: AttrName.AGEATCDR}
];

const defaultMinAge = '18';
const defaultMaxAge = '120';

function sortByCountThenName(critA, critB) {
  const A = critA.count || 0;
  const B = critB.count || 0;
  const diff = B - A;
  return diff === 0
        ? (critA.name > critB.name ? 1 : -1)
        : diff;
}

interface Props {
  count: number;
  criteriaType: CriteriaType;
  select: Function;
  selectedIds: Array<string>;
  selections: Array<any>;
}

interface State {
  ageType: AttrName;
  ageTypeNodes: any;
  calculating: boolean;
  count: number;
  loading: boolean;
  maxAge: string;
  minAge: string;
  nodes: Array<any>;
}

export class Demographics extends React.Component<Props, State> {
  ageWrapper: HTMLDivElement;
  slider: {
    get: () => Array<string>;
    set: (values: Array<number>) => void;
  };

  constructor(props: Props) {
    super(props);
    this.state = {
      ageType: AttrName.AGE,
      ageTypeNodes: undefined,
      calculating: false,
      count: null,
      loading: true,
      maxAge: defaultMaxAge,
      minAge: defaultMinAge,
      nodes: undefined,
    };
  }

  componentDidMount(): void {
    if (this.props.criteriaType === CriteriaType.AGE) {
      if (serverConfigStore.getValue().enableCBAgeTypeOptions) {
        this.loadAgeNodesFromApi();
      } else {
        this.setState({loading: false});
      }
    } else {
      this.loadNodesFromApi();
    }
  }

  componentDidUpdate(prevProps: Readonly<Props>): void {
    const {criteriaType, selections} = this.props;
    if (selections !== prevProps.selections && criteriaType !== CriteriaType.AGE) {
      this.calculate();
    }
  }

  async loadNodesFromApi() {
    const {criteriaType, selections} = this.props;
    const {cdrVersionId} = currentWorkspaceStore.getValue();
    this.setState({loading: true});
    const response = await cohortBuilderApi().findCriteriaBy(+cdrVersionId, DomainType.PERSON.toString(), criteriaType.toString());
    const nodes = response.items.filter(item => item.parentId !== 0)
      .sort(sortByCountThenName)
      .map(node => ({...node, parameterId: `param${node.conceptId || node.code}`}));
    if (selections.length) {
      this.calculate(true);
    }
    this.setState({loading: false, nodes});
  }

  async loadAgeNodesFromApi() {
    const {cdrVersionId} = currentWorkspaceStore.getValue();
    const initialValue = {
      [AttrName.AGE]: [],
      [AttrName.AGEATCONSENT]: [],
      [AttrName.AGEATCDR]: []
    };
    const response = await cohortBuilderApi().findAgeTypeCounts(+cdrVersionId);
    const ageTypeNodes = response.items.reduce((acc, item) => {
      acc[item.ageType].push(item);
      return acc;
    }, initialValue);
    this.setState({ageTypeNodes}, () => this.calculateAgeFromNodes());
  }

  initAgeRange() {
    const {count, selections} = this.props;
    if (selections.length) {
      const {attributes} = selections[0];
      const {name, operands} = attributes[0];
      this.slider.set([+operands[0], +operands[1]]);
      this.setState({count: count || null, maxAge: operands[1], minAge: operands[0]});
      if (serverConfigStore.getValue().enableCBAgeTypeOptions) {
        this.setState({ageType: name});
      }
    } else {
      // timeout prevents Angular 'ExpressionChangedAfterItHasBeenCheckedError' in CB modal component
      setTimeout(() => this.updateAgeSelection());
    }
    if (!serverConfigStore.getValue().enableCBAgeTypeOptions) {
      const {cdrVersionId} = currentWorkspaceStore.getValue();
      if (!ageCountStore.getValue()[cdrVersionId]) {
        // Get total age count for this cdr version if it doesn't exist in the store yet
        this.calculateAge(true);
      } else if (this.setTotalAge && !selections.length) {
        this.setState({count:  ageCountStore.getValue()[cdrVersionId]});
      }
    }
  }

  onMinChange(minAge: string) {
    const {maxAge} = this.state;
    let sliderMin = +minAge;
    if (+minAge < +defaultMinAge) {
      sliderMin = +defaultMinAge;
    } else if (+minAge > +maxAge) {
      sliderMin = +maxAge;
    }
    this.slider.set([sliderMin, null]);
    this.setState({minAge}, () => {
      if (serverConfigStore.getValue().enableCBAgeTypeOptions) {
        this.calculateAgeFromNodes('min');
      } else {
        this.setState({count: null});
      }
    });
  }

  onMaxChange(maxAge: string) {
    const {minAge} = this.state;
    let sliderMax = +maxAge;
    if (+maxAge > +defaultMaxAge) {
      sliderMax = +defaultMaxAge;
    } else if (+maxAge < +minAge) {
      sliderMax = +minAge;
    }
    this.slider.set([null, sliderMax]);
    this.setState({maxAge}, () => {
      if (serverConfigStore.getValue().enableCBAgeTypeOptions) {
        this.calculateAgeFromNodes('max');
      } else {
        this.setState({count: null});
      }
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
    this.slider.set([null, +maxAge]);
    this.setState({maxAge}, () => this.updateAgeSelection());
  }

  onMinBlur() {
    const {maxAge} = this.state;
    let {minAge} = this.state;
    if (+minAge > +maxAge) {
      minAge = maxAge;
    } else if (+minAge < +defaultMinAge || minAge === '') {
      minAge = defaultMinAge;
    }
    this.slider.set([+minAge, null]);
    this.setState({minAge}, () => this.updateAgeSelection());
  }

  onRadioChange(ageType: AttrName) {
    this.setState({ageType}, () => {
      this.updateAgeSelection();
      this.calculateAgeFromNodes();
    });
  }

  onSliderInit(slider: React.ReactNode) {
    this.slider = slider['noUiSlider'];
    if (this.slider) {
      this.initAgeRange();
      this.centerAgeCount();
    }
  }

  onSliderUpdate(range: Array<string>) {
    // Use split here to drop the decimals (the slider defaults to 2 decimal places)
    const [min, max] = range.map(n => n.split('.')[0]);
    this.setState({maxAge: max, minAge: min}, () => {
      if (serverConfigStore.getValue().enableCBAgeTypeOptions) {
        this.calculateAgeFromNodes();
      } else {
        this.setState({count: null});
      }
    });
  }

  updateAgeSelection() {
    const {ageType, maxAge, minAge} = this.state;
    const selectedNode = {
      ...ageNode,
      name: `Age In Range ${minAge} - ${maxAge}`,
      attributes: [{
        name: ageType.toString(),
        operator: Operator.BETWEEN,
        operands: [minAge, maxAge]
      }],
    };
    this.props.select(selectedNode);
  }

  centerAgeCount() {
    if (serverConfigStore.getValue().enableCBAgeTypeOptions && !!this.slider) {
      // get range from slider element and convert the strings to numbers
      const [sliderMin, sliderMax] = this.slider.get().map(v => +v);
      // get width as a % by dividing the selected age range by the full slider range
      const width = (sliderMax - sliderMin) / (+defaultMaxAge - +defaultMinAge) * 100;
      // get left margin as a % by dividing the change in minAge by the full slider range
      const marginLeft = (sliderMin - +defaultMinAge) / (+defaultMaxAge - +defaultMinAge) * 100;
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

  async calculateAge(init?: boolean) {
    const {maxAge, minAge} = this.state;
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
    try {
      const response = await cohortBuilderApi().countParticipants(+cdrVersionId, request);
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
    } catch (err) {
      console.error(err);
      this.setState({calculating: false});
    }
  }

  calculateAgeFromNodes(minOrMax?: string) {
    const {ageTypeNodes, ageType, maxAge, minAge} = this.state;
    let max = +maxAge;
    let min = +minAge;
    if (minOrMax === 'min' && min > +max) {
      min = max;
    } else if (minOrMax === 'max' && max < min) {
      max = min;
    }
    const count = ageTypeNodes[ageType]
      .filter(node => node.age >= min && node.age <= max)
      .reduce((acc, node) => acc + node.count, 0);
    this.setState({count, loading: false}, () => this.centerAgeCount());
  }

  get showPreview() {
    const {criteriaType, selections} = this.props;
    return !this.state.loading
      && (selections && selections.length > 0)
      && !(criteriaType === CriteriaType.AGE && serverConfigStore.getValue().enableCBAgeTypeOptions);
  }

  // Checks if form is in its initial state and if a count already exists before setting the total age count
  get setTotalAge() {
    const {count, maxAge, minAge} = this.state;
    return minAge === defaultMinAge && maxAge === defaultMaxAge && !count;
  }

  render() {
    const {criteriaType, selectedIds} = this.props;
    const {ageType, calculating, count, loading, maxAge, minAge, nodes} = this.state;
    const isAge = criteriaType === CriteriaType.AGE;
    const calcDisabled = calculating || count !== null;
    return loading
      ? <div style={{textAlign: 'center'}}><Spinner style={{marginTop: '3rem'}}/></div>
      : <React.Fragment>
        {isAge
          // Age slider with number inputs
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
                onChange={(e) => this.onMinChange(e.target.value)}/>
              <div style={serverConfigStore.getValue().enableCBAgeTypeOptions
                ? {...styles.slider, marginBottom: '0.75rem'}
                : styles.slider}>
                {serverConfigStore.getValue().enableCBAgeTypeOptions && <div ref={(el) => this.ageWrapper = el} id='count-wrapper'>
                  {calculating
                    ? <Spinner size={16}/>
                    : <span style={styles.count} id='age-count'>
                      {count.toLocaleString()}
                    </span>
                  }
                </div>}
                <Nouislider behaviour='drag'
                  connect
                  instanceRef={(slider) => this.onSliderInit(slider)}
                  onChange={() => this.updateAgeSelection()}
                  onSlide={(v) => this.onSliderUpdate(v)}
                  range={{min: +defaultMinAge, max: +defaultMaxAge}}
                  start={[+defaultMinAge, +defaultMaxAge]}
                  step={1}/>
              </div>
              <input style={styles.ageInput}
                type='number'
                id='max-age'
                min={minAge} max={defaultMaxAge}
                value={maxAge}
                onBlur={() => this.onMaxBlur()}
                onChange={(e) => this.onMaxChange(e.target.value)}/>
            </div>
            {serverConfigStore.getValue().enableCBAgeTypeOptions && <div style={{marginLeft: '1rem'}}>
              {ageTypes.map((ageTypeRadio, a) => <div key={a} style={{display: 'inline-block', marginRight: '0.5rem'}}>
                <input type='radio' name='ageType'
                  style={{marginRight: '0.25rem'}}
                  onChange={() => this.onRadioChange(ageTypeRadio.type)}
                  checked={ageTypeRadio.type === ageType}/>
                <label>{ageTypeRadio.label}</label>
              </div>)}
            </div>}
          </div>
          // List of selectable criteria used for Race, Ethnicity, Gender and Sex
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
        {this.showPreview && <div style={isAge ? {...styles.countPreview, ...styles.agePreview} : styles.countPreview}>
          {isAge && <div style={{float: 'left', marginRight: '0.25rem'}}>
            <button style={calcDisabled ? {...styles.calculateBtn, opacity: 0.4} : styles.calculateBtn}
              disabled={calcDisabled}
              onClick={() => this.calculateAge()}>
              {calculating && <Spinner style={{marginRight: '0.25rem'}} size={16}/>}
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
  @Input('count') count: Props['count'];
  @Input('criteriaType') criteriaType: Props['criteriaType'];
  @Input('select') select: Props['select'];
  @Input('selectedIds') selectedIds: Props['selectedIds'];
  @Input('selections') selections: Props['selections'];

  constructor() {
    super(Demographics, ['count', 'criteriaType', 'select', 'selectedIds', 'selections']);
  }
}
