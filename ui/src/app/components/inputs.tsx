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

export const CheckBox = ({onChange, ...props}) => {
  return <input
    type='checkbox'
    onChange={onChange ? (e => onChange(e.target.checked)) : undefined}
    {...props}
  />;
};

interface LabeledCheckboxProps {
  initialValue: boolean;
  disabled: boolean;
  onChange: Function;
  style: object;
  checkboxStyle: object;
  labelStyle: object;
  label: string;
}

interface LabeledCheckboxState {
  value: boolean;
}

export class LabeledCheckBox extends React.Component<LabeledCheckboxProps, LabeledCheckboxState> {
  constructor(props: any) {
    super(props);
    this.state = {
      value: props.initialValue
    }
  }

  toggleValue() {
    if(!this.props.disabled) {
      this.setState(previousState => ({value: !previousState.value}))
    }
  }

  render() {
    return <div style={this.props.style}>
      <CheckBox
          style={{...this.props.checkboxStyle, verticalAlign: 'middle'}}
          checked={this.state.value}
          disabled={this.props.disabled}
          onChange={e => this.props.onChange(e)}
      />
      <label
          style={this.props.labelStyle}
          onClick={() => this.toggleValue()}
      >
        {this.props.label}
      </label>
    </div>;
  }
};

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
