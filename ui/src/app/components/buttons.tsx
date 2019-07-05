import {styles as cardStyles} from 'app/components/card';
import {TooltipTrigger} from 'app/components/popups';
import {IconComponent} from 'app/icons/icon';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import * as fp from 'lodash/fp';
import * as React from 'react';
import * as Interactive from 'react-interactive';

export const styles = {
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
  }
};

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
  // Don't propagate test IDs to the rendered child component.
  delete props['data-test-id'];
  return <Interactive
    as={as} {...props}
    onClick={(...args) => onClick && !disabled && onClick(...args)}
  />;
};

export const Button = ({type = 'primary', style = {}, disabled = false, ...props}) => {
  // Don't propagate test IDs to the rendered child component.
  delete props['data-test-id'];
  return <Clickable
    disabled={disabled} {...props}
    {...fp.merge(computeStyle(buttonVariants[type], {disabled}), {style})}
  />;
};

export const MenuItem = ({tooltip = '', disabled = false, children, ...props}) => {
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
