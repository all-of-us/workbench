import {Component, Input} from '@angular/core';
import * as React from 'react';

import {ClrIcon} from 'app/components/icons';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase} from 'app/utils';

const sidebarContent = require('assets/json/help-sidebar.json')

const styles = reactStyles({
  sidebar: {
    position: 'absolute',
    top: 0,
    right: '-45px',
    height: '100%',
    width: 'calc(14rem + 85px)',
    overflow: 'hidden',
    color: colors.primary,
  },
  content: {
    position: 'absolute',
    top: 0,
    right: '45px',
    height: '100%',
    width: '14rem',
    padding: '0.5rem',
    background: colorWithWhiteness(colors.primary, .87),
    transition: 'margin-right 0.5s ease-out'
  },
  iconContainer: {
    position: 'absolute',
    top: 0,
    right: 0,
    height: '100%',
    width: '45px',
    background: colorWithWhiteness(colors.primary, 0.4),
    zIndex: 10010
  },
  icon: {
    color: colors.white,
    marginTop: '0.75rem',
    padding: '0.25rem 0',
    cursor: 'pointer',
    textAlign: 'center',
  },
  closeIcon: {
    cursor: 'pointer',
    position: 'absolute',
    margin: '-10px 0 0 -52px',
    width: '40px',
    borderBottomLeftRadius: '10px',
    borderTopLeftRadius: '10px',
    background: colorWithWhiteness(colors.primary, 0.4),
    color: colors.white,
    height: '40px',
    textAlign: 'center',
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
  }
});

const contentStyles = {
  closed: {
    ...styles.content,
    marginRight: 'calc(-14rem - 40px)',
  },
  open: {
    ...styles.content,
    marginRight: 0,
  },
};

const activeIconStyle = {
  ...styles.icon,
  background: colorWithWhiteness(colors.primary, 0.55)
};

interface Props {
  location: string;
}

interface State {
  activeIcon: string;
  sidebarOpen: boolean;
  searchTerm: string;
}
export class HelpSidebar extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      activeIcon: undefined,
      sidebarOpen: false,
      searchTerm: undefined
    };
    console.log(sidebarContent);
  }

  onIconClick = (icon: string) => {
    let {activeIcon, sidebarOpen} = this.state;
    sidebarOpen = icon === activeIcon ? !sidebarOpen : true;
    activeIcon = sidebarOpen ? icon : undefined;
    this.setState({activeIcon, sidebarOpen});
  }

  render() {
    const {location} = this.props;
    const {activeIcon, sidebarOpen} = this.state;
    return <div style={styles.sidebar}>
      <div style={sidebarOpen ? contentStyles.open : contentStyles.closed}>
        <div style={styles.closeIcon}
             onClick={() => this.setState({activeIcon: undefined, sidebarOpen: false})}>
          <ClrIcon shape='caret right' size={32} style={{marginTop: '3px'}}/>
        </div>
        <h3 style={{...styles.sectionTitle, marginTop: 0}}>Help Tips</h3>
        {sidebarContent[location].map((section, s) => <div key={s}>
          <h3 style={styles.sectionTitle}>{section.title}</h3>
          {section.content.map((content, c) => {
            return typeof content === 'string' ? <p key={c} style={styles.contentItem}>{content}</p>
              : <div>
                <h4 style={styles.contentTitle}>{content.title}</h4>
                {content.content.map((item, i) => <p key={i} style={styles.contentItem}>{item}</p>)}
              </div>;
          })}
        </div>)}
      </div>
      <div style={styles.iconContainer}>
        <div style={activeIcon === 'help' ? activeIconStyle : styles.icon}>
          <ClrIcon className='is-solid' shape='info-standard' size={28}
            onClick={() => this.onIconClick('help')} />
        </div>
        <div style={activeIcon === 'book' ? activeIconStyle : styles.icon}>
          <ClrIcon className='is-solid' shape='book' size={32}
            onClick={() => this.onIconClick('book')} />
        </div>
        {location === 'review' &&
          <div style={activeIcon === 'annotations' ? activeIconStyle : styles.icon}>
            <ClrIcon shape='note' size={32}
              onClick={() => this.onIconClick('annotations')} />
          </div>
        }
      </div>
    </div>;
  }
}

@Component({
  selector: 'app-help-sidebar',
  template: '<div #root></div>',
})
export class HelpSidebarComponent extends ReactWrapperBase {
  @Input('location') location: Props['location'];

  constructor() {
    super(HelpSidebar, ['location']);
  }
}
