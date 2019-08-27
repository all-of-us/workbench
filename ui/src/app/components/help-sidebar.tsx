import {Component, Input} from '@angular/core';
import * as React from 'react';

import {ClrIcon} from 'app/components/icons';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles, ReactWrapperBase} from 'app/utils';

const styles = reactStyles({
  sidebar: {
    position: 'absolute',
    top: 0,
    right: '-45px',
    height: '100%',
    width: 'calc(14rem + 45px)',
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
    textAlign: 'center',
    height: '100%',
    width: '45px',
    background: colorWithWhiteness(colors.primary, 0.4),
    zIndex: 10010
  },
  icon: {
    color: colors.white,
    marginTop: '1.25rem',
    cursor: 'pointer'
  }
});

const contentStyles = {
  closed: {
    ...styles.content,
    marginRight: '-14rem',
  },
  open: {
    ...styles.content,
    marginRight: 0,
  },
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
  }

  render() {
    const {location} = this.props;
    const {activeIcon, sidebarOpen} = this.state;
    return <div style={styles.sidebar}>
      <div style={sidebarOpen ? contentStyles.open : contentStyles.closed}>
        <h3 style={{fontWeight: 600, margin: 0}}>Help Tips</h3>
      </div>
      <div style={styles.iconContainer}>
        <ClrIcon className='is-solid' shape='info-standard' size={28} style={styles.icon}
          onClick={() => this.setState({activeIcon: 'info', sidebarOpen: !sidebarOpen})} />
        <ClrIcon className='is-solid' shape='book' size={32} style={styles.icon}
          onClick={() => this.setState({activeIcon: 'book', sidebarOpen: !sidebarOpen})} />
        {location === 'review' &&
          <ClrIcon shape='note' size={32} style={styles.icon}
            onClick={() => this.setState({activeIcon: 'annotations', sidebarOpen: !sidebarOpen})} />
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
