import * as React from 'react';
import * as fp from 'lodash/fp';

import {
  Criteria,
  CriteriaSearchRequest,
  CriteriaType,
  Domain,
} from 'generated/fetch';

import { AlertDanger } from 'app/components/alert';
import { ClrIcon } from 'app/components/icons';
import { TextInput } from 'app/components/inputs';
import { TooltipTrigger } from 'app/components/popups';
import { Spinner } from 'app/components/spinners';
import { domainToTitle } from 'app/pages/data/cohort/utils';
import { cohortBuilderApi } from 'app/services/swagger-fetch-clients';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import {
  highlightSearchTerm,
  reactStyles,
  validateInputForMySQL,
} from 'app/utils';
import { AnalyticsTracker } from 'app/utils/analytics';
import { currentWorkspaceStore } from 'app/utils/navigation';

const styles = reactStyles({
  searchContainer: {
    float: 'left',
    background: colors.white,
    width: '95%',
    zIndex: 10,
  },
  searchBar: {
    backgroundColor: colorWithWhiteness(colors.secondary, 0.8),
    borderRadius: '5px',
    height: '2.505rem',
    padding: '0.45rem 0.87rem',
  },
  searchInput: {
    background: 'transparent',
    border: 0,
    height: '1.5rem',
    marginLeft: '0.375rem',
    outline: 'none',
    padding: '0',
    width: '94%',
  },
  dropdownMenu: {
    position: 'absolute',
    top: '100%',
    left: 0,
    marginTop: '.125rem',
    display: 'flex',
    flexDirection: 'column',
    background: colors.white,
    padding: '.375rem 0',
    border: `1px solid ${colorWithWhiteness(colors.black, 0.8)}`,
    boxShadow: '0 1px 0.1875rem hsla(0,0%,45%,.25)',
    maxHeight: '22.5rem',
    minHeight: '1.875rem',
    overflowY: 'auto',
    width: '100%',
    borderRadius: '.1875rem',
    zIndex: 105,
  },
  dropdownItem: {
    height: '1.5rem',
    background: 'transparent',
    border: 0,
    cursor: 'pointer',
    margin: 0,
    overflow: 'hidden',
    padding: '0 1.5rem',
    textAlign: 'left',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
    width: '100%',
  },
  inputAlert: {
    justifyContent: 'space-between',
    padding: '0.3rem',
    width: '64.3%',
  },
  infoIcon: {
    color: colorWithWhiteness(colors.accent, 0.1),
    marginLeft: '0.375rem',
    height: '100%',
  },
});

const searchTooltip = (
  <div style={{ marginLeft: '0.75rem' }}>
    The following special operators can be used to augment search terms:
    <ul style={{ listStylePosition: 'outside' }}>
      <li>
        (*) is the wildcard operator. This operator can be used with a prefix or
        suffix. For example: ceph* (starts with) or *statin (ends with - NOTE:
        when searching for ends with it will only match with end of concept
        name)
      </li>
      <li>
        (-) indicates that this word must <b>not</b> be present. For example:
        lung -cancer
      </li>
      <li>
        (") a phrase that is enclosed within double quote (") characters matches
        only rows that contain the phrase literally, as it was typed. For
        example: "lung cancer"
      </li>
      <li>
        These operators can be combined to produce more complex search
        operations. For example: brain tum* -neoplasm
      </li>
    </ul>
  </div>
);

const searchTrigger = 2;

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
      truncated: false,
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
    const { offsetWidth, scrollWidth } = this.container;
    this.setState({ truncated: scrollWidth > offsetWidth });
  }

  render() {
    const {
      highlighted,
      onClick,
      option: { name },
      searchTerm,
    } = this.props;
    const { hover } = this.state;
    const displayText = highlightSearchTerm(searchTerm, name, colors.success);
    return (
      <div>
        <TooltipTrigger
          content={<div>{displayText}</div>}
          disabled={!this.state.truncated}
        >
          <button
            ref={(e) => (this.container = e)}
            style={
              highlighted || hover
                ? {
                    ...styles.dropdownItem,
                    background: colorWithWhiteness(colors.black, 0.93),
                  }
                : styles.dropdownItem
            }
            onClick={() => onClick()}
            onMouseEnter={() => this.setState({ hover: true })}
            onMouseLeave={() => this.setState({ hover: false })}
          >
            {displayText}
          </button>
        </TooltipTrigger>
      </div>
    );
  }
}

