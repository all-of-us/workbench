import * as React from 'react';
import { CSSProperties } from 'react';
import Interactive from 'react-interactive';
import { Link } from 'react-router-dom';
import { HashLink } from 'react-router-hash-link';
import * as fp from 'lodash/fp';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { Interactive as TerraUIInteractive } from '@terra-ui-packages/components';
import { RouteLink } from 'app/components/app-router';
import { styles as cardStyles } from 'app/components/card';
import { CircleEllipsisIcon, ClrIcon, SnowmanIcon } from 'app/components/icons';
import { TooltipTrigger } from 'app/components/popups';
import colors, { colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';
import times from 'assets/icons/times-light.svg';

export interface LinkLocationState {
  pathname: string;
}

export const styles = reactStyles({
  baseNew: {
    display: 'inline-flex',
    justifyContent: 'space-around',
    alignItems: 'center',
    minWidth: '4.5rem',
    maxWidth: '22.5rem',
    height: 50,
    fontWeight: 500,
    fontSize: 14,
    textTransform: 'uppercase',
    lineHeight: '18px',
    overflow: 'hidden',
    whiteSpace: 'nowrap',
    textOverflow: 'ellipsis',
    userSelect: 'none',
    margin: 0,
    padding: '0 22px',
    borderRadius: 5,
    boxSizing: 'border-box',
  },
  inlineAnchor: {
    display: 'inline-block',
    color: colors.accent,
  },
  slidingButtonContainer: {
    // Use position sticky so the FAB does not continue past the page footer. We
    // use a padding-bottom since when the FAB ignores margins in "fixed"
    // positioning mode but respects them in "absolute" mode.
    bottom: 0,
    paddingBottom: '1.5rem',
    position: 'sticky',

    // We use a flex container with flex-end to pin the FAB to the right.
    // To make this a client option, alternate this between flex-end/flex-start.
    display: 'flex',
    justifyContent: 'flex-end',
  },
  slidingButton: {
    boxShadow: '0 2px 5px 0 rgba(0,0,0,0.26), 0 2px 10px 0 rgba(0,0,0,0.16)',
    borderRadius: '4.5rem',
    backgroundColor: colors.accent,
    height: '2.7rem',
    minWidth: '2.7rem',
    cursor: 'pointer',
  },

  slidingButtonContent: {
    color: colors.white,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    height: '100%',
  },

  slidingButtonDisable: {
    backgroundColor: colorWithWhiteness(colors.dark, 0.4),
    cursor: 'not-allowed',
  },

  slidingButtonText: {
    padding: 0,
    fontWeight: 600,
    maxWidth: 0,
    overflow: 'hidden',
    textTransform: 'uppercase',
    transition: 'max-width 0.5s ease-out, padding 0.1s linear 0.2s',
    whiteSpace: 'pre',
  },

  slidingButtonHovering: {
    padding: '0 0.75rem',
    /* Note: Ideally this would not be hardcoded since the expanded text value is
     * dynamic. Unfortunately using unset or a higher max-width results in a
     * choppy transition. This constant will need to be increased or made dynamic
     * if we decide to use longer expanded messages. */
    maxWidth: '200px',
  },
});

const hoverAlpha = 0.2;
const disabledAlpha = 0.6;

const buttonVariants = {
  primary: {
    style: {
      ...styles.baseNew,
      borderRadius: '0.45rem',
      backgroundColor: colors.primary,
      color: colors.white,
    },
    disabledStyle: {
      backgroundColor: colorWithWhiteness(colors.dark, disabledAlpha),
    },
    hover: { backgroundColor: colorWithWhiteness(colors.primary, hoverAlpha) },
  },
  secondary: {
    style: {
      ...styles.baseNew,
      backgroundColor: 'transparent',
      color: colors.primary,
    },
    disabledStyle: {
      borderColor: colorWithWhiteness(colors.dark, disabledAlpha),
      color: colorWithWhiteness(colors.dark, disabledAlpha),
    },
    hover: { backgroundColor: colors.primary, color: colors.white },
  },
  secondaryOutline: {
    style: {
      ...styles.baseNew,
      backgroundColor: 'transparent',
      color: colors.primary,
      borderStyle: 'solid',
      borderWidth: '1px',
    },
    disabledStyle: {
      borderColor: colorWithWhiteness(colors.dark, disabledAlpha),
      color: colorWithWhiteness(colors.dark, disabledAlpha),
    },
    hover: { backgroundColor: colors.primary, color: colors.white },
  },
  secondaryLight: {
    style: {
      ...styles.baseNew,
      backgroundColor: 'transparent',
      color: colors.accent,
    },
    disabledStyle: {
      borderColor: colorWithWhiteness(colors.dark, disabledAlpha),
      color: colorWithWhiteness(colors.dark, disabledAlpha),
    },
    hover: { color: colorWithWhiteness(colors.accent, 0.4) },
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
      padding: '0 0.75rem',
    },
    disabledStyle: {
      borderColor: colorWithWhiteness(colors.dark, disabledAlpha),
      color: colorWithWhiteness(colors.dark, disabledAlpha),
    },
    hover: {
      borderColor: colorWithWhiteness(colors.accent, 0.4),
      color: colorWithWhiteness(colors.accent, 0.4),
    },
  },
  primaryOnDarkBackground: {
    style: {
      ...styles.baseNew,
      borderRadius: '0.3rem',
      backgroundColor: colors.primary,
      color: colors.white,
    },
    disabledStyle: {
      backgroundColor: colorWithWhiteness(colors.dark, disabledAlpha),
    },
    hover: { backgroundColor: 'rgba(255,255,255,0.3)' },
  },
  secondaryOnDarkBackground: {
    style: {
      ...styles.baseNew,
      borderRadius: '0.3rem',
      backgroundColor: colors.secondary,
      color: colors.white,
    },
    disabledStyle: {
      backgroundColor: colorWithWhiteness(colors.dark, disabledAlpha),
    },
    hover: {
      backgroundColor: colorWithWhiteness(colors.secondary, hoverAlpha),
    },
  },
  purplePrimary: {
    style: {
      ...styles.baseNew,
      backgroundColor: colors.primary,
      color: colors.white,
    },
    disabledStyle: {
      backgroundColor: colorWithWhiteness(colors.dark, disabledAlpha),
    },
    hover: { backgroundColor: colorWithWhiteness(colors.primary, hoverAlpha) },
  },
  purpleSecondary: {
    style: {
      ...styles.baseNew,
      backgroundColor: 'transparent',
      color: colors.primary,
    },
    disabledStyle: {
      borderColor: colorWithWhiteness(colors.dark, disabledAlpha),
      color: colorWithWhiteness(colors.dark, disabledAlpha),
    },
    hover: {
      backgroundColor: colorWithWhiteness(colors.primary, hoverAlpha),
      color: colors.white,
      borderColor: colorWithWhiteness(colors.primary, hoverAlpha),
    },
  },
  link: {
    style: {
      ...styles.baseNew,
      color: colors.accent,
    },
    disabledStyle: {
      color: colorWithWhiteness(colors.dark, disabledAlpha),
    },
    hover: { color: colorWithWhiteness(colors.accent, 0.4) },
  },
};

