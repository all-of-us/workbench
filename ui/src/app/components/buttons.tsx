import * as React from 'react';

const styles = {
  base: ({ disabled }) => ({
    display: 'inline-flex', justifyContent: 'space-around', alignItems: 'center',
    height: '1.5rem', minWidth: '3rem', maxWidth: '15rem',
    fontWeight: 500, fontSize: 12, letterSpacing: '0.02rem', textTransform: 'uppercase',
    overflow: 'hidden', whiteSpace: 'nowrap', textOverflow: 'ellipsis',
    cursor: disabled ? undefined : 'pointer', userSelect: 'none',
    margin: 0, padding: '0rem 0.77rem',
  }),
  primary: ({ hover, disabled }) => ({
    border: 'none',
    backgroundColor: disabled ? '#c3c3c3' : (hover ? '#4356A7' : '#262262'),
    borderRadius: '0.3rem',
    color: '#fff',
  }),
  secondary: ({ hover, disabled }) => ({
    border: '2px solid',
    borderColor: disabled ? '#c3c3c3' : '#262262',
    backgroundColor: disabled ? '#f1f2f2' : (hover ? '#262262' : 'transparent'),
    borderRadius: '0.2rem',
    color: disabled ? '#c3c3c3' : (hover ? '#ffffff' : '#262262'),
    padding: '0.5rem 0.77rem',
    cursor: 'pointer',
    textTransform: 'uppercase',
    letterSpacing: '0.02rem',
    lineHeight: '0.77rem'
  }),
  darklingPrimary: ({ hover, disabled }) => ({
    backgroundColor: disabled ? '#c3c3c3' : (hover ? 'rgba(255,255,255,0.3)' : '#262262'),
    color: '#ffffff',
    border: '0px',
    borderRadius: '0.2rem'
  }),
  darklingSecondary: ({ hover, disabled }) => ({
    backgroundColor: disabled ? '#c3c3c3' : (hover ? '#50ACE1' : '#0079b8'),
    color: '#ffffff',
    border: '0px',
    borderRadius: '0.2rem'
  })
};

/*
 * Uses the function-as-child technique. The children function is called with:
 *   hover: the current hover state
 *   trackHover: transforms an element, adds hooks to track hover state
 */
class HoverContainer extends React.Component<
  { children: ({ hover: boolean, trackHover: Function }) => React.ReactNode },
  { hover: boolean }
> {
  constructor(props) {
    super(props);
    this.state = { hover: false };
  }

  trackHover = el => {
    return React.cloneElement(el, {
      onMouseEnter: (...args) => {
        if (el.props.onMouseEnter) {
          el.props.onMouseEnter(...args);
        }
        this.setState({ hover: true });
      },
      onMouseLeave: (...args) => {
        if (el.props.onMouseLeave) {
          el.props.onMouseLeave(...args);
        }
        this.setState({ hover: false });
      }
    });
  }

  render() {
    const { children } = this.props;
    const { hover } = this.state;
    return children({ hover, trackHover: this.trackHover });
  }
}

export const Clickable = ({ disabled = false, onClick = null, ...props }) => {
  return <div
    {...props}
    onClick={(...args) => onClick && !disabled && onClick(...args)}
  />;
};

export const Button = ({ type = 'primary', style = {}, disabled = false, ...props }) => {
  return <HoverContainer>
    {({ hover, trackHover }) => {
      return trackHover(<Clickable
        {...{ disabled, ...props }}
        style={{ ...styles.base({ disabled }), ...styles[type]({ hover, disabled }), ...style }}
      />);
    }}
  </HoverContainer>;
};

export const Secondarybutton = ({ type = 'secondary', style = {}, disabled = false, ...props }) => {
  return <HoverContainer>
    {({ hover, trackHover }) => {
      return trackHover(<Clickable
          {...{ disabled, ...props }}
          style={{ ...styles.base({ disabled }), ...styles[type]({ hover, disabled }), ...style }}
      />);
    }}
  </HoverContainer>;
};
