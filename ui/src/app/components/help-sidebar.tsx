import {Component, Input} from '@angular/core';
import {faEdit} from '@fortawesome/free-regular-svg-icons';
import {faInfoCircle} from '@fortawesome/free-solid-svg-icons';
import {FontAwesomeIcon} from '@fortawesome/react-fontawesome';
import * as fp from 'lodash/fp';
import * as React from 'react';
import {Subscription} from 'rxjs/Subscription';

import {ClrIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import {SidebarContent} from 'app/pages/data/cohort-review/sidebar-content.component';
import {participantStore} from 'app/services/review-state.service';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {highlightSearchTerm, reactStyles, ReactWrapperBase, withUserProfile} from 'app/utils';
import {openZendeskWidget} from 'app/utils/zendesk';
import {ParticipantCohortStatus} from 'generated/fetch';

const proIcons = {
  thunderstorm: '/assets/icons/thunderstorm-solid.svg'
};
const sidebarContent = require('assets/json/help-sidebar.json');

const styles = reactStyles({
  sidebarContainer: {
    position: 'absolute',
    top: '60px',
    right: 0,
    height: 'calc(100% - 60px)',
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
    top: '60px',
    right: 0,
    height: 'calc(100% - 60px)',
    minHeight: 'calc(100vh - 156px)',
    width: '45px',
    background: colorWithWhiteness(colors.primary, 0.4),
    zIndex: 101
  },
  icon: {
    background: colorWithWhiteness(colors.primary, 0.48),
    color: colors.white,
    display: 'table-cell',
    height: '46px',
    width: '45px',
    borderBottom: `1px solid ${colorWithWhiteness(colors.primary, 0.4)}`,
    cursor: 'pointer',
    textAlign: 'center',
    transition: 'background 0.2s linear',
    verticalAlign: 'middle'
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
    padding: '1.25rem 0.75rem',
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
    cursor: 'not-allowed'
  }
};

const icons = [{
  id: 'help',
  disabled: false,
  faIcon: faInfoCircle,
  page: null,
  style: {fontSize: '22px'},
  tooltip: 'Help Tips',
}, {
  id: 'thunderstorm',
  disabled: true,
  faIcon: null,
  page: null,
  style: {height: '26px', width: '26px', marginTop: '0.25rem', opacity: 0.5},
  tooltip: 'Compute Configuration',
}, {
  id: 'annotations',
  disabled: false,
  faIcon: faEdit,
  page: 'reviewParticipantDetail',
  style: {fontSize: '20px', marginLeft: '5px'},
  tooltip: 'Annotations',
}];

interface Props {
  helpContent: string;
  setSidebarState: Function;
  sidebarOpen: boolean;
  profileState: any;
}

interface State {
  activeIcon: string;
  filteredContent: Array<any>;
  participant: ParticipantCohortStatus;
  searchTerm: string;
  tooltipId: number;
}
export const HelpSidebar = withUserProfile()(
  class extends React.Component<Props, State> {
    subscription: Subscription;
    constructor(props: Props) {
      super(props);
      this.state = {
        activeIcon: undefined,
        filteredContent: undefined,
        participant: undefined,
        searchTerm: '',
        tooltipId: undefined
      };
    }

    debounceInput = fp.debounce(300, (input: string) => {
      if (input.length < 3) {
        this.setState({filteredContent: undefined});
      } else {
        this.searchHelpTips(input.trim().toLowerCase());
      }
    });

    componentDidMount(): void {
      this.subscription = participantStore.subscribe(participant => this.setState({participant}));
    }

    componentDidUpdate(prevProps: Readonly<Props>): void {
      // close the sidebar on each navigation excluding navigating between participants in cohort review
      if (!this.props.sidebarOpen && prevProps.sidebarOpen) {
        setTimeout(() => {
          // check if the sidebar has been opened again before resetting activeIcon
          if (!this.props.sidebarOpen) {
            this.setState({activeIcon: undefined});
          }
        }, 300);
      }
    }

    componentWillUnmount(): void {
      this.subscription.unsubscribe();
    }

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
      const {setSidebarState, sidebarOpen} = this.props;
      const {activeIcon} = this.state;
      const newSidebarOpen = !(icon === activeIcon && sidebarOpen);
      if (newSidebarOpen) {
        this.setState({activeIcon: icon});
        setSidebarState(true);
      } else {
        setSidebarState(false);
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

    highlightMatches(content: string) {
      const {searchTerm} = this.state;
      return highlightSearchTerm(searchTerm, content, colors.success);
    }

    render() {
      const {helpContent, setSidebarState, sidebarOpen} = this.props;
      const {activeIcon, filteredContent, participant, searchTerm, tooltipId} = this.state;
      const displayContent = filteredContent !== undefined ? filteredContent : sidebarContent[helpContent];
      const contentStyle = (tab: string) => ({
        display: activeIcon === tab ? 'block' : 'none',
        height: 'calc(100% - 1rem)',
        overflow: 'auto',
        padding: '0.5rem 0.5rem 5.5rem',
      });
      return <React.Fragment>
        <div style={styles.iconContainer}>
          {icons.map((icon, i) => (!icon.page || icon.page === helpContent) && <div key={i} style={{display: 'table'}}>
            <TooltipTrigger disabled={tooltipId !== i} content={<div>{icon.tooltip}</div>}>
              <div style={activeIcon === icon.id ? iconStyles.active : icon.disabled ? iconStyles.disabled : styles.icon}
                   onMouseOver={() => this.setState({tooltipId: i})}
                   onMouseOut={() => this.setState({tooltipId: undefined})}>
                {icon.faIcon === null
                  ? <img src={proIcons[icon.id]} style={icon.style} />
                  : <FontAwesomeIcon icon={icon.faIcon} style={icon.style} onClick={() => this.onIconClick(icon.id)} />
                }
              </div>
            </TooltipTrigger>
          </div>)}
        </div>
        <div style={activeIcon ? {...styles.sidebarContainer, ...styles.sidebarContainerActive} : styles.sidebarContainer}>
          <div style={sidebarOpen ? {...styles.sidebar, ...styles.sidebarOpen} : styles.sidebar} data-test-id='sidebar-content'>
            <div style={styles.topBar}>
              <ClrIcon shape='caret right' size={22} style={styles.closeIcon} onClick={() => setSidebarState(false)} />
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
              {!!displayContent && displayContent.length > 0
                ? displayContent.map((section, s) => <div key={s}>
                  <h3 style={styles.sectionTitle} data-test-id={`section-title-${s}`}>{this.highlightMatches(section.title)}</h3>
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
              {participant && <SidebarContent />}
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
  @Input('helpContent') helpContent: Props['helpContent'];
  @Input('setSidebarState') setSidebarState: Props['setSidebarState'];
  @Input('sidebarOpen') sidebarOpen: Props['sidebarOpen'];

  constructor() {
    super(HelpSidebar, ['helpContent', 'setSidebarState', 'sidebarOpen']);
  }
}