const computeStyle = (
  { style = {}, hover = {}, disabledStyle = {} },
  { disabled }
) => {
  return {
    style: {
      ...style,
      ...(disabled ? { cursor: 'not-allowed', ...disabledStyle } : {}),
    },
    hover: disabled ? undefined : hover,
  };
};

interface ClickableProps {
  as?: string | React.ReactElement<any, any> | React.FC;
  disabled?: boolean;
  onClick?: any;
  propagateDataTestId?: boolean;
  [key: string]: any;
}

export const Clickable = ({
  as = 'div',
  disabled = false,
  onClick = null,
  propagateDataTestId = false,
  ...props
}: ClickableProps) => {
  // `fp.omit` used to prevent propagation of test IDs to the rendered child component.
  const childProps = propagateDataTestId
    ? props
    : fp.omit(['data-test-id'], props);
  return (
    <Interactive
      as={as}
      {...childProps}
      onClick={(...args) => onClick && !disabled && onClick(...args)}
    />
  );
};

export const Button = ({
  children,
  path = '',
  type = 'primary',
  style = {},
  disabled = false,
  propagateDataTestId = false,
  ...props
}) => {
  // `fp.omit` used to prevent propagation of test IDs to the rendered child component.
  const childProps = propagateDataTestId
    ? props
    : fp.omit(['data-test-id'], props);
  const computedStyle = fp.merge(
    computeStyle(buttonVariants[type], { disabled }),
    { style }
  );
  return path ? (
    <RouteLink path={path} {...computedStyle}>
      <Clickable disabled={disabled} {...childProps}>
        {children}
      </Clickable>
    </RouteLink>
  ) : (
    <Clickable disabled={disabled} {...computedStyle} {...childProps}>
      {children}
    </Clickable>
  );
};

