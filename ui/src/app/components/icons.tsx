import * as React from 'react';
import * as fp from 'lodash/fp';
import { faCircle, faCopy } from '@fortawesome/free-regular-svg-icons';
import {
  faArrowUpRightFromSquare,
  faBan,
  faCaretRight,
  faCheck,
  faCheckCircle,
  faClock,
  faExclamationTriangle,
  faLongArrowAltLeft,
  faLongArrowAltRight,
  faMinusCircle,
  faRepeat,
  faTrash,
} from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import colors, { addOpacity } from 'app/styles/colors';
import { ReactComponent as circleEllipsis } from 'assets/icons/circle-ellipsis.svg';
import { ReactComponent as controlledTierBadge } from 'assets/icons/controlled-tier-badge.svg';
import CW_icon from 'assets/icons/CW_icon.png';
import googleCloudLogo from 'assets/icons/google-cloud.svg';
import { ReactComponent as locationArrow } from 'assets/icons/location-arrow.svg';
import { ReactComponent as registeredTierBadge } from 'assets/icons/registered-tier-badge.svg';

export const CircleEllipsisIcon = circleEllipsis;
export const ControlledTierBadge = controlledTierBadge;
export const LocationArrowIcon = locationArrow;
export const RegisteredTierBadge = registeredTierBadge;

export const styles = {
  infoIcon: {
    color: colors.accent,
    height: '22px',
    width: '22px',
    fill: colors.accent,
  },

  successIcon: {
    color: colors.success,
    width: '20px',
    height: '20px',
  },

  dangerIcon: {
    color: colors.danger,
    width: '20px',
    height: '20px',
  },

  snowmanIcon: {
    marginLeft: -9,
    width: '21px',
    height: '21px',
  },

  circleBackground: {
    background: `${addOpacity(colors.accent, 0.15)}`,
    borderRadius: '50%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },

  defaultCircle: {
    height: '2.25rem',
    width: '2.25rem',
  },
};

export const ClrIcon = ({ className = '', ...props }) => {
  // `fp.omit` used to prevent propagation of test IDs to the rendered child component.
  return React.createElement('clr-icon', {
    class: className,
    ...fp.omit(['data-test-id'], props),
  });
};

export const SnowmanIcon = ({ style = {}, ...props }) => (
  <ClrIcon
    shape='ellipsis-vertical'
    {...props}
    style={{ ...styles.snowmanIcon, ...style }}
  />
);

export const InfoIcon = ({ style = {}, ...props }) => (
  <ClrIcon
    shape='info-standard'
    {...props}
    class='is-solid'
    style={{ ...styles.infoIcon, ...style }}
  />
);

export const WarningIcon = ({ style = {}, ...props }) => (
  <ClrIcon
    shape='warning-standard'
    {...props}
    class='is-solid'
    style={{ ...styles.dangerIcon, ...style }}
  />
);

export const ValidationIcon = (props) => {
  if (props.validSuccess === undefined) {
    return null;
  } else if (props.validSuccess) {
    return (
      <ClrIcon
        shape='success-standard'
        class='is-solid'
        style={{ ...styles.successIcon, ...props.style }}
      />
    );
  } else {
    return (
      <ClrIcon
        shape='warning-standard'
        class='is-solid'
        style={{ ...styles.dangerIcon, ...props.style }}
      />
    );
  }
};

const Icon = ({ shape, size, style, color, ...props }) => {
  return (
    <FontAwesomeIcon
      icon={shape}
      style={{ height: size, width: size, color, ...style }}
      {...props}
    />
  );
};
export const ArrowLeft = (props) => (
  <Icon shape={faLongArrowAltLeft} {...props} />
);
export const ArrowRight = (props) => (
  <Icon shape={faLongArrowAltRight} {...props} />
);
export const Ban = (props) => <Icon shape={faBan} {...props} />;
export const CaretRight = (props) => <Icon shape={faCaretRight} {...props} />;
export const Check = (props) => <Icon shape={faCheck} {...props} />;
export const CheckCircle = (props) => <Icon shape={faCheckCircle} {...props} />;
export const Circle = (props) => <Icon shape={faCircle} {...props} />;
export const Clock = (props) => <Icon shape={faClock} {...props} />;
export const Copy = (props) => <Icon shape={faCopy} {...props} />;
export const ExclamationTriangle = (props) => (
  <Icon shape={faExclamationTriangle} color={colors.danger} {...props} />
);
export const MinusCircle = (props) => <Icon shape={faMinusCircle} {...props} />;
export const Repeat = (props) => <Icon shape={faRepeat} {...props} />;
export const TrashCan = (props) => <Icon shape={faTrash} {...props} />;

const svgIcon =
  (src) =>
  ({ size = 25, ...props }) =>
    <img style={{ height: size, width: size }} src={src} {...props} />;

export const GoogleCloudLogoSvg = svgIcon(googleCloudLogo);

export const CommunityIcon = ({ size = 25, ...props }) => (
  <img
    aria-label={'Community Workspace'}
    src={CW_icon}
    style={{ width: size, height: size }}
    {...props}
  />
);

export const NewWindowIcon = (props) => (
  <Icon shape={faArrowUpRightFromSquare} {...props} />
);
