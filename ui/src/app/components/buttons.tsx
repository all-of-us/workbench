import {styles as cardStyles} from 'app/components/card';
import {ClrIcon} from 'app/components/icons';
import {TooltipTrigger} from 'app/components/popups';
import {IconComponent} from 'app/icons/icon';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils/index';
import * as fp from 'lodash/fp';
import * as React from 'react';
import * as Interactive from 'react-interactive';

export const styles = reactStyles({
  baseNew: {
    display: 'inline-flex', justifyContent: 'space-around', alignItems: 'center',
    minWidth: '3rem', maxWidth: '15rem',
    height: 50,
    fontWeight: 500, fontSize: 14, textTransform: 'uppercase', lineHeight: '18px',
    overflow: 'hidden', whiteSpace: 'nowrap', textOverflow: 'ellipsis',
    userSelect: 'none',
    margin: 0, padding: '0 22px',
    borderRadius: 5,
    boxSizing: 'border-box'
  },
  slidingButton: {
    boxShadow: '0 2px 5px 0 rgba(0,0,0,0.26), 0 2px 10px 0 rgba(0,0,0,0.16)',
    position: 'fixed',
    bottom: '1rem',
    right: '2.9rem',
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

  slidingButtonDisable: {
    backgroundColor: colorWithWhiteness(colors.dark, 0.4),
    cursor: 'not-allowed'
  },

  slidingButtonText: {
    padding: 0,
    fontWeight: 600,
    maxWidth: 0,
    overflow: 'hidden',
    textTransform: 'uppercase',
    transition: 'max-width 0.5s ease-out, padding 0.1s linear 0.2s',
    whiteSpace: 'pre'
  },

  slidingButtonHovering: {
    padding: '0 0.5rem',
    /* Note: Ideally this would not be hardcoded since the expanded text value is
     * dynamic. Unfortunately using unset or a higher max-width results in a
     * choppy transition. This constant will need to be increased or made dynamic
     * if we decide to use longer expanded messages. */
    maxWidth: '200px'
  }
});

const hoverAlpha = 0.2;
const disabledAlpha = 0.6;

const buttonVariants = {
  primary: {
    style: {
      ...styles.baseNew,
      borderRadius: '0.3rem',
      backgroundColor: colors.primary, color: colors.white,
    },
    disabledStyle: {backgroundColor: colorWithWhiteness(colors.dark, disabledAlpha)},
    hover: {backgroundColor: colorWithWhiteness(colors.primary, hoverAlpha)}
  },
  secondary: {
    style: {
      ...styles.baseNew,
      backgroundColor: 'transparent',
      color: colors.primary,
    },
    disabledStyle: {
      borderColor: colorWithWhiteness(colors.dark, disabledAlpha),
      color: colorWithWhiteness(colors.dark, disabledAlpha)
    },
    hover: {backgroundColor: colors.primary, color: colors.white}
  },
  secondaryLight: {
    style: {
      ...styles.baseNew,
      backgroundColor: 'transparent',
      color: colors.accent
    },
    disabledStyle: {
      borderColor: colorWithWhiteness(colors.dark, disabledAlpha),
      color: colorWithWhiteness(colors.dark, disabledAlpha)
    },
    hover: {color: colorWithWhiteness(colors.accent, 0.4)}
  },
  primaryOnDarkBackground: {
    style: {
      ...styles.baseNew,
      borderRadius: '0.2rem',
      backgroundColor: colors.primary, color: colors.white
    },
    disabledStyle: {backgroundColor: colorWithWhiteness(colors.dark, disabledAlpha)},
    hover: {backgroundColor: 'rgba(255,255,255,0.3)'}
  },
  secondaryOnDarkBackground: {
    style: {
      ...styles.baseNew,
      borderRadius: '0.2rem',
      backgroundColor: colors.secondary, color: colors.white
    },
    disabledStyle: {backgroundColor: colorWithWhiteness(colors.dark, disabledAlpha)},
    hover: {backgroundColor: colorWithWhiteness(colors.secondary, hoverAlpha)}
  },
  purplePrimary: {
    style: {
      ...styles.baseNew,
      backgroundColor: colors.primary, color: colors.white,
    },
    disabledStyle: {backgroundColor: colorWithWhiteness(colors.dark, disabledAlpha)},
    hover: {backgroundColor: colorWithWhiteness(colors.primary, hoverAlpha)}
  },
  purpleSecondary: {
    style: {
      ...styles.baseNew,
      backgroundColor: 'transparent',
      color: colors.primary,
    },
    disabledStyle: {
      borderColor: colorWithWhiteness(colors.dark, disabledAlpha),
      color: colorWithWhiteness(colors.dark, disabledAlpha)
    },
    hover: {backgroundColor: colorWithWhiteness(colors.primary, hoverAlpha),
      color: colors.white, borderColor: colorWithWhiteness(colors.primary, hoverAlpha)}
  },
  link: {
    style: {
      ...styles.baseNew,
      color: colors.accent
    },
    disabledStyle: {
      color: colorWithWhiteness(colors.dark, disabledAlpha)
    },
    hover: {color: colorWithWhiteness(colors.accent, 0.4)}
  }
};

const computeStyle = ({style = {}, hover = {}, disabledStyle = {}}, {disabled}) => {
  return {
    style: {...style, ...(disabled ? {cursor: 'not-allowed', ...disabledStyle} : {})},
    hover: disabled ? undefined : hover
  };
};

// Set data test id = '' in the child to prevent it from propegating to all children
export const Clickable = ({as = 'div', disabled = false, onClick = null, ...props}) => {
  return <Interactive
    // `fp.omit` used to prevent propagation of test IDs to the rendered child component.
    as={as} {...fp.omit(['data-test-id'], props)}
    onClick={(...args) => onClick && !disabled && onClick(...args)}
  />;
};

export const Button = ({type = 'primary', style = {}, disabled = false, ...props}) => {
  return <Clickable
    // `fp.omit` used to prevent propagation of test IDs to the rendered child component.
    disabled={disabled} {...fp.omit(['data-test-id'], props)}
    {...fp.merge(computeStyle(buttonVariants[type], {disabled}), {style})}
  />;
};

export const MenuItem = ({icon, tooltip = '', disabled = false, children, ...props}) => {
  return <TooltipTrigger side='left' content={tooltip}>
    <Clickable
      // data-test-id is the text within the MenuItem, with whitespace removed
      // and appended with '-menu-item'
      data-test-id={children.toString().replace(/\s/g, '') + '-menu-item'}
      disabled={disabled}
      style={{
        display: 'flex', alignItems: 'center', justifyContent: 'start',
        fontSize: 12, minWidth: 125, height: 32,
        color: disabled ? colorWithWhiteness(colors.dark, disabledAlpha) : 'black',
        padding: '0 12px',
        cursor: disabled ? 'not-allowed' : 'pointer'
      }}
      hover={!disabled ? {backgroundColor: colorWithWhiteness(colors.accent, 0.92)} : undefined}
      {...props}
    >
      <ClrIcon shape={icon} style={{marginRight: 8}} size={15}/>
      {children}
    </Clickable>
  </TooltipTrigger>;
};

export const IconButton = ({icon, style = {}, tooltip = '', disabled = false, ...props}) => {
  return <TooltipTrigger side='left' content={tooltip}>
    <Clickable
        data-test-id={icon}
        disabled={disabled}
        {...props}
    >
      <IconComponent icon={icon} disabled={disabled} style={style}/>
    </Clickable>
  </TooltipTrigger>;
};

const cardButtonBase = {
  style: {
    alignItems: 'flex-start', alignContent: 'left', fontWeight: 500,
    justifyContent: 'center', padding: '0 1rem', color: colors.accent,
  },
  disabledStyle: {color: colorWithWhiteness(colors.dark, disabledAlpha), cursor: 'not-allowed'}

};

const cardButtonStyle = {
  large: {
    style: {
      ...cardStyles.workspaceCard, ...cardButtonBase.style,
      boxShadow: '0 0 2px 0 rgba(0, 0, 0, 0.12), 0 3px 2px 0 rgba(0, 0, 0, 0.12)',
      fontSize: 20, lineHeight: '28px',
    },
    disabledStyle: {...cardButtonBase.disabledStyle}
  },

  small: {
    style : {
      ...cardStyles.resourceCard, ...cardButtonBase.style,
      fontSize: 18, lineHeight: '22px',
      minWidth: '200px', maxWidth: '200px', minHeight: '105px', maxHeight: '105px',
      marginTop: '1rem', marginRight: '1rem',
    },
    disabledStyle: {...cardButtonBase.disabledStyle}
  }
};

export const CardButton = ({type = 'large', disabled = false, style = {}, children, ...props}) => {
  return <Clickable
    disabled={disabled} {...props}
    {...fp.merge(computeStyle(cardButtonStyle[type], {disabled}), {style})}
  >{children}</Clickable>;
};

const tabButtonStyle = {
  style: {
    margin: '0 1rem',
    textAlign: 'center',
    color: colors.accent,
    fontSize: '16px',
    lineHeight: '28px',
  },
  hover: {},
  disabledStyle: {}
};

const activeTabButtonStyle = {
  style: {
    borderBottom: `4px solid ${colors.accent}`,
    fontWeight: 600
  }
};

export const TabButton = ({disabled = false, style = {}, active = false, children, ...props}) => {
  const tabButtonStyleMerged = {
    style: {...tabButtonStyle.style, ...(active ? activeTabButtonStyle.style : {})},
    hover: tabButtonStyle.hover,
    disabledStyle: tabButtonStyle.disabledStyle,
  };
  return <Clickable
    disabled={disabled} {...props}
    {...fp.merge(computeStyle(tabButtonStyleMerged, {disabled}), {style})}
  >{children}</Clickable>;
};

export const Link = ({disabled = false, style = {}, children, ...props}) => {
  const linkStyle = {
    style: {color: colors.accent},
    hover: {textDecoration: 'underline'}
  };
  return <Clickable
      disabled={disabled} {...props}
      {...fp.merge(computeStyle(linkStyle, {disabled}), {style})}
  >{children}</Clickable>;
};

interface SlidingFabState {
  hovering: boolean;
}

interface SlidingFabProps {
  submitFunction: Function;
  expanded: string;
  disable: boolean;
  iconShape: string;
  tooltip?: boolean;
  tooltipContent?: JSX.Element;
}

export class SlidingFabReact extends React.Component<SlidingFabProps, SlidingFabState> {

  constructor(props) {
    super(props);
    this.state = {hovering: false};
  }

  render() {
    const {hovering} = this.state;
    const {expanded, disable, iconShape, tooltip, tooltipContent} = this.props;
    return <div data-test-id='sliding-button'
                style={disable ? {...styles.slidingButton,
                  ...styles.slidingButtonDisable} : styles.slidingButton}
                onMouseEnter={() => this.setState({hovering: true})}
                onMouseLeave={() => this.setState({hovering: false})}
                onClick={() => disable ? {} : this.props.submitFunction()}>
      <TooltipTrigger content={tooltipContent} disabled={!tooltip}>
        <div style={styles.slidingButtonContainer}>
          <div style={hovering ? {...styles.slidingButtonText,
            ...styles.slidingButtonHovering} : styles.slidingButtonText}>
            {expanded}
          </div>
          <ClrIcon shape={iconShape} style={{height: '1.5rem', width: '1.5rem',
            marginRight: '.145rem'}}/>
        </div>
      </TooltipTrigger>
    </div>;
  }
}