export const ButtonWithLocationState = ({
  children,
  path = '',
  type = 'primary',
  style = {},
  disabled = false,
  propagateDataTestId = false,
  ...props
}) => {
  // `fp.omit` used to prevent propagation of test IDs to the rendered child component.
  const childProps = propagateDataTestId
    ? props
    : fp.omit(['data-test-id'], props);
  const computedStyle = fp.merge(
    computeStyle(buttonVariants[type], { disabled }),
    { style }
  );
  return (
    <RouteLink
      disabled={disabled}
      path={{
        pathname: path,
        state: { pathname: location.pathname } as LinkLocationState,
      }}
      {...computedStyle}
    >
      <Clickable
        style={disabled ? { cursor: 'not-allowed' } : {}}
        disabled={disabled}
        {...childProps}
      >
        {children}
      </Clickable>
    </RouteLink>
  );
};

// uses HashLink to allow navigation to anchor IDs on the same page

// example: if you have a component with id='user-info', then you can create a HashLinkButton which navigates to it:
// <HashLinkButton path='#user-info'/>

export const HashLinkButton = ({
  children,
  path,
  type = 'primary',
  style = {},
  disabled = false,
  propagateDataTestId = false,
  ...props
}) => {
  // `fp.omit` used to prevent propagation of test IDs to the rendered child component.
  const childProps = propagateDataTestId
    ? props
    : fp.omit(['data-test-id'], props);
  const computedStyle = fp.merge(
    computeStyle(buttonVariants[type], { disabled }),
    { style }
  );
  return (
    <HashLink smooth to={path} {...computedStyle}>
      <Clickable disabled={disabled} {...childProps}>
        {children}
      </Clickable>
    </HashLink>
  );
};

export const MenuItem = ({
  icon = null,
  faIcon = null,
  tooltip = '' as any,
  disabled = false,
  children,
  style = {},
  ...props
}) => {
  return (
    <TooltipTrigger side='left' content={tooltip}>
      <Clickable
        // data-test-id is the text within the MenuItem, with whitespace removed
        // and appended with '-menu-item'
        data-test-id={children.toString().replace(/\s/g, '') + '-menu-item'}
        disabled={disabled}
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'start',
          fontSize: 12,
          minWidth: 125,
          height: 32,
          color: disabled
            ? colorWithWhiteness(colors.dark, disabledAlpha)
            : 'black',
          padding: '0 12px',
          cursor: disabled ? 'not-allowed' : 'pointer',
          ...style,
        }}
        hover={
          !disabled
            ? { backgroundColor: colorWithWhiteness(colors.accent, 0.92) }
            : undefined
        }
        {...props}
      >
        {icon && (
          // TODO(RW-5682): Use a consistent icon type throughout. For now, support both.
          <ClrIcon shape={icon} style={{ marginRight: 8 }} size={15} />
        )}
        {faIcon && (
          // For consistency with ClrIcon: FontAwesome default icon size is ~11px.
          // To align these, we add 2px additional margin on either side.
          // See also https://fontawesome.com/how-to-use/on-the-web/styling/sizing-icons
          <FontAwesomeIcon
            icon={faIcon}
            style={{ marginLeft: 2, marginRight: 10 }}
          />
        )}
        {children}
      </Clickable>
    </TooltipTrigger>
  );
};

