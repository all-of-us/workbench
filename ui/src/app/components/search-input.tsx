import {TextInput} from 'app/components/inputs';
import {TooltipTrigger} from 'app/components/popups';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';
import * as fp from 'lodash/fp';
import * as React from 'react';

const styles = reactStyles({
  dropdownMenu: {
    display: 'block',
    maxHeight: '12rem',
    minHeight: '30px',
    visibility: 'visible',
    overflowY: 'scroll',
    width: '35%',
    marginTop: '.25rem',
    zIndex: 100,
    border: `1px solid ${colorWithWhiteness(colors.dark, 0.7)}`,
    borderRadius: '5px',
    backgroundColor: colors.white,
  },
  open: {
    position: 'absolute',
    backgroundColor: colors.white,
    border: '1px solid'
  },
  box: {
    borderRadius: '5px',
    paddingTop: '0.2rem',
    paddingLeft: '0.2rem',
    color: colors.primary,
  },
  boxHover: {
    background: 'rgb(234, 243, 250)',
    color: colors.primary,
    paddingTop: '0.2rem',
    paddingLeft: '0.2rem',
  },
  boxHoverElement: {
    color: colors.primary,
    margin: 0,
    padding: 0,
    fontFamily: 'Montserrat',
    fontSize: '14px',
    lineHeight: '32px',
  },
});

// Fifo accepts a function 'fn' that returns a promise, and returns a
// new function that returns a promise. Promises returned by the
// resulting function return their result of the last-instantiated
// promise. For example, if a0, a1 are the start and end times of a
// computation represented by promise A, and b0, b1 are the start and end
// times of a computation represented by promise B, and a0 < b0 < b1 <
// a1, then the value returned by promise A at time a1 will be the result
// of promise B, *not* promise A, because B was instantiated after A.
function fifo(fn) {
  let nextCallID = 1;
  let lastCompletedCallID = 0;
  let lastCompletedResult = null;
  return (...args) => {
    const thisCallID = nextCallID++;
    return new Promise((accept, reject) => {
      fn.apply(null, args).then((newResult) => {
        if (thisCallID > lastCompletedCallID) {
          lastCompletedCallID = thisCallID;
          lastCompletedResult = newResult;
        }
        accept(lastCompletedResult);
      }).catch((e) => {
        reject(e);
      });
    });
  };
}

interface SearchInputState {
  matches: Array<string>;
  hover: Array<boolean>;
  state: number;
}

export interface SearchInputProps {
  enabled: boolean;
  placeholder: string;
  value: string;
  onSearch: (keyword: string) => Promise<Array<string>>;
  tooltip: string;
  onChange: (newInput: string) => void;
}

export class SearchInput extends React.Component<SearchInputProps, SearchInputState> {
  // Forward declarations
  _searchTermChangedEvent: Function;
  _onSearch: Function;
  _START: number;
  _ACTIVE: number;
  _SUGGEST: number;
  _HOVER: number;

  constructor(props) {
    super(props);
    this.state = {
      matches: [],
      hover: [],
      state: this._START,
    };
    this._onSearch = fifo(props.onSearch);
    this._searchTermChangedEvent = fp.debounce(300, this._search);

    // State machine states
    this._START = 0;   // Input box lacks focus, drop-down invisible
    this._ACTIVE = 1;  // Input box focused, drop-down invisible
    this._SUGGEST = 2; // Input box focused, search results visible
    this._HOVER = 3;   // Input box focused, search results visible, item activated
  }

  _toggleHover(j) {
    return this.state.hover.map((elt, i) => {
      return (i === j) ? !elt : elt;
    });
  }

  _search(keyword: string): void {
    this.setState({matches: []});
    keyword = keyword.trim();
    if (!keyword) {
      return;
    }
    this._onSearch(keyword).then((matches) => {
      this.setState({
        matches: matches,
        state: matches.length > 0 ?
          this._SUGGEST : this._ACTIVE,
        hover: matches.map(() => false),
      });
    }).catch((e) => {
      console.error(e);
    });
  }

  _onBlur() {
    const state = this.state.state;
    if (state === this._ACTIVE  ||
        state === this._SUGGEST ||
        state === this._HOVER) {
      this.setState({state: this._START});
    }
  }

  _onChange(match) {
    this.setState({state: this._ACTIVE});
    this._searchTermChangedEvent(match);
    this.props.onChange(match);
  }

  _onFocus() {
    this.setState({state: this._ACTIVE});
  }

  _onMouseOver(j) {
    this.setState({
      state: this._HOVER,
      hover: this._toggleHover(j)
    });
  }

  _onMouseOut(j) {
    this.setState({
      state: this._SUGGEST,
      hover: this._toggleHover(j)
    });
  }

  _onMouseDown(match) {
    this.props.onChange(match);
  }

  _suggest() {
    const s = this.state.state;
    const rval = this.props.enabled &&
      (s === this._SUGGEST || s === this._HOVER);
    return rval;
  }

  render() {
    const borderColor = colorWithWhiteness(colors.dark, 0.7);
    const inputStyle = {
      width: '90%',
      border: `1px solid ${borderColor}`,
      borderRadius: '5px'
    };
    return (
      <div style={{position: 'relative'}}>
        <TooltipTrigger
          content={this.props.tooltip}
          disabled={this.props.enabled}>
          <TextInput
            value={this.props.value}
            style={inputStyle}
            onFocus={this._onFocus.bind(this)}
            onBlur={this._onBlur.bind(this)}
            onChange={e => this._onChange(e)}
            placeholder={this.props.placeholder}
            disabled={!this.props.enabled}/>
        </TooltipTrigger>
        {this._suggest() &&
          <div data-test-id='drop-down'
            style={{...styles.dropdownMenu, ...styles.open, minWidth: '90%'}}>
            {this.state.matches.map((match, j) => {
              return (
                <div key={j} style={this.state.hover[j] ? styles.boxHover : styles.box}
                  onMouseOver={this._onMouseOver.bind(this, j)}
                  onMouseOut={this._onMouseOut.bind(this, j)}
                  onMouseDown={this._onMouseDown.bind(this, match)}>
                  <h5 style={styles.boxHoverElement}>{match}</h5>
                </div>
              );
            })}
          </div>
        }
      </div>
    );
  }
}
