import {Component, Input} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {SidebarContent} from 'app/cohort-review/sidebar-content/sidebar-content.component';
import {ClrIcon} from 'app/components/icons';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase} from 'app/utils';

const sidebarContent = require('assets/json/help-sidebar.json');

const styles = reactStyles({
  sidebarContainer: {
    position: 'absolute',
    top: 0,
    right: 'calc(-0.6rem - 45px)',
    height: '100%',
    minHeight: 'calc(100vh - 156px)',
    width: 'calc(14rem + 45px)',
    overflow: 'hidden',
    color: colors.primary,
    zIndex: -1,
  },
  sidebarContainerActive: {
    zIndex: 100,
  },
  sidebar: {
    position: 'absolute',
    top: 0,
    right: '45px',
    height: '100%',
    width: '14rem',
    overflow: 'auto',
    marginRight: 'calc(-14rem - 40px)',
    background: colorWithWhiteness(colors.primary, .87),
    transition: 'margin-right 0.5s ease-out'
  },
  sidebarOpen: {
    marginRight: 0,
  },
  iconContainer: {
    position: 'absolute',
    top: 0,
    right: 'calc(-0.6rem - 45px)',
    height: '100%',
    minHeight: 'calc(100vh - 156px)',
    width: '45px',
    background: colorWithWhiteness(colors.primary, 0.4),
    zIndex: 101
  },
  icon: {
    color: colors.white,
    marginTop: '1rem',
    padding: '0.25rem 0',
    cursor: 'pointer',
    textAlign: 'center',
    transition: 'background 0.2s linear'
  },
  topBar: {
    width: '14rem',
    background: colorWithWhiteness(colors.primary, 0.4),
    color: colors.white,
    height: '1rem',
    zIndex: 1
  },
  closeIcon: {
    cursor: 'pointer',
    verticalAlign: 'top'
  },
  sectionTitle: {
    marginTop: '0.5rem',
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
    color: colorWithWhiteness(colors.primary, -1),
  },
  textInput: {
    width: '90%',
    height: '1.5rem',
    padding: '0 0 0 5px',
    border: 0,
    backgroundColor: 'transparent',
    outline: 'none',
  },
  highlighted: {
    color: colorWithWhiteness(colors.success, -0.4),
    backgroundColor: colorWithWhiteness(colors.success, 0.7),
    display: 'inline-block'
  }
});

const iconStyles = {
  active: {
    ...styles.icon,
    background: colorWithWhiteness(colors.primary, 0.55)
  },
  disabled: {
    ...styles.icon,
    opacity: 0.4,
    cursor: 'not-allowed'
  }
};

interface Props {
  location: string;
  participant?: any;
  setParticipant?: Function;
}

interface State {
  activeIcon: string;
  filteredContent: any;
  sidebarOpen: boolean;
  searchTerm: string;
}
export class HelpSidebar extends React.Component<Props, State> {
  debounceInput: Function;
  constructor(props: Props) {
    super(props);
    this.state = {
      activeIcon: undefined,
      filteredContent: undefined,
      sidebarOpen: false,
      searchTerm: ''
    };
    this.debounceInput = fp.debounce(300, (input: string) => {
      if (input === '' || input.length < 3) {
        this.setState({filteredContent: undefined});
      } else {
        this.searchHelpTips(input.toLowerCase());
      }
    });
  }