interface Props {
  node: Criteria;
  searchTerms: string;
  setIngredients: Function;
  selectedSurvey: string;
  selectOption: Function;
  setInput: Function;
}

interface State {
  error: boolean;
  highlightedOption: number;
  inputErrors: Array<string>;
  loading: boolean;
  options: Array<any>;
  optionSelected: boolean;
  subtype: string;
}

export class SearchBar extends React.Component<Props, State> {
  dropdown: HTMLDivElement;
  constructor(props: Props) {
    super(props);
    this.state = {
      error: false,
      highlightedOption: null,
      inputErrors: [],
      loading: false,
      options: null,
      optionSelected: false,
      subtype: undefined,
    };
  }

  debounceInput = fp.debounce(300, (input: string) => {
    if (input.trim().length < searchTrigger) {
      this.setState({ inputErrors: [], options: null });
    } else {
      const inputErrors = validateInputForMySQL(input, searchTrigger);
      if (inputErrors.length > 0) {
        this.setState({ inputErrors, options: null });
      } else {
        this.handleInput();
      }
    }
  });

  componentDidMount() {
    document.getElementById('root').addEventListener('click', (e) => {
      if (!!this.dropdown && !this.dropdown.contains(e.target as Node)) {
        this.setState({ options: null });
      }
    });
  }

  componentDidUpdate(prevProps: Readonly<Props>): void {
    const {
      node: { domainId },
      searchTerms,
    } = this.props;
    if (searchTerms !== prevProps.searchTerms) {
      if (
        domainId === Domain.PHYSICAL_MEASUREMENT.toString() ||
        domainId === Domain.VISIT.toString()
      ) {
        AnalyticsTracker.CohortBuilder.SearchTerms(
          `Hierarchy search - ${domainToTitle(domainId)} - '${searchTerms}'`
        );
      } else if (this.state.optionSelected) {
        this.setState({ optionSelected: false });
      } else {
        this.debounceInput(searchTerms);
      }
    }
  }

  handleInput() {
    const {
      node: { domainId, isStandard, subtype, type },
      searchTerms,
      selectedSurvey,
    } = this.props;
    AnalyticsTracker.CohortBuilder.SearchTerms(
      `${domainToTitle(domainId)} - '${searchTerms}'`
    );
    this.setState({ inputErrors: [], loading: true });
    const { id, namespace } = currentWorkspaceStore.getValue();
    let apiCall;
    switch (domainId) {
      case Domain.DRUG.toString():
        apiCall = cohortBuilderApi().findDrugBrandOrIngredientByValue(
          namespace,
          id,
          searchTerms
        );
        break;
      case Domain.SURVEY.toString():
        const surveyRequest: CriteriaSearchRequest = {
          domain: Domain.SURVEY.toString(),
          surveyName: selectedSurvey || 'All surveys',
          term: searchTerms,
          standard: true,
        };
        apiCall = cohortBuilderApi().findCriteriaAutoComplete(
          namespace,
          id,
          surveyRequest
        );
        break;
      default:
        const request: CriteriaSearchRequest = {
          domain: domainId,
          term: searchTerms,
          type: type,
          standard: isStandard,
        };
        apiCall = cohortBuilderApi().findCriteriaAutoComplete(
          namespace,
          id,
          request
        );
    }
    apiCall.then(
      (resp) => {
        const optionNames: Array<string> = [];
        const options = resp.items.filter((option) => {
          if (
            !optionNames.includes(option.name) &&
            (domainId !== Domain.MEASUREMENT.toString() ||
              option.subtype === subtype)
          ) {
            optionNames.push(option.name);
            return true;
          }
          return false;
        });
        this.setState({ highlightedOption: null, loading: false, options });
      },
      (err) => this.setState({ error: err })
    );
  }

