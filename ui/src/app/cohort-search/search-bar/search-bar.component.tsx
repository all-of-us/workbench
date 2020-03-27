import * as fp from 'lodash/fp';
import * as React from 'react';

import {domainToTitle} from 'app/cohort-search/utils';
import {ClrIcon} from 'app/components/icons';
import {TextInput} from 'app/components/inputs';
import {TooltipTrigger} from 'app/components/popups';
import {Spinner} from 'app/components/spinners';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {highlightSearchTerm, reactStyles} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {Criteria, CriteriaType, DomainType} from 'generated/fetch';
import {Key} from 'ts-key-enum';

const styles = reactStyles({
  searchContainer: {
    background: colors.white,
    width: '100%',
    zIndex: 10,
  },
  searchBar: {
    backgroundColor: colorWithWhiteness(colors.secondary, 0.8),
    borderRadius: '5px',
    height: '40px',
    padding: '7px 14px',
  },
  searchInput: {
    background: 'transparent',
    border: 0,
    height: '1rem',
    marginLeft: '0.25rem',
    outline: 'none',
    padding: '0',
    width: '85%',
  },
  dropdownMenu: {
    position: 'absolute',
    top: '100%',
    left: 0,
    marginTop: '.083333rem',
    display: 'flex',
    flexDirection: 'column',
    background: colors.white,
    padding: '.25rem 0',
    border: '1px solid #ccc',
    boxShadow: '0 1px 0.125rem hsla(0,0%,45%,.25)',
    maxHeight: '15rem',
    minHeight: '1.25rem',
    overflowY: 'auto',
    width: '100%',
    borderRadius: '.125rem',
    zIndex: 1000
  },
  dropdownItem: {
    height: '1rem',
    background: 'transparent',
    border: 0,
    cursor: 'pointer',
    margin: 0,
    overflow: 'hidden',
    padding: '0 1rem',
    textAlign: 'left',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    width: '100%'
  }
});

const trigger = 3;

interface OptionProps {
  onClick: Function;
  option: any;
  searchTerm: string;
  highlighted: boolean;
}

interface OptionState {
  hover: boolean;
  truncated: boolean;
}

class SearchBarOption extends React.Component<OptionProps, OptionState> {
  container: HTMLButtonElement;
  constructor(props: OptionProps) {
    super(props);
    this.state = {
      hover: false,
      truncated: false
    };
  }

  handleResize = fp.debounce(100, () => {
    this.checkContainerWidth();
  });

  componentDidMount(): void {
    this.checkContainerWidth();
    window.addEventListener('resize', this.handleResize);
  }

  componentWillUnmount(): void {
    window.removeEventListener('resize', this.handleResize);
  }

  checkContainerWidth() {
    const {offsetWidth, scrollWidth} = this.container;
    this.setState({truncated: scrollWidth > offsetWidth});
  }

  render() {
    const {highlighted, onClick, option: {name}, searchTerm} = this.props;
    const {hover} = this.state;
    const displayText = highlightSearchTerm(searchTerm, name, colors.success);
    return <div>
      <TooltipTrigger content={<div>{displayText}</div>} disabled={!this.state.truncated}>
        <button ref={(e) => this.container = e}
          style={highlighted || hover ? {...styles.dropdownItem, background: colorWithWhiteness(colors.black, .93)} : styles.dropdownItem}
          onClick={() => onClick()}
          onMouseEnter={() => this.setState({hover: true})}
          onMouseLeave={() => this.setState({hover: false})}>
            {displayText}
        </button>
      </TooltipTrigger>
    </div>;
  }
}

interface Props {
  node: Criteria;
  searchTerms: string;
  setIngredients: Function;
  selectOption: Function;
  setInput: Function;
}

interface State {
  options: Array<any>;
  loading: boolean;
  optionSelected: boolean;
  error: boolean;
  highlightedOption: number;
  subtype: string;
}

export class SearchBar extends React.Component<Props, State> {
  dropdown: HTMLDivElement;
  constructor(props: Props) {
    super(props);
    this.state = {
      options: null,
      loading: false,
      optionSelected: false,
      error: false,
      highlightedOption: null,
      subtype: undefined,
    };
  }

  debounceInput = fp.debounce(300, (input: string) => {
    if (input.length < trigger) {
      this.setState({options: null});
    } else {
      this.handleInput();
    }
  });

  componentDidMount() {
    document.addEventListener('click', (e) => {
      if (!!this.dropdown && !this.dropdown.contains(e.target as Node)) {
        this.setState({options: null});
      }
    });
  }

