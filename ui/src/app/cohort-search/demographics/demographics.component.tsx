import Nouislider from 'nouislider-react';
import * as React from 'react';

import {typeToTitle} from 'app/cohort-search/utils';
import {Button} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {NumberInput, RadioButton} from 'app/components/inputs';
import {Spinner} from 'app/components/spinners';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {AttrName, CriteriaType, Domain, Operator} from 'generated/fetch';

const styles = reactStyles({
  ageContainer: {
    margin: '0.5rem 1rem',
    maxHeight: '15rem',
    padding: '0.5rem 0 1.5rem 1rem',
    width: '80%'
  },
  ageInput: {
    fontSize: '13px',
    marginTop: '0.25rem',
    padding: '0 0 0 0.5rem',
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
    width: '35%',
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
    padding: '0.5rem 0 0 0.25rem'
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
  }
});
// Template node used for age selections
const ageNode = {
  hasAncestorData: false,
  code: '',
  domainId: Domain.PERSON,
  group: false,
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
      this.loadAgeNodesFromApi();
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
    const {id, namespace} = currentWorkspaceStore.getValue();
    this.setState({loading: true});
    const response = await cohortBuilderApi().findCriteriaBy(namespace, id, Domain.PERSON.toString(), criteriaType.toString());
    const nodes = response.items.filter(item => item.count !== -1)
      .sort(sortByCountThenName)
      .map(node => ({...node, parameterId: `param${node.conceptId || node.code}`}));
    if (selections.length) {
      this.calculate(true);
    }
    this.setState({loading: false, nodes});
  }

  async loadAgeNodesFromApi() {
    const {id, namespace} = currentWorkspaceStore.getValue();
    const initialValue = {
      [AttrName.AGE]: [],
      [AttrName.AGEATCONSENT]: [],
      [AttrName.AGEATCDR]: []
    };
    const response = await cohortBuilderApi().findAgeTypeCounts(namespace, id);
    const ageTypeNodes = response.items.reduce((acc, item) => {
      acc[item.ageType].push(item);
      return acc;
    }, initialValue);
    this.setState({ageTypeNodes}, () => this.calculateAgeFromNodes());
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
    this.setState({minAge}, () => this.calculateAgeFromNodes('min'));
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
    this.setState({maxAge}, () => this.calculateAgeFromNodes('max'));
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
    this.setState({maxAge});
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
    this.setState({minAge});
  }

  onRadioChange(ageType: AttrName) {
    this.setState({ageType}, () => this.calculateAgeFromNodes());
  }

  onSliderInit(slider: React.ReactNode) {
    this.slider = slider['noUiSlider'];
    if (this.slider) {
      this.centerAgeCount();
    }
  }

  onSliderUpdate(range: Array<string>) {
    // Use split here to drop the decimals (the slider defaults to 2 decimal places)
    const [min, max] = range.map(n => n.split('.')[0]);
    this.setState({maxAge: max, minAge: min}, () => this.calculateAgeFromNodes());
  }

  get ageParameterId() {
    const {ageType, maxAge, minAge} = this.state;
    return `${ageType.toString()}-${minAge}-${maxAge}`;
  }

  addAgeSelection() {
    const {ageType, maxAge, minAge} = this.state;
    const ageTypeLabel = ageTypes.find(at => at.type === ageType).label;
    const selectedNode = {
      ...ageNode,
      parameterId: this.ageParameterId,
      name: `${ageTypeLabel} In Range ${minAge} - ${maxAge}`,
      attributes: [{
        name: ageType.toString(),
        operator: Operator.BETWEEN,
        operands: [minAge, maxAge]
      }],
    };
    this.props.select(selectedNode);
  }

  centerAgeCount() {
    if (!!this.slider) {
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
    const {selections} = this.props;
    return !this.state.loading && !!selections && selections.length > 0;
  }

  render() {
    const {criteriaType, selectedIds} = this.props;
    const {ageType, calculating, count, loading, maxAge, minAge, nodes} = this.state;
    return loading
      ? <div style={{textAlign: 'center'}}>
        <Spinner style={{marginTop: '3rem'}}/>
      </div>
      : criteriaType === CriteriaType.AGE
        // Age slider with number inputs
        ? <div style={styles.ageContainer}>
          <div style={styles.sliderContainer}>
            <div style={{width: '2.5rem'}}>
              <NumberInput style={styles.ageInput}
                min={defaultMinAge} max={maxAge}
                value={minAge}
                onBlur={() => this.onMinBlur()}
                onChange={(v) => this.onMinChange(v)}/>
            </div>
            <div style={{...styles.slider, marginBottom: '0.75rem'}}>
              <div ref={(el) => this.ageWrapper = el} id='count-wrapper'>
                {calculating
                  ? <Spinner size={16}/>
                  : <span style={styles.count} id='age-count'>
                    {count.toLocaleString()}
                  </span>
                }
              </div>
              <Nouislider behaviour='drag'
                connect
                instanceRef={(slider) => this.onSliderInit(slider)}
                onSlide={(v) => this.onSliderUpdate(v)}
                range={{min: +defaultMinAge, max: +defaultMaxAge}}
                start={[+defaultMinAge, +defaultMaxAge]}
                step={1}/>
            </div>
            <div style={{width: '2.5rem'}}>
              <NumberInput style={styles.ageInput}
                min={minAge} max={defaultMaxAge}
                value={maxAge}
                onBlur={() => this.onMaxBlur()}
                onChange={(v) => this.onMaxChange(v)}/>
            </div>
            <Button style={{marginLeft: '1rem'}}
                    type='primary'
                    disabled={selectedIds.includes(this.ageParameterId)}
                    onClick={() => this.addAgeSelection()}>
              Add Selection
            </Button>
          </div>
          <div style={{marginLeft: '1rem'}}>
            {ageTypes.map((ageTypeRadio, index) =>
              <div key={index} style={{display: 'inline-block', marginRight: '0.5rem'}}>
                <RadioButton name='ageType'
                  style={{marginRight: '0.25rem'}}
                  onChange={() => this.onRadioChange(ageTypeRadio.type)}
                  checked={ageTypeRadio.type === ageType}/>
                <label>{ageTypeRadio.label}</label>
              </div>)
            }
          </div>
        </div>
        // List of selectable criteria used for Race, Ethnicity, Gender and Sex
        : <React.Fragment>
          <div style={styles.selectList}>
            <div style={{margin: '0.25rem 0', overflow: 'auto', width: '100%'}}>
              {nodes.map((opt, o) => <div key={o} style={styles.option}>
                {selectedIds.includes(opt.parameterId)
                  ? <ClrIcon shape='check-circle' size='20' style={{...styles.selectIcon, ...styles.selected}}/>
                  : <ClrIcon shape='plus-circle'  size='20' style={styles.selectIcon} onClick={() => this.selectOption(opt)}/>
                }
                {opt.name}
                {!!opt.count && <span style={styles.count}>
                  {opt.count.toLocaleString()}
                </span>}
              </div>)}
            </div>
          </div>
          {this.showPreview && <div style={styles.countPreview}>
            {count !== null && <div style={{fontSize: '14px'}}>
              <div style={{color: colors.primary, fontWeight: 500}}>
                Results
              </div>
              <div>
                Number Participants:
                <b style={{color: colors.dark}}>
                  &nbsp;{count.toLocaleString()}
                </b>
              </div>
            </div>}
          </div>}
        </React.Fragment>;
  }
}
