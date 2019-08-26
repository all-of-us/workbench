import {Component, Input} from '@angular/core';
import * as React from 'react';

import {ClrIcon} from 'app/components/icons';
import colors from 'app/styles/colors';
import {reactStyles, ReactWrapperBase} from 'app/utils';

const styles = reactStyles({
  sidebar: {
    position: 'absolute',
    top: 0,
    right: 0,
    height: '100%',
    background: colors.secondary,
    padding: '0.25rem'
  },
  icon: {
    color: colors.white
  }
});

interface Props {
  location: string;
}

interface State {
  open: boolean;
  searchTerm: string;
}
export class HelpSidebar extends React.Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = {
      open: false,
      searchTerm: undefined
    };
  }

  render() {
    return <div style={styles.sidebar}>
      <div><ClrIcon className='is-solid' shape='info-standard' size={22} style={styles.icon}/></div>
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