  searchHelpTips(input: string) {
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
              ic.content = ic.content.filter(i => inputMatch(i));
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

  highlightSearchTerm(stringToHighlight: string) {
    const {searchTerm} = this.state;
    if (searchTerm === '' || searchTerm.length < 3) {
      return stringToHighlight;
    }
    const words: string[] = [];
    let searchWords = searchTerm.split(new RegExp(',| '));
    searchWords = searchWords
    .filter(w => w.length > 0 )
    .map(word => word.replace(/[&!^\/\\#,+()$~%.'":*?<>{}]/g, ''));
    const matchString = new RegExp(searchWords.join('|'), 'i');
    const matches = stringToHighlight.match(new RegExp(matchString, 'gi'));
    const splits = stringToHighlight.split(new RegExp(matchString, 'gi'));
    if (matches) {
      for (let i = 0; i < matches.length; i++) {
        words.push(splits[i], matches[i]);
      }
      words.push(splits[splits.length - 1]);
    }
    return words.map((word, w) => <span key={w}
      style={matchString.test(word.toLowerCase()) ? styles.highlighted : {}}>
        {word}
      </span>);
  }

  onIconClick(icon: string) {
    const {activeIcon, sidebarOpen} = this.state;
    const newSidebarOpen = !(icon === activeIcon && sidebarOpen);
    if (newSidebarOpen) {
      this.setState({activeIcon: icon, sidebarOpen: newSidebarOpen});
    } else {
      this.hideSidebar();
    }
  }

  onInputChange(value: string) {
    this.setState({searchTerm: value});
    this.debounceInput(value);
  }

  hideSidebar() {
    this.setState({sidebarOpen: false});
    // keep activeIcon set while the sidebar transition completes
    setTimeout(() => {
      const {sidebarOpen} = this.state;
      // check if the sidebar has been opened again before resetting activeIcon
      if (!sidebarOpen) {
        this.setState({activeIcon: undefined});
      }
    }, 300);
  }

  render() {
    const {location, participant, setParticipant} = this.props;
    const {activeIcon, filteredContent, searchTerm, sidebarOpen} = this.state;
    const displayContent = filteredContent !== undefined ? filteredContent : sidebarContent[location];
    const contentStyle = (tab: string) => ({
      display: activeIcon === tab ? 'block' : 'none',
      height: 'calc(100% - 1rem)',
      overflow: 'auto',
      padding: '0.5rem',
    });
    return <React.Fragment>
      <div style={styles.iconContainer}>
        <div style={activeIcon === 'help' ? iconStyles.active : styles.icon}>
          <ClrIcon className='is-solid' shape='info-standard' size={28}
                   onClick={() => this.onIconClick('help')} />
        </div>
        <div style={iconStyles.disabled}>
          <ClrIcon className='is-solid' shape='book' size={32} />
        </div>
        {location === 'reviewParticipantDetail' &&
          <div style={activeIcon === 'annotations' ? iconStyles.active : styles.icon}>
            <ClrIcon shape='note' size={32}
                     onClick={() => this.onIconClick('annotations')} />
          </div>
        }
      </div>
      <div style={activeIcon ? {...styles.sidebarContainer, ...styles.sidebarContainerActive} : styles.sidebarContainer}>
        <div style={sidebarOpen ? {...styles.sidebar, ...styles.sidebarOpen} : styles.sidebar}>
          <div style={styles.topBar}>
            <ClrIcon shape='caret right' size={22} style={styles.closeIcon}
              onClick={() => this.hideSidebar()} />
          </div>
          <div style={contentStyle('help')}>
            <h3 style={{...styles.sectionTitle, marginTop: 0}}>Help Tips</h3>
            <div style={styles.textSearch}>
              <ClrIcon style={{color: colors.primary, margin: '0 0.3rem'}} shape='search' size={16} />
              <input
                type='text'
                style={styles.textInput}
                value={searchTerm}
                onChange={(e) => this.onInputChange(e.target.value)}
                placeholder={'Search'} />
            </div>
            {displayContent.map((section, s) => <div key={s}>
              <h3 style={styles.sectionTitle}>{this.highlightSearchTerm(section.title)}</h3>
              {section.content.map((content, c) => {
                return typeof content === 'string'
                  ? <p key={c} style={styles.contentItem}>{this.highlightSearchTerm(content)}</p>
                  : <div key={c}>
                    <h4 style={styles.contentTitle}>{this.highlightSearchTerm(content.title)}</h4>
                    {content.content.map((item, i) =>
                      <p key={i} style={styles.contentItem}>{this.highlightSearchTerm(item)}</p>)
                    }
                  </div>;
              })}
            </div>)}
          </div>
          <div style={contentStyle('annotations')}>
            {!!participant &&
              <SidebarContent participant={participant} setParticipant={() => setParticipant(participant)} />
            }
          </div>
        </div>
      </div>
    </React.Fragment>;
  }
}

@Component({
  selector: 'app-help-sidebar',
  template: '<div #root></div>',
})
export class HelpSidebarComponent extends ReactWrapperBase {
  @Input('location') location: Props['location'];
  @Input('participant') participant: Props['participant'];
  @Input('setParticipant') setParticipant: Props['setParticipant'];

  constructor() {
    super(HelpSidebar, ['location', 'participant', 'setParticipant']);
  }
}
