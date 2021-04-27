import {ClrIcon} from 'app/components/icons';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {highlightSearchTerm, reactStyles} from 'app/utils';
import * as fp from 'lodash/fp';
import * as React from 'react';
const sidebarContent = require('assets/json/help-sidebar.json');

const styles = reactStyles({
  sectionTitle: {
    fontWeight: 600,
    color: colors.primary
  },
  contentTitle: {
    marginTop: '0.25rem',
    fontSize: '14px',
    fontWeight: 600,
    color: colors.primary
  },
  contentItem: {
    marginTop: 0,
    color: colors.primary
  },
  textSearch: {
    width: '100%',
    borderRadius: '4px',
    backgroundColor: colorWithWhiteness(colors.primary, .95),
    marginTop: '5px',
    marginBottom: '0.5rem',
    color: colors.primary,
  },
  textInput: {
    width: '85%',
    height: '1.5rem',
    padding: '0 0 0 5px',
    border: 0,
    backgroundColor: 'transparent',
    outline: 'none',
  }
});

interface Props {
  pageKey: string;
  allowSearch: boolean;
  onSearch?: Function;
}

interface State {
  searchTerm: string;
  filteredContent: Array<any>;
}

export class HelpTips extends React.Component<Props, State> {
  constructor(props) {
    super(props);
    this.state = {
      filteredContent: undefined,
      searchTerm: ''
    };
  }

  debounceInput = fp.debounce(300, (input: string) => {
    if (input.length < 3) {
      this.setState({filteredContent: undefined});
    } else {
      this.searchHelpTips(input.trim().toLowerCase());
    }
  });

  searchHelpTips(input: string) {
    this.props.onSearch();
    // For each object, we check the title first. If it matches, we return the entire content array.
    // If the title doesn't match, we check each element of the content array for matches
    const filteredContent = fp.values(JSON.parse(JSON.stringify(sidebarContent))).reduce((acc, section) => {
      const inputMatch = (text: string) => text.toLowerCase().includes(input);
      const content = section.reduce((ac, item) => {
        if (!inputMatch(item.title)) {
          item.content = item.content.reduce((a, ic) => {
            if (typeof ic === 'string') {
              if (inputMatch(ic)) {
                a.push(ic);
              }
            } else if (inputMatch(ic.title)) {
              a.push(ic);
            } else {
              ic.content = ic.content.filter(inputMatch);
              if (ic.content.length) {
                a.push(ic);
              }
            }
            return a;
          }, []);
        }
        return item.content.length ? [...ac, item] : ac;
      }, []);
      return [...acc, ...content];
    }, []);
    this.setState({filteredContent});
  }

  onInputChange(value: string) {
    this.setState({searchTerm: value});
    this.debounceInput(value);
  }

  highlightMatches(content: string) {
    const {searchTerm} = this.state;
    return highlightSearchTerm(searchTerm, content, colors.success);
  }

  helpContentKey(pageKey: string) {
    if (pageKey === 'conceptSetActions' || pageKey === 'searchConceptSets') {
      return 'conceptSets';
    }
    return pageKey;
  }

  render() {
    const {filteredContent} = this.state;
    const displayContent = filteredContent !== undefined ? filteredContent : sidebarContent[this.helpContentKey(this.props.pageKey)];

    return <div>
      {this.props.allowSearch && <div style={styles.textSearch}>
          <ClrIcon style={{color: colors.primary, margin: '0 0.25rem'}} shape='search' size={16} />
          <input
              type='text'
              style={styles.textInput}
              value={this.state.searchTerm}
              onChange={(e) => this.onInputChange(e.target.value)}
              placeholder={'Search'} />
      </div>}
      {!!displayContent && displayContent.length > 0
        ? displayContent.map((section, s) => <div key={s}>
          <h3 style={{...styles.sectionTitle, marginTop: s === 0 ? 0 : '0.5rem'}} data-test-id={`section-title-${s}`}>
            {this.highlightMatches(section.title)}
          </h3>
          {section.content.map((content, c) => {
            return typeof content === 'string'
              ? <p key={c} style={styles.contentItem}>{this.highlightMatches(content)}</p>
              : <div key={c}>
                <h4 style={styles.contentTitle}>{this.highlightMatches(content.title)}</h4>
                {content.content.map((item, i) =>
                  <p key={i} style={styles.contentItem}>{this.highlightMatches(item)}</p>
                )}
              </div>;
          })}
        </div>)
        : <div style={{marginTop: '0.5rem'}}><em>No results found</em></div>
      }
    </div>;
  }
}
