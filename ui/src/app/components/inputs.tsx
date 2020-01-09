import * as fp from 'lodash/fp';
import * as moment from 'moment';
import * as React from 'react';
import Calendar from 'react-calendar/dist/entry.nostyle';
import RSelect from 'react-select';
import Switch from 'react-switch';

import {Clickable} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {PopupTrigger} from 'app/components/popups';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {withStyle} from 'app/utils/index';

export const inputBorderColor = colorWithWhiteness(colors.dark, 0.6);

export const styles = {
  successfulInput: {
    borderColor: colors.success
  },

  checkbox: {
    cursor: 'pointer',
    verticalAlign: 'middle'
  },

  checkboxLabel: {
    cursor: 'pointer',
    verticalAlign: 'middle'
  },

  disabledStyle: {
    cursor: 'default'
  },

  error: {
    padding: '0 0.5rem',
    fontWeight: 600,
    color: colors.primary,
    marginTop: '0.2rem',
    width: '90%'
  },

  errorMessage: {
    width: '12.5rem',
    padding: '.25rem',
    float: 'right' as 'right',
    marginTop: 0,
    background: colorWithWhiteness(colors.danger, 0.9),
    color: colors.primary,
    border: `1px solid ${colors.danger}`,
    borderRadius: '2px',
    display: 'flex' as 'flex',
    flexDirection: 'row' as 'row',
    fontSize: '13px'
  },

  iconArea: {
    display: 'inline-block',
    marginLeft: '-30px',
    minWidth: '30px'
  }
};


export const Error = withStyle(styles.error)('div');
export const ErrorMessage = withStyle(styles.errorMessage)('div');

export const ValidationError = ({children}) => {
  if (!children) {
    return null;
  }
  return <div
    style={{
      color: colors.danger,
      fontSize: 10, fontWeight: 500, textTransform: 'uppercase',
      marginLeft: '0.5rem', marginTop: '0.25rem'
    }}
  >{children}</div>;
};

export const TextInput = React.forwardRef(({style = {}, onChange, ...props}:
      {style?: React.CSSProperties, onChange: Function, [key: string]: any},
                                           ref: React.Ref<HTMLInputElement>) => {
  return <input
    {...props}
    ref = {ref}
    onChange={onChange ? (e => onChange(e.target.value)) : undefined}
    type='text'
    style={{
      width: '100%', height: '1.5rem',
      borderColor: inputBorderColor, borderWidth: 1,
      borderStyle: 'solid', borderRadius: 3,
      padding: '0 0.5rem',
      backgroundColor: colors.white,
      ...style
    }}
  />;
});

export const NumberInput = ({style = {}, value, onChange, ...props}) => {
  return <TextInput
    {...props}
    type='number'
    value={fp.cond([
      [fp.isUndefined, () => undefined],
      [fp.isNull, () => ''],
      [fp.stubTrue, v => v.toString()]
    ])(value)}
    onChange={onChange ? (v => onChange(v === '' ? null : +v)) : undefined}
  />;
};

export const TextArea = ({style = {}, onChange, ...props}) => {
  return <textarea
    {...props}
    onChange={onChange ? (e => onChange(e.target.value)) : undefined}
    style={{
      width: '100%',
      borderColor: inputBorderColor, borderWidth: 1, borderStyle: 'solid', borderRadius: 3,
      padding: '0.25rem 0.5rem',
      backgroundColor: colors.white,
      ...style
    }}
  />;
};

export const RadioButton = ({ onChange, ...props }) => {
  return <input
    type='radio'
    {...props}
    onChange={onChange}
    onClick={onChange}
  />;
};

interface CheckBoxProps {
  // Whether the checkbox should be checked. If manageOwnState is false, the
  // checkbox will always be rendered with the value of this prop. If
  // manageOwnState is true, this prop controls only the initial value.
  checked?: boolean;
  // Whether the checkbox is rendered in a disabled state.
  disabled?: boolean;
  id?: string;
  // An optional label to show alongside the input. Can be a plain string or
  // any React node.
  label?: React.ReactNode;
  // Styles to apply to the label element. Only relevant when label is non-null.
  labelStyle?: React.CSSProperties;
  // Indicates whether the CheckBox should be responsible for managing its own
  // state. When false, the HTML input will always be rendered with the value of
  // the passed 'checked' prop.
  manageOwnState: boolean;
  // Callback called when the user clicks the checkbox or label, containing the
  // new checked value.
  onChange?: (boolean) => void;
  // Styles for the <input> checkbox component.
  style?: React.CSSProperties;
  // If the label is non-empty, styles to be applied to the <span> wrapper.
  wrapperStyle?: React.CSSProperties;
}