  componentDidUpdate(prevProps: Readonly<Props>): void {
    const {node: {domainId}, searchTerms} = this.props;
    if (searchTerms !== prevProps.searchTerms) {
      if (domainId === DomainType.PHYSICALMEASUREMENT.toString()) {
        triggerEvent(`Cohort Builder Search - Physical Measurements`, 'Search', searchTerms);
      } else if (this.state.optionSelected) {
        this.setState({optionSelected: false});
      } else {
        this.debounceInput(searchTerms);
      }
    }
  }

  handleInput() {
    const {node: {domainId, isStandard, type}, searchTerms} = this.props;
    triggerEvent(`Cohort Builder Search - ${domainToTitle(domainId)}`, 'Search', searchTerms);
    this.setState({loading: true});
    const {cdrVersionId} = currentWorkspaceStore.getValue();
    const apiCall = domainId === DomainType.DRUG.toString()
      ? cohortBuilderApi().findDrugBrandOrIngredientByValue(+cdrVersionId, searchTerms)
      : cohortBuilderApi().findCriteriaAutoComplete(+cdrVersionId, domainId, searchTerms, type, isStandard);
    apiCall.then(resp => {
      const optionNames: Array<string> = [];
      const options = resp.items.filter(option => {
        if (!optionNames.includes(option.name)) {
          optionNames.push(option.name);
          return true;
        }
        return false;
      });
      this.setState({highlightedOption: null, loading: false, options});
    }, (err) => this.setState({error: err}));
  }

  get showOverflow() {
    const {options} = this.state;
    return options && options.length <= 10;
  }

  selectOption(option: any) {
    if (option) {
      const {selectOption, setIngredients, setInput} = this.props;
      setInput(option.name);
      this.setState({highlightedOption: null, options: null, optionSelected: true});
      if (option.type === CriteriaType.BRAND.toString()) {
        const cdrId = +(currentWorkspaceStore.getValue().cdrVersionId);
        cohortBuilderApi().findDrugIngredientByConceptId(cdrId, option.conceptId)
          .then(resp => {
            if (resp.items.length) {
              const ingredients = resp.items.map(it => it.name);
              setIngredients(ingredients);
              // just grabbing the first one on the list for now
              selectOption(resp.items[0]);
            }
          });
      } else {
        setIngredients(null);
        selectOption(option);
      }
    }
  }

  onKeyDown(key: Key) {
    switch (key) {
      case Key.ArrowDown:
        this.moveDown();
        break;
      case Key.ArrowUp:
        this.moveUp();
        break;
      case Key.Enter:
        this.enterSelect();
    }
  }

  moveUp() {
    const {highlightedOption} = this.state;
    if (highlightedOption === 0) {
      this.setState({highlightedOption: null});
    } else if (highlightedOption > 0) {
      this.setState({highlightedOption: highlightedOption - 1});
    }
  }

  moveDown() {
    const {highlightedOption, options} = this.state;
    if (highlightedOption === null) {
      this.setState({highlightedOption: 0});
    } else if ((highlightedOption + 1) < options.length) {
      this.setState({highlightedOption: highlightedOption + 1});
    }
  }

  enterSelect() {
    const {highlightedOption, options} = this.state;
    this.selectOption(options[highlightedOption]);
  }

  render() {
    const {highlightedOption, loading, options} = this.state;
    const inputValue = highlightedOption !== null ? options[highlightedOption].name : this.props.searchTerms;
    return <div style={{position: 'relative', width: '100%'}}>
      <div style={styles.searchContainer}>
        <div style={styles.searchBar}>
          {loading ? <Spinner style={{verticalAlign: 'middle'}} size={16}/> : <ClrIcon shape='search' size='18'/>}
          <TextInput style={styles.searchInput}
            value={inputValue}
            onChange={(e) => this.props.setInput(e)}
            onKeyDown={(e) => this.onKeyDown(e.key)}/>
        </div>
      </div>
      {options !== null && <div ref={(el) => this.dropdown = el} style={styles.dropdownMenu}>
        {options.length === 0
          ? <em style={{padding: '0.15rem 1.25rem'}}>No results based on your search</em>
          : options.map((opt, o) => <SearchBarOption key={o}
                                            option={opt}
                                            searchTerm={this.props.searchTerms}
                                            highlighted={o === highlightedOption}
                                            onClick={() => this.selectOption(opt)}/>)}
      </div>}
    </div>;
  }
}