export const IconButton = ({
  icon: Icon,
  style = {},
  hover = {},
  tooltip = '',
  disabled = false,
  title = '',
  ...props
}) => {
  return (
    <TooltipTrigger side='left' content={tooltip}>
      <TerraUIInteractive
        tagName='div'
        style={{
          color: disabled ? colors.disabled : colors.accent,
          cursor: disabled ? 'auto' : 'pointer',
          ...style,
        }}
        hover={{
          color: !disabled && colorWithWhiteness(colors.accent, 0.2),
          ...hover,
        }}
        disabled={disabled}
        {...props}
      >
        <Icon {...{ title, disabled, style }} />
      </TerraUIInteractive>
    </TooltipTrigger>
  );
};

export const SnowmanButton = ({ ...props }) => (
  <IconButton icon={SnowmanIcon} {...props} />
);

export const KebabCircleButton = ({ ...props }) => (
  <IconButton
    icon={CircleEllipsisIcon}
    style={{
      width: 21,
      height: 21,
    }}
    {...props}
  />
);

const cardButtonBase = {
  style: {
    alignItems: 'flex-start',
    alignContent: 'left',
    fontWeight: 500,
    justifyContent: 'center',
    padding: '0 1.5rem',
    color: colors.accent,
  },
  disabledStyle: {
    color: colorWithWhiteness(colors.dark, disabledAlpha),
    cursor: 'not-allowed',
  },
};

const cardButtonStyle = {
  large: {
    style: {
      ...cardStyles.workspaceCard,
      ...cardButtonBase.style,
      boxShadow:
        '0 0 2px 0 rgba(0, 0, 0, 0.12), 0 3px 2px 0 rgba(0, 0, 0, 0.12)',
      fontSize: 20,
      lineHeight: '28px',
    },
    disabledStyle: { ...cardButtonBase.disabledStyle },
  },

  small: {
    style: {
      ...cardStyles.resourceCard,
      ...cardButtonBase.style,
      fontSize: 18,
      lineHeight: '22px',
      minWidth: '200px',
      maxWidth: '200px',
      minHeight: '105px',
      maxHeight: '105px',
      marginTop: '1.5rem',
      marginRight: '1.5rem',
    },
    disabledStyle: { ...cardButtonBase.disabledStyle },
  },
};

export const CardButton = ({
  type = 'large',
  disabled = false,
  style = {},
  children,
  ...props
}) => {
  return (
    <Clickable
      disabled={disabled}
      {...props}
      {...fp.merge(computeStyle(cardButtonStyle[type], { disabled }), {
        style,
      })}
    >
      {children}
    </Clickable>
  );
};

const tabButtonStyle = {
  style: {
    margin: '0 1.5rem',
    textAlign: 'center',
    color: colors.accent,
    fontSize: '16px',
    lineHeight: '28px',
  },
  hover: {},
  disabledStyle: {},
};

const activeTabButtonStyle = {
  style: {
    borderBottom: `4px solid ${colors.accent}`,
    fontWeight: 600,
  },
};

export const TabButton = ({
  disabled = false,
  style = {},
  active = false,
  children,
  ...props
}) => {
  const tabButtonStyleMerged = {
    style: {
      ...tabButtonStyle.style,
      ...(active ? activeTabButtonStyle.style : {}),
    },
    hover: tabButtonStyle.hover,
    disabledStyle: tabButtonStyle.disabledStyle,
  };
  return (
    <Clickable
      disabled={disabled}
      {...props}
      {...fp.merge(computeStyle(tabButtonStyleMerged, { disabled }), { style })}
    >
      {children}
    </Clickable>
  );
};

