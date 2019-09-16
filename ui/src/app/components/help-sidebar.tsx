import {Component, Input} from '@angular/core';
import * as fp from 'lodash/fp';
import * as React from 'react';

import {ClrIcon} from 'app/components/icons';
import {SidebarContent} from 'app/pages/data/cohort-review/sidebar-content.component';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {highlightSearchTerm, reactStyles, ReactWrapperBase, withUserProfile} from 'app/utils';
import {openZendeskWidget} from 'app/utils/zendesk';

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
    color: colors.primary,
  },
  textInput: {
    width: '85%',
    height: '1.5rem',
    padding: '0 0 0 5px',
    border: 0,
    backgroundColor: 'transparent',
    outline: 'none',
  },
  footer: {
    position: 'absolute',
    bottom: 0,
    padding: '1rem 1.5rem 1.5rem 1rem',
    background: colorWithWhiteness(colors.primary, .8),
  },
  link: {
    color: colors.accent,
    cursor: 'pointer',
    textDecoration: 'none'
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
  profileState: any;
  participant?: any;
  setParticipant?: Function;
}

interface State {
  activeIcon: string;
  filteredContent: any;
  sidebarOpen: boolean;
  searchTerm: string;
}
export const HelpSidebar = withUserProfile()(
  class extends React.Component<Props, State> {
    constructor(props: Props) {
      super(props);
      this.state = {
        activeIcon: undefined,
        filteredContent: undefined,
        sidebarOpen: false,
        searchTerm: undefined
      };
      console.log(props);
    }

    debounceInput = fp.debounce(300, (input: string) => {
      if (input.length < 3) {
        this.setState({filteredContent: undefined});
      } else {
        this.searchHelpTips(input.trim().toLowerCase());
      }
    });

    searchHelpTips(input: string) {
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

    openContactWidget() {
      const {profileState: {profile: {contactEmail, familyName, givenName, username}}} = this.props;
      openZendeskWidget(givenName, familyName, username, contactEmail);
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

    highlightMatches(content: string) {
      const {searchTerm} = this.state;
      return highlightSearchTerm(searchTerm, content, colors.success);
    }

    render() {
      const {location, participant, setParticipant} = this.props;
      const {activeIcon, filteredContent, searchTerm, sidebarOpen} = this.state;
      const displayContent = filteredContent !== undefined ? filteredContent : sidebarContent[location];
      const contentStyle = (tab: string) => ({
        display: activeIcon === tab ? 'block' : 'none',
        height: 'calc(100% - 1rem)',
        overflow: 'auto',
        padding: '0.5rem 0.5rem 6.5rem',
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
                <ClrIcon style={{color: colors.primary, margin: '0 0.25rem'}} shape='search' size={16} />
                <input
                  type='text'
                  style={styles.textInput}
                  value={searchTerm}
                  onChange={(e) => this.onInputChange(e.target.value)}
                  placeholder={'Search'} />
              </div>
              {displayContent.length > 0
                ? displayContent.map((section, s) => <div key={s}>
                  <h3 style={styles.sectionTitle}>{this.highlightMatches(section.title)}</h3>
                  {section.content.map((content, c) => {
                    return typeof content === 'string'
                      ? <p key={c} style={styles.contentItem}>{this.highlightMatches(content)}</p>
                      : <div key={c}>
                        <h4 style={styles.contentTitle}>{this.highlightMatches(content.title)}</h4>
                        {content.content.map((item, i) =>
                          <p key={i} style={styles.contentItem}>{this.highlightMatches(item)}</p>)
                        }
                      </div>;
                  })}
                </div>)
                : <div style={{marginTop: '0.5rem'}}><em>No results found</em></div>
              }
            </div>
            <div style={contentStyle('annotations')}>
              {!!participant &&
                <SidebarContent participant={participant} setParticipant={() => setParticipant(participant)} />
              }
            </div>
            <div style={styles.footer}>
              <h3 style={{...styles.sectionTitle, marginTop: 0}}>Not finding what you're looking for?</h3>
              <p style={styles.contentItem}>
                Visit the
                <a style={styles.link}
                  href='https://www.researchallofus.org/frequently-asked-questions/'
                  target='_blank'> FAQs here </a>
                or <span style={styles.link}
                  onClick={() => this.openContactWidget()}> contact us</span>.
              </p>
            </div>
          </div>
        </div>
      </React.Fragment>;
    }
  }
);

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
