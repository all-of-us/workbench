import * as fp from 'lodash/fp';
import * as moment from 'moment';
import * as React from 'react';
import Calendar from 'react-calendar/dist/entry.nostyle';
import RSelect from 'react-select';
import Switch from 'react-switch';

import {Clickable} from 'app/components/buttons';
import {ClrIcon} from 'app/components/icons';
import {PopupTrigger} from 'app/components/popups';
import {commonStyles} from 'app/pages/login/account-creation/common';
import colors, {colorWithWhiteness} from 'app/styles/colors';
import {withStyle} from 'app/utils/index';
import {FlexRow} from './flex';

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
    minWidth: '30px',
    minHeight: '28px',
    // Without explicit vertical align, this div is top-aligned when empty, which can cause some
    // excess space above the input element and layout jitter when an icon becomes shown.
    verticalAlign: 'middle',
  },

  inputStyle: {
    backgroundColor: colors.white,
    borderColor: inputBorderColor,
    borderRadius: 3,
    borderStyle: 'solid',
    borderWidth: 1,
    height: '1.5rem',
    padding: '0 0.5rem',
    width: '100%'
  },

  textBoxWithLengthValidationTextBoxStyle: {
    height: '15rem',
    resize: 'none',
    width: '48rem',
    borderRadius: '3px 3px 0 0',
    borderColor: colorWithWhiteness(colors.dark, 0.5)
  },

  textBoxWithLengthValidationValidationStyle: {
    justifyContent: 'space-between',
    width: '48rem',
    backgroundColor: colorWithWhiteness(colors.primary, 0.95),
    fontSize: 12,
    colors: colors.primary,
    padding: '0.25rem',
    borderRadius: '0 0 3px 3px', marginTop: '-0.5rem',
    border: `1px solid ${colorWithWhiteness(colors.dark, 0.5)}`
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

interface TextAreaWithLengthValidationMessageProps {
  heightOverride?: {};
  id: string;
  initialText: string;
  maxCharacters: number;
  tooLongWarningCharacters: number;
  onChange: (s: string) => void;
  tooShortWarningCharacters?: number;
  tooShortWarning?: string;
  textBoxStyleOverrides?: {};
}

interface TextAreaWithLengthValidationMessageState {
  showTooShortWarning: boolean;
  text: string;
}

export class TextAreaWithLengthValidationMessage extends React.Component<
  TextAreaWithLengthValidationMessageProps,
  TextAreaWithLengthValidationMessageState
> {
  constructor(props: TextAreaWithLengthValidationMessageProps) {
    super(props);
    this.state = {
      showTooShortWarning: false,
      text: props.initialText
    };
  }

  onTextUpdate(text) {
    if (this.state.showTooShortWarning && text.length >= 50) {
      this.setState({showTooShortWarning: false});
    }
    this.setState({text: text});
    this.props.onChange(text);
  }

  updateShowTooShortWarning() {
    if (
        this.state.text
        && this.props.tooShortWarningCharacters
        && this.props.tooShortWarning
        && this.state.text.length < this.props.tooShortWarningCharacters
    ) {
      this.setState({showTooShortWarning: true});
    } else {
      this.setState({showTooShortWarning: false});
    }
  }

  renderCharacterLimitMessage(textColor: string, message: string) {
    return <div
        data-test-id='characterLimit'
        style={{color: textColor, marginLeft: 'auto', flex: '0 0 auto'}}
    >
      {message}
    </div>;
  }

  render() {
    const {id, maxCharacters, tooShortWarning} = this.props;
    const {showTooShortWarning, text} = this.state;

    return <React.Fragment>
      <TextArea
          style={{...styles.textBoxWithLengthValidationTextBoxStyle, ...this.props.textBoxStyleOverrides, ...this.props.heightOverride}}
          id={id}
          value={text}
          onBlur={() => this.updateShowTooShortWarning()}
          onChange={v => this.onTextUpdate(v)}
      />
      <FlexRow
          style={{...styles.textBoxWithLengthValidationValidationStyle, ...this.props.textBoxStyleOverrides}}
      >
        {showTooShortWarning &&
          <label
              data-test-id='warning'
              style={{
                color: colors.danger,
                justifyContent: 'flex-start',
                marginRight: '.25rem'
              }}
          >
            {tooShortWarning}
          </label>
        }
        {!text &&
          this.renderCharacterLimitMessage(colors.primary, maxCharacters + ' characters remaining')
        }
        {text && text.length < maxCharacters &&
          this.renderCharacterLimitMessage(colors.primary, (maxCharacters - text.length) + ' characters remaining')
        }
        {text && text.length === maxCharacters &&
          this.renderCharacterLimitMessage(colors.danger, '0 characters remaining')
        }
        {text && text.length > maxCharacters &&
          this.renderCharacterLimitMessage(colors.danger, (text.length - maxCharacters) + ' characters over')
        }
      </FlexRow>
    </React.Fragment>;
  }
}

export const FormValidationErrorMessage = withStyle({
  color: colors.danger,
  fontSize: 12
})('div');

export const TextInput = React.forwardRef(({style = {}, onChange, onBlur, ...props}:
      {style?: React.CSSProperties, onChange: Function, onBlur: Function, [key: string]: any},
                                           ref: React.Ref<HTMLInputElement>) => {
  return <input
    {...props}
    ref = {ref}
    onChange={onChange ? (e => onChange(e.target.value)) : undefined}
    onBlur={onBlur ? (e => onBlur(e.target.value)) : undefined}
    type='text'
    style={{...styles.inputStyle, ...style}}
  />;
});

export const NumberInput = React.forwardRef((
    {style = {}, value, onChange, onBlur, ...props}:
    {style?: React.CSSProperties, value: string, onChange: Function, onBlur?: Function, [key: string]: any},
    ref: React.Ref<HTMLInputElement>
  ) => {
  return <input
    {...props}
    ref={ref}
    type='number'
    style={{...styles.inputStyle, ...style}}
    value={fp.cond([
      [fp.isUndefined, () => undefined],
      [fp.isNull, () => ''],
      [fp.stubTrue, v => v.toString()]
    ])(value)}
    onChange={onChange ? (v => onChange(v.target.value === '' ? null : +v.target.value)) : undefined}
    onBlur={onBlur ? (v => onBlur(v.target.value)) : undefined}
  />;
});

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

interface ToggleProps {
  name: string;
  checked: boolean;
  disabled?: boolean;
  onToggle: (checked) => void;
  style?: React.CSSProperties;
  height?: number;
  width?: number;
}

export class Toggle extends React.Component<ToggleProps>  {
  constructor(props) {
    super(props);
  }

  render() {
    const {name, checked, disabled, onToggle, style, height, width} = this.props;
    return <label style={{display: 'flex', flexDirection: 'row', alignItems: 'center', paddingBottom: '.5rem', ...style}}>
      <Switch
          onChange={onToggle}
          checked={checked}
          checkedIcon={false}
          disabled={disabled}
          height={height}
          width={width}
      />
      <span style={{marginLeft: '.5rem'}}>{name}</span>
    </label>;
  }
}

/**
 * Creates a text input component with a label shown above it.
 * @param props
 * @constructor
 */
export function TextInputWithLabel(props) {
  return <div style={{...props.containerStyle}}>
    {props.labelContent}
    {props.labelText && <label style={{
      fontSize: 14,
      color: colors.primary,
      lineHeight: '22px',
      fontWeight: 600,
      ...props.labelStyle
    }}>{props.labelText}</label>}
    <div style={{marginTop: '0.1rem'}}>
      <TextInput data-test-id={props.inputId}
                 id={props.inputId}
                 name={props.inputName}
                 placeholder={props.placeholder}
                 value={props.value}
                 disabled={props.disabled}
                 onChange={props.onChange}
                 onBlur={props.onBlur}
                 invalid={props.invalid ? props.invalid.toString() : undefined}
                 style={{...commonStyles.sectionInput, ...props.inputStyle}}/>
      {props.children}
    </div>
  </div>;
}
