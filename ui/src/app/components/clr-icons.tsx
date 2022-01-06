import * as React from 'react';
import * as fp from 'lodash/fp';

import {reactStyles} from 'app/utils';
import colors from 'app/styles/colors';

const styles = reactStyles({
  snowmanIcon: {
    marginLeft: -9,
    width: '21px',
    height: '21px'
  },
  infoIcon: {
    color: colors.accent,
    height: '22px',
    width: '22px',
    fill: colors.accent
  },
  successIcon: {
    color: colors.success,
    width: '20px',
    height: '20px'
  },
  dangerIcon: {
    color: colors.danger,
    width: '20px',
    height: '20px'
  },
})

// collect all of the ClrIcon references in one file, to help with migration to FontAwesome (RW-7683)

/**
 * @deprecated if you're implementing a new icon, please use Font Awesome instead (the "fa" icons in icons.tsx)
 */
export const ClrIcon = ({className = '', ...props}) => {
  // `fp.omit` used to prevent propagation of test IDs to the rendered child component.
  return React.createElement('clr-icon', {class: className, ...fp.omit(['data-test-id'], props)});
};

export const SnowmanIcon = ({style = {}, ...props}) => {
  return <ClrIcon shape='ellipsis-vertical' {...props} style={{...styles.snowmanIcon, ...style}}/>;
};

export const InfoIcon = ({style = {}, ...props}) =>
  <ClrIcon shape='info-standard' {...props} class='is-solid'
           style={{...styles.infoIcon, ...style}}/>;

export const ValidationIcon = props => {
  if (props.validSuccess === undefined) {
    return null;
  } else if (props.validSuccess) {
    return <SuccessStandardIcon class='is-solid' style={{...styles.successIcon, ...props.style}}/>;
  } else {
    return <WarningStandardIcon class='is-solid' style={{...styles.dangerIcon, ...props.style}}/>;
  }
};

export const AngleIcon = props => <ClrIcon shape='angle' {...props}/>
export const ArrowIcon = props => <ClrIcon shape='arrow' {...props}/>
export const BarChartIcon = props => <ClrIcon shape='bar-chart' {...props}/>
export const BarsIcon = props => <ClrIcon shape='bars' {...props}/>
export const CalendarIcon = props => <ClrIcon shape='calendar' {...props}/>
export const CaretDownIcon = props => <ClrIcon shape='caret down' {...props}/>
export const CaretRightIcon = props => <ClrIcon shape='caret right' {...props}/>
export const CheckCircleIcon = props => <ClrIcon shape='check-circle' {...props}/>
export const CheckIcon = props => <ClrIcon shape='check' {...props}/>
export const CopyIcon = props => <ClrIcon shape='copy' {...props}/>
export const EllipsisVerticalIcon = props => <ClrIcon shape='ellipsis-vertical' {...props}/>
export const ExclamationCircleIcon = props => <ClrIcon shape='exclamation-circle' {...props}/>
export const ExclamationTriangleIcon = props => <ClrIcon shape='exclamation-triangle' {...props}/>
export const ExportIcon = props => <ClrIcon shape='export' {...props}/>
export const EyeHideIcon = props => <ClrIcon shape='eye-hide' {...props}/>
export const FlagIcon = props => <ClrIcon shape='flag' {...props}/>
export const InfoStandardIcon = props => <ClrIcon shape='info-standard' {...props}/>
export const LockIcon = props => <ClrIcon shape='lock' {...props}/>
export const MinusCircleIcon = props => <ClrIcon shape='minus-circle' {...props}/>
export const PlusCircleIcon = props => <ClrIcon shape='plus-circle' {...props}/>
export const SearchIcon = props => <ClrIcon shape='search' {...props}/>
export const SliderIcon = props => <ClrIcon shape='slider' {...props}/>
export const SortByIcon = props => <ClrIcon shape='sort-by' {...props}/>
export const SuccessStandardIcon = props => <ClrIcon shape='success-standard' {...props}/>
export const SyncIcon = props => <ClrIcon shape='sync' {...props}/>
export const TimesCircleIcon = props => <ClrIcon shape='times-circle' {...props}/>
export const TimesIcon = props => <ClrIcon shape='times' {...props}/>
export const TrashIcon = props => <ClrIcon shape='trash' {...props}/>
export const WarningStandardIcon = props => <ClrIcon shape='warning-standard' {...props}/>
