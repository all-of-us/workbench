import * as React from 'react';


import {ClrIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils';

const styles = reactStyles({
  slidingButton: {
    boxShadow: '0 2px 5px 0 rgba(0,0,0,0.26), 0 2px 10px 0 rgba(0,0,0,0.16)',
    position: 'fixed',
    bottom: '1rem',
    right: '1rem',
    borderRadius: '3rem',
    backgroundColor: colors.accent,
    height: '1.8rem',
    minWidth: '1.8rem',
    cursor: 'pointer'
  },

  slidingButtonContainer: {
    color: colors.white,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    height: '100%'
  },

  disable: {
    backgroundColor: colorWithWhiteness(colors.dark, 0.4),
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

interface State {
  hovering: boolean;
}

interface Props {
  submitFunction: Function;
  expanded: string;
  disable: boolean;
  iconShape: string;
  tooltip?: boolean;
  tooltipContent?: JSX.Element;
}

export class SlidingFabReact extends React.Component<Props, State> {

  constructor(props) {
    super(props);
    this.state = {hovering: false};
  }

  render() {
    const {hovering} = this.state;
    const {expanded, disable, iconShape, tooltip, tooltipContent} = this.props;
    return <div data-test-id='sliding-button'
      style={disable ? {...styles.slidingButton, ...styles.disable} : styles.slidingButton}
      onMouseEnter={() => this.setState({hovering: true})}
      onMouseLeave={() => this.setState({hovering: false})}
      onClick={() => disable ? {} : this.props.submitFunction()}>
      <TooltipTrigger content={tooltipContent} disabled={!tooltip}>
        <div style={styles.slidingButtonContainer}>
          <div style={hovering ? {...styles.text, ...styles.hovering} : styles.text}>
            {expanded}
          </div>
          <ClrIcon shape={iconShape} style={{height: '1.5rem', width: '1.5rem',
            marginRight: '.145rem'}}/>
        </div>
      </TooltipTrigger>
    </div>;
  }
}

