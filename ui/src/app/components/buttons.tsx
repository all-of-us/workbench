import {styles as cardStyles} from 'app/components/card';
import {ClrIcon, SnowmanIcon} from 'app/components/icons';
import {Interactive as LocalInteractive} from 'app/components/interactive';
import {TooltipTrigger} from 'app/components/popups';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {reactStyles} from 'app/utils/index';
import {navigateAndPreventDefaultIfNoKeysPressed} from 'app/utils/navigation';
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

  slidingButtonContainer: {
    // Use position sticky so the FAB does not continue past the page footer. We
    // use a padding-bottom since when the FAB ignores margins in "fixed"
    // positioning mode but respects them in "absolute" mode.
    bottom: 0,
    paddingBottom: '1rem',
    position: 'sticky',

    // We use a flex container with flex-end to pin the FAB to the right.
    // To make this a client option, alternate this between flex-end/flex-start.
    display: 'flex',
    justifyContent: 'flex-end',
  },
  slidingButton: {
    boxShadow: '0 2px 5px 0 rgba(0,0,0,0.26), 0 2px 10px 0 rgba(0,0,0,0.16)',
    borderRadius: '3rem',
    backgroundColor: colors.accent,
    height: '1.8rem',
    minWidth: '1.8rem',
    cursor: 'pointer'
  },

  slidingButtonContent: {
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
  secondarySmall: {
    style: {
      ...styles.baseNew,
      backgroundColor: 'transparent',
      borderColor: colors.accent,
      borderRadius: 0,
      borderStyle: 'solid',
      borderWidth: '1px',
      color: colors.accent,
      fontSize: '10.5px',
      height: '30px',
      padding: '0 0.5rem'
    },
    disabledStyle: {
      borderColor: colorWithWhiteness(colors.dark, disabledAlpha),
      color: colorWithWhiteness(colors.dark, disabledAlpha)
    },
    hover: {
      borderColor: colorWithWhiteness(colors.accent, 0.4),
      color: colorWithWhiteness(colors.accent, 0.4)
    }
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

export const Clickable = ({as = 'div', disabled = false, onClick = null, propagateDataTestId = false, ...props}) => {
  // `fp.omit` used to prevent propagation of test IDs to the rendered child component.
  const childProps = propagateDataTestId ? props : fp.omit(['data-test-id'], props);
  return <Interactive
    as={as} {...childProps}
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

export const MenuItem = ({icon = null, tooltip = '', disabled = false, children, style = {}, ...props}) => {
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
        cursor: disabled ? 'not-allowed' : 'pointer',
        ...style
      }}
      hover={!disabled ? {backgroundColor: colorWithWhiteness(colors.accent, 0.92)} : undefined}
      {...props}
    >
      {icon && <ClrIcon shape={icon} style={{marginRight: 8}} size={15}/>}
      {children}
    </Clickable>
  </TooltipTrigger>;
};

export const IconButton = ({icon: Icon, style = {}, hover = {}, tooltip = '', disabled = false, ...props}) => {
  return <TooltipTrigger side='left' content={tooltip}>
    <LocalInteractive tagName='div'
                 style={{
                   color: disabled ? colors.disabled : colors.accent,
                   cursor: disabled ? 'auto' : 'pointer',
                   ...style
                 }}
                 hover={{color: !disabled && colorWithWhiteness(colors.accent, 0.2), ...hover}}
                 disabled={disabled}
                 {...props}>
        <Icon disabled={disabled} style={{marginLeft: '.5rem', ...style}}/>
      </LocalInteractive>
  </TooltipTrigger>;
};

export const SnowmanButton = ({...props}) => <IconButton icon={SnowmanIcon} {...props} propagateDataTestId={true}/>;

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

export const StyledAnchorTag = ({href, children, analyticsFn = null, style = {}, ...props}) => {
  const inlineAnchor = {
    display: 'inline-block',
    color: colors.accent
  };
  return <a href={href}
            onClick={e => {
              if (analyticsFn) {
                analyticsFn();
              }
              // This does same page navigation iff there is no key pressed and target is not set.
              if (props.target === undefined && !href.startsWith('https://') && !href.startsWith('http://')) {
                navigateAndPreventDefaultIfNoKeysPressed(e, href);
              }
            }}
            style={{...inlineAnchor, ...style}} {...props}>{children}</a>;
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
    return <div style={styles.slidingButtonContainer}>
      <div data-test-id='sliding-button'
           style={disable ? {...styles.slidingButton,
             ...styles.slidingButtonDisable} : styles.slidingButton}
           onMouseEnter={() => this.setState({hovering: true})}
           onMouseLeave={() => this.setState({hovering: false})}
           onClick={() => disable ? {} : this.props.submitFunction()}>
        <TooltipTrigger content={tooltipContent} disabled={!tooltip}>
          <div style={styles.slidingButtonContent}>
            <div style={hovering ? {...styles.slidingButtonText,
              ...styles.slidingButtonHovering} : styles.slidingButtonText}>
              {expanded}
            </div>
            <ClrIcon shape={iconShape} style={{height: '1.5rem', width: '1.5rem',
              marginRight: '.145rem'}}/>
          </div>
        </TooltipTrigger>
      </div>
    </div>;
  }
}