// The intended use of this component is as a button that is styled as a link, but does not actually navigate anywhere.
export const LinkButton = ({
  disabled = false,
  style = {},
  children,
  ...props
}) => {
  const linkStyle = {
    style: { color: colors.accent },
    hover: { textDecoration: 'underline' },
  };
  return (
    <Clickable
      disabled={disabled}
      {...props}
      {...fp.merge(computeStyle(linkStyle, { disabled }), { style })}
    >
      {children}
    </Clickable>
  );
};

export const StyledRouterLink = ({
  path,
  children,
  disabled = false,
  propagateDataTestId = false,
  analyticsFn = null,
  style = {},
  ...props
}) => {
  const childProps = propagateDataTestId
    ? props
    : fp.omit(['data-test-id'], props);
  const linkStyle = {
    style: { ...styles.inlineAnchor },
  };
  const computedStyles = fp.merge(computeStyle(linkStyle, { disabled }), {
    style,
  });
  // A react-router Link will attempt to navigate whenever you click on it; it has no concept
  // of 'disabled'. So if it is disabled, we render a span instead.
  return disabled ? (
    <span {...computedStyles} {...childProps}>
      {children}
    </span>
  ) : (
    <Link
      to={path}
      onClick={() => analyticsFn?.()}
      {...computedStyles}
      {...childProps}
    >
      {children}
    </Link>
  );
};

export const StyledExternalLink = ({
  href,
  children,
  analyticsFn = null,
  style = {},
  disabled = false,
  ...props
}) => {
  const linkStyle = {
    style: { ...styles.inlineAnchor },
  };
  const computedStyles = fp.merge(computeStyle(linkStyle, { disabled }), {
    style,
  });

  return disabled ? (
    <span {...computedStyles}>{children}</span>
  ) : (
    <a
      href={href}
      onClick={() => analyticsFn?.()}
      {...computedStyles}
      {...props}
    >
      {children}
    </a>
  );
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

export class SlidingFabReact extends React.Component<
  SlidingFabProps,
  SlidingFabState
> {
  constructor(props) {
    super(props);
    this.state = { hovering: false };
  }

  render() {
    const { hovering } = this.state;
    const { expanded, disable, iconShape, tooltip, tooltipContent } =
      this.props;
    return (
      <div style={styles.slidingButtonContainer}>
        <div
          data-test-id='sliding-button'
          style={
            disable
              ? { ...styles.slidingButton, ...styles.slidingButtonDisable }
              : styles.slidingButton
          }
          onMouseEnter={() => this.setState({ hovering: true })}
          onMouseLeave={() => this.setState({ hovering: false })}
          onClick={() => (disable ? {} : this.props.submitFunction())}
        >
          <TooltipTrigger content={tooltipContent} disabled={!tooltip}>
            <div style={styles.slidingButtonContent}>
              <div
                style={
                  hovering
                    ? {
                        ...styles.slidingButtonText,
                        ...styles.slidingButtonHovering,
                      }
                    : styles.slidingButtonText
                }
              >
                {expanded}
              </div>
              <ClrIcon
                shape={iconShape}
                style={{
                  height: '2.25rem',
                  width: '2.25rem',
                  marginRight: '0.2175rem',
                }}
              />
            </div>
          </TooltipTrigger>
        </div>
      </div>
    );
  }
}

interface CloseButtonProps {
  onClose: Function;
  style?: CSSProperties;
}
export const CloseButton = (props: CloseButtonProps) => {
  const { onClose, style } = props;
  return (
    <Clickable {...{ style }} onClick={onClose}>
      <img src={times} style={{ height: '27px', width: '17px' }} alt='Close' />
    </Clickable>
  );
};
