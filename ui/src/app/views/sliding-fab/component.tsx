import * as React from 'react';

import {Component, Input} from '@angular/core';

import {ClrIcon} from 'app/components/icons';
import {reactStyles, ReactWrapperBase} from 'app/utils';

const styles = reactStyles({
  slidingButton: {
    boxShadow: '0 2px 5px 0 rgba(0,0,0,0.26), 0 2px 10px 0 rgba(0,0,0,0.16)',
    position: 'fixed',
    bottom: '1rem',
    right: '1rem',
    color: 'white',
    borderRadius: '3rem',
    backgroundColor: '#2691D0',
    height: '1.8rem',
    minWidth: '1.8rem',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    cursor: 'pointer'
  },

  disable: {
    backgroundColor: 'grey',
    cursor: 'not-allowed'
  },

  text: {
    padding: 0,
    fontWeight: 600,
    maxWidth: 0,
    overflow: 'hidden',
    textTransform: 'uppercase',
    transition: 'max-width 0.5s ease-out, padding 0.1s linear 0.2s',
    whiteSpace: 'pre'
  },

  hovering: {
    padding: '0 0.5rem',
    /* Note: Ideally this would not be hardcoded since the expanded text value is
     * dynamic. Unfortunately using unset or a higher max-width results in a
     * choppy transition. This constant will need to be increased or made dynamic
     * if we decide to use longer expanded messages. */
    maxWidth: '200px'
  }
});

export class SlidingFabReact extends React.Component<{submitFunction: Function,
  expanded: string, disable: boolean, iconShape: string}, {hovering: boolean}> {

  constructor(props: {submitFunction, expanded, disable, iconShape}) {
    super(props);
    this.state = {hovering: false};
  }

  render() {
    const {hovering} = this.state;
    const {expanded, disable, iconShape} = this.props;
    return <div data-test-id='sliding-button'
      style={disable ? {...styles.slidingButton, ...styles.disable} : styles.slidingButton}
      onMouseEnter={() => this.setState({hovering: true})}
      onMouseLeave={() => this.setState({hovering: false})}
      onClick={() => disable ? {} : this.props.submitFunction()}>
      <div style={hovering ? {...styles.text, ...styles.hovering} : styles.text}>{expanded}</div>
      <ClrIcon shape={iconShape} style={{height: '1.5rem', width: '1.5rem',
        marginRight: '.145rem'}}/>
    </div>;
  }
}

@Component({
  selector: 'app-sliding-fab',
  styleUrls: [],
  template: '<div #root></div>',
})
export class SlidingFabComponent extends ReactWrapperBase {
  @Input('submitFunction') submitFunction: Function;
  @Input('iconShape') iconShape: string;
  @Input('expanded') expanded: string;
  @Input('disable') disable: boolean;

  constructor() {
    super(SlidingFabReact, ['submitFunction', 'expanded', 'disable', 'iconShape']);
  }
}