  get showOverflow() {
    const { options } = this.state;
    return options && options.length <= 10;
  }

  selectOption(option: any) {
    if (option) {
      const { selectOption, setIngredients, setInput } = this.props;
      setInput(option.name);
      this.setState({
        highlightedOption: null,
        options: null,
        optionSelected: true,
      });
      if (option.type === CriteriaType.BRAND.toString()) {
        const { id, namespace } = currentWorkspaceStore.getValue();
        cohortBuilderApi()
          .findDrugIngredientByConceptId(namespace, id, option.conceptId)
          .then((resp) => {
            if (resp.items.length) {
              const ingredients = resp.items.map((it) => it.name);
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

  onKeyDown(key) {
    switch (key) {
      case 'ArrowDown':
        this.moveDown();
        break;
      case 'ArrowUp':
        this.moveUp();
        break;
      case 'Enter':
        this.enterSelect();
    }
  }

  moveUp() {
    const { highlightedOption } = this.state;
    if (highlightedOption === 0) {
      this.setState({ highlightedOption: null });
    } else if (highlightedOption > 0) {
      this.setState({ highlightedOption: highlightedOption - 1 });
    }
  }

  moveDown() {
    const { highlightedOption, options } = this.state;
    if (highlightedOption === null) {
      this.setState({ highlightedOption: 0 });
    } else if (highlightedOption + 1 < options.length) {
      this.setState({ highlightedOption: highlightedOption + 1 });
    }
  }

  enterSelect() {
    const { highlightedOption, options } = this.state;
    this.selectOption(options[highlightedOption]);
  }

  doesDomainIncludeToolTip() {
    return (
      this.props.node.domainId !== Domain.VISIT.toString() &&
      this.props.node.domainId !== Domain.PHYSICAL_MEASUREMENT.toString()
    );
  }

  render() {
    const { highlightedOption, inputErrors, loading, options } = this.state;
    const inputValue =
      highlightedOption !== null
        ? options?.[highlightedOption].name
        : this.props.searchTerms;
    return (
      <div style={{ display: 'flex', position: 'relative', width: '100%' }}>
        <div style={styles.searchContainer}>
          <div style={styles.searchBar}>
            {loading ? (
              <Spinner style={{ verticalAlign: 'middle' }} size={16} />
            ) : (
              <ClrIcon shape='search' size='18' />
            )}
            <TextInput
              style={styles.searchInput}
              value={inputValue}
              onChange={(e) => this.props.setInput(e)}
              onKeyDown={(e) => this.onKeyDown(e.key)}
            />
          </div>
          {inputErrors.map((error, e) => (
            <AlertDanger key={e} style={styles.inputAlert}>
              <span data-test-id='input-error-alert'>{error}</span>
            </AlertDanger>
          ))}
        </div>
        <div style={{ float: 'right' }}>
          {this.doesDomainIncludeToolTip() && (
            <TooltipTrigger side='top' content={searchTooltip}>
              <ClrIcon
                style={styles.infoIcon}
                className='is-solid'
                shape='info-standard'
              />
            </TooltipTrigger>
          )}
        </div>
        {options !== null && (
          <div ref={(el) => (this.dropdown = el)} style={styles.dropdownMenu}>
            {options.length === 0 ? (
              <em style={{ padding: '0.225rem 1.875rem' }}>
                No results based on your search
              </em>
            ) : (
              options.map((opt, o) => (
                <SearchBarOption
                  key={o}
                  option={opt}
                  searchTerm={this.props.searchTerms}
                  highlighted={o === highlightedOption}
                  onClick={() => this.selectOption(opt)}
                />
              ))
            )}
          </div>
        )}
      </div>
    );
  }
}