interface CheckBoxState {
  checked: boolean;
}

export class CheckBox extends React.Component<CheckBoxProps, CheckBoxState> {
  static defaultProps: CheckBoxProps = {
    checked: false,
    manageOwnState: true
  };

  uniqueId?: string = null;

  constructor(props: CheckBoxProps) {
    super(props);
    this.state = {
      checked: props.checked
    };

    this.uniqueId = props.id || fp.uniqueId('checkbox');
  }

  handleChange(e: React.ChangeEvent<HTMLInputElement>) {
    if (this.props.manageOwnState) {
      // We only track state internally if props aren't being used to render
      // the checkbox value.
      this.setState({checked: !this.state.checked});
    }
    if (this.props.onChange) {
      this.props.onChange(e.target.checked);
    }
  }
  render() {
    const {
      checked, disabled, label, labelStyle, onChange, manageOwnState, style, wrapperStyle,
      ...otherProps
    } = this.props;
    const maybeDisabledOverrides = disabled ? styles.disabledStyle : {};

    const input = <input
      id={this.uniqueId}
      type='checkbox'
      checked={manageOwnState ? this.state.checked : checked}
      disabled={disabled}
      onChange={e => this.handleChange(e)}
      style={{...styles.checkbox, ...style, ...maybeDisabledOverrides}}
      {...otherProps}
    />;
    if (label) {
      return <span style={wrapperStyle}>{input}
        <label htmlFor={this.uniqueId}
               style={{...styles.checkboxLabel,
                 ...labelStyle,
                 ...maybeDisabledOverrides}}>{label}</label>
      </span>;
    } else {
      return input;
    }
  }
}

export const Select = ({value, options, onChange, ...props}) => {
  return <RSelect
    value={options.find(o => o.value === value)}
    options={options}
    onChange={o => onChange(o && o.value)}
    {...props}
  />;
};

export class DatePicker extends React.Component<
  {value: Date, onChange: Function, onBlur?: Function, maxDate?: Date, disabled?: boolean,
    placeholder?: string}
> {
  popup: React.RefObject<any>;
  constructor(props) {
    super(props);
    this.popup = React.createRef();
  }

  render() {
    const {value, onChange, onBlur, disabled, placeholder, ...props} = this.props;
    let date, text;
    if (value !== null && typeof value === 'object') {
      date = value;
      text = value.toISOString().slice(0, 10);
    } else {
      date = moment(value, 'YYYY-MM-DD', true).isValid()
        ? new Date(new Date(value).toUTCString().substr(0, 25)) : null;
      text = value || '';
    }
    return <div
      style={{
        display: 'flex',
        width: '100%', height: '1.5rem',
        border: 0,
        padding: '0',
        color: colors.dark, background: 'transparent',
        ...(disabled ? {opacity: .5} : {}),
      }}
    >
      <TextInput
        value={text}
        onChange={onChange}
        onBlur={onBlur}
        disabled={disabled}
        placeholder={placeholder}
        style={{...(disabled ? {cursor: 'not-allowed'} : {})}}/>
      <PopupTrigger
        ref={this.popup}
        content={<Calendar
          {...props}
          value={date}
          onChange={v => {
            this.popup.current.close();
            onChange(v);
          }}
        />}
      >
        <Clickable style={{display: 'flex', alignItems: 'center', flex: 1,
          ...(disabled ? {cursor: 'not-allowed'} : {})}} disabled={disabled}>
          <ClrIcon
            style={{flex: 'none', marginLeft: '4px', color: colors.accent}}
            shape='calendar'
            size={20} />
        </Clickable>
      </PopupTrigger>
    </div>;
  }
}

export const Toggle = ({name, enabled, onToggle, ...props}) => {
  return <label style={{display: 'flex', flexDirection: 'row', paddingBottom: '.5rem'}}>
    <Switch onChange={onToggle} checked={enabled} checkedIcon={false}
            {...props}
    />
    <span style={{marginLeft: '.5rem'}}>{name}</span>
  </label>;
};
