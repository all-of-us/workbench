import * as fp from 'lodash/fp';
import * as React from 'react';

import {autocompleteStore, subtreePathStore, subtreeSelectedStore} from 'app/cohort-search/search-state.service';
import {domainToTitle} from 'app/cohort-search/utils';
import {ClrIcon} from 'app/components/icons';
import {TextInput} from 'app/components/inputs';
import {TooltipTrigger} from 'app/components/popups';
import {cohortBuilderApi} from 'app/services/swagger-fetch-clients';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {highlightSearchTerm, reactStyles} from 'app/utils';
import {triggerEvent} from 'app/utils/analytics';
import {currentWorkspaceStore} from 'app/utils/navigation';
import {Criteria, CriteriaType, DomainType} from 'generated/fetch';
import {AutoComplete} from 'primereact/autocomplete';
import {Subscription} from 'rxjs/Subscription';

const styles = reactStyles({
  searchContainer: {
    position: 'absolute',
    width: '95%',
    padding: '0.4rem 0',
    background: colors.white,
    zIndex: 10,
  },
  searchBar: {
    height: '40px',
    width: '100%',
    padding: '7px 14px',
    borderRadius: '5px',
    backgroundColor: colorWithWhiteness(colors.secondary, 0.8),
  },
  searchInput: {
    width: '85%',
    height: '1rem',
    marginLeft: '0.25rem',
    padding: '0',
    background: 'transparent',
    border: 0,
    outline: 'none',
  },
});

const trigger = 3;

interface OptionProps {
  option: any;
  searchTerm: string;
  highlighted: boolean;
}

interface OptionState {
  truncated: boolean;
}

class SearchBarOption extends React.Component<OptionProps, OptionState> {
  container: HTMLButtonElement;
  constructor(props: OptionProps) {
    super(props);
    this.state = {truncated: false};
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
    const {option: {name}, searchTerm} = this.props;
    const displayText = highlightSearchTerm(searchTerm, name, colors.success);
    return <button className='dropdown-item'ref={(e) => this.container = e}>
      <TooltipTrigger content={<div>{displayText}</div>} disabled={!this.state.truncated}>
        <div>{displayText}</div>
      </TooltipTrigger>
    </button>;
  }
}

interface Props {
  setIngredients: Function;
  node: Criteria;
}

interface State {
  searchTerm: string;
  typedTerm: string;
  options: Array<any>;
  loading: boolean;
  noResults: boolean;
  optionSelected: boolean;
  error: boolean;
  highlightedOption: number;
  subtype: string;
}

export class SearchBar extends React.Component<Props, State> {
  subscription: Subscription;
  constructor(props: Props) {
    super(props);
    this.state = {
      searchTerm: undefined,
      typedTerm: undefined,
      options: [],
      loading: false,
      noResults: false,
      optionSelected: false,
      error: false,
      highlightedOption: undefined,
      subtype: undefined,
    };
  }

  debounceInput = fp.debounce(300, (input: string) => {
    console.log(input);
    if (input.length < trigger) {
      this.setState({options: [], noResults: false});
    } else {
      this.handleInput();
    }
  });

  componentDidMount() {
    this.subscription = autocompleteStore.subscribe(searchTerm => {
      this.setState({searchTerm});
    });
  }

  componentWillUnmount() {
    this.subscription.unsubscribe();
  }

  onInputChange(value: string) {
    console.log(value);
    const {node: {domainId}} = this.props;
    autocompleteStore.next(value);
    if (domainId === DomainType.PHYSICALMEASUREMENT.toString()) {
      triggerEvent(`Cohort Builder Search - Physical Measurements`, 'Search', value);
      autocompleteStore.next(value);
    } else {
      this.debounceInput(value);
    }
  }

  handleInput() {
    const {node: {domainId, isStandard, type}} = this.props;
    const {searchTerm} = this.state;
    triggerEvent(`Cohort Builder Search - ${domainToTitle(domainId)}`, 'Search', searchTerm);
    this.setState({loading: true, optionSelected: false, noResults: false});
    const {cdrVersionId} = currentWorkspaceStore.getValue();
    const apiCall = domainId === DomainType.DRUG.toString()
      ? cohortBuilderApi().findDrugBrandOrIngredientByValue(+cdrVersionId, searchTerm)
      : cohortBuilderApi().findCriteriaAutoComplete(+cdrVersionId, domainId, searchTerm, type, isStandard);
    apiCall.then(resp => {
      const optionNames: Array<string> = [];
      const options = resp.items.filter(option => {
        if (!optionNames.includes(option.name)) {
          optionNames.push(option.name);
          return true;
        }
        return false;
      });
      this.setState({highlightedOption: null, loading: false, noResults: options.length === 0, options});
    }, (err) => this.setState({error: err}));
  }

  get showOverflow() {
    const {options} = this.state;
    return options && options.length <= 10;
  }

  selectOption(option: any) {
    if (option) {
      const {setIngredients} = this.props;
      this.setState({optionSelected: true, searchTerm: option.name});
      if (option.type === CriteriaType[CriteriaType.BRAND]) {
        const cdrId = +(currentWorkspaceStore.getValue().cdrVersionId);
        cohortBuilderApi().findDrugIngredientByConceptId(cdrId, option.conceptId)
          .then(resp => {
            if (resp.items.length) {
              const ingredients = resp.items.map(it => it.name);
              setIngredients(ingredients);
              // just grabbing the first one on the list for now
              const {path, id} = resp.items[0];
              subtreePathStore.next(path.split('.'));
              subtreeSelectedStore.next(id);
            }
          });
      } else {
        setIngredients(null);
        autocompleteStore.next(option.name);
        subtreePathStore.next(option.path.split('.'));
        subtreeSelectedStore.next(option.id);
      }
    }
  }

  enterSelect() {
    const {highlightedOption, options} = this.state;
    this.selectOption(options[highlightedOption]);
  }

  render() {
    const {node} = this.props;
    const {highlightedOption, options, searchTerm} = this.state;
    return <div>
      <div style={styles.searchContainer}>
        <div style={styles.searchBar}>
          <ClrIcon shape='search' size='18'/>
          <TextInput style={styles.searchInput} value={searchTerm} onChange={(e) => this.onInputChange(e)} />
        </div>
        {/*<AutoComplete field='name' value={searchTerm} onChange={(e) => this.onInputChange(e.value)}*/}
        {/*  suggestions={options} onSelect={(e) => this.selectOption(e.value)} completeMethod={() => this.inputChange()}/>*/}
      </div>
      {options.length && <div className='dropdown-menu'>
        {options.map((opt, o) => <SearchBarOption key={o}
                                            option={opt}
                                            searchTerm={searchTerm}
                                            highlighted={o === highlightedOption}/>)}
      </div>}
    </div>;
  }
}
