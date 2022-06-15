import * as React from 'react';
import { CSSProperties } from 'react';

import { FlexColumn, FlexRow } from 'app/components/flex';
import { CheckBox, RadioButton } from 'app/components/inputs';
import { TooltipTrigger } from 'app/components/popups';
import colors from 'app/styles/colors';
import { reactStyles, useId } from 'app/utils';

import { ClrIcon } from './icons';

const styles = reactStyles({
  question: { fontWeight: 'bold', color: colors.primary, fontSize: '14px' },
  answer: { margin: '0.0rem 0.25rem', color: colors.primary },
});

const Option = (props: {
  checked: boolean;
  value: any;
  label: string;
  style?: CSSProperties;
  onChange: (any) => void;
  multiple?: boolean;
  disabled?: boolean;
  disabledText?: string;
}) => {
  const { checked, disabled, disabledText, label, multiple, onChange, value } =
    props;
  const id = useId();
  return (
    <TooltipTrigger
      content={disabled && disabledText && <div>{disabledText}</div>}
    >
      <FlexRow style={{ alignItems: 'center' }}>
        {multiple ? (
          <CheckBox
            data-test-id='nothing-to-report'
            manageOwnState={false}
            id={id}
            disabled={disabled}
            checked={checked}
            onChange={onChange}
          />
        ) : (
          <RadioButton
            data-test-id='nothing-to-report'
            id={id}
            disabled={disabled}
            checked={checked}
            onChange={onChange}
            value={value}
          />
        )}
        <label htmlFor={id} style={styles.answer}>
          {label}
        </label>
      </FlexRow>
    </TooltipTrigger>
  );
};

interface MultipleChoiceOption {
  label: string;
  value: any;
  otherText?: string;
  showInput?: boolean;
  onChange?: (any) => void;
  onChangeOtherText?: (any) => void;
  onClick?: (any) => void;
  onExpand?: () => void;
  disabled?: boolean;
  disabledText?: string;
  subOptions?: MultipleChoiceOption[];
  showSubOptions?: boolean;
  otherTextMaxLength?: number;
}

export const MultipleChoiceQuestion = (props: {
  question: string;
  options: MultipleChoiceOption[];
  selected: string | string[];
  onChange: (any) => void;
  style?: CSSProperties;
  multiple?: boolean;
  horizontalOptions?: boolean;
}) => {
  const {
    horizontalOptions,
    options,
    onChange,
    question,
    selected,
    multiple,
    style,
  } = props;

  // TODO: Change variable names and/or split into two functions
  const handleQuestionChange = (
    e,
    label,
    parentValue?: any,
    childValues?: any[]
  ) => {
    if (multiple) {
      console.log('What is e? ', e);
      let result = e
        ? [...(selected as string[]), label]
        : (selected as string[]).filter((r) => r !== label);

      // Add parent if e true
      if (e && parentValue && result.indexOf(parentValue) === -1) {
        result.push(parentValue);
      }

      if (!e && childValues) {
        result = result.filter((item) => !childValues.includes(item));
      }
      onChange(result);
    } else {
      onChange(e.target.value);
    }
  };

  const renderOption = (option: MultipleChoiceOption, parentValue?: any) => {
    return (
      <FlexColumn style={option.subOptions && { width: '100%' }}>
        <FlexRow style={{ alignItems: 'center' }}>
          <Option
            disabled={option.disabled}
            disabledText={option.disabledText}
            value={option.value}
            label={option.label}
            checked={
              multiple
                ? selected.includes(option.value)
                : selected === option.value
            }
            onChange={(e) => {
              handleQuestionChange(
                e,
                option.value,
                parentValue,
                option.subOptions?.map((subOption) => subOption.value)
              );
              option.onChange?.(e);
            }}
            multiple={multiple}
          />
          {option.subOptions && (
            <ClrIcon
              shape='angle'
              style={{
                marginRight: '0.5rem',
                cursor: 'pointer',
                transform: option.showSubOptions
                  ? 'rotate(180deg)'
                  : 'rotate(0deg)',
              }}
              onClick={option.onExpand}
            />
          )}
        </FlexRow>

        {option.showInput &&
          ((multiple && selected.includes(option.value)) ||
            selected === option.value) && (
            <input
              data-test-id='search'
              style={{
                marginBottom: '.5em',
                marginLeft: '0.75rem',
                width: '300px',
              }}
              type='text'
              value={option.otherText}
              maxLength={option.otherTextMaxLength}
              onChange={(e) => option.onChangeOtherText?.(e.target.value)}
            />
          )}
        {option.showSubOptions &&
          option.subOptions &&
          (horizontalOptions ? (
            <FlexRow
              style={{
                flex: 1,
                flexWrap: 'wrap',
                gap: '0.5rem',
                marginLeft: '2.0rem',
              }}
            >
              {option.subOptions.map((subOption) =>
                renderOption(subOption, option.value)
              )}
            </FlexRow>
          ) : (
            <FlexColumn
              style={{
                flex: 1,
                gap: '0.5rem',
                marginLeft: '2.0rem',
              }}
            >
              {option.subOptions.map((subOption) =>
                renderOption(subOption, option.value)
              )}
            </FlexColumn>
          ))}
      </FlexColumn>
    );
  };

  const optionComponents = options.map((option) => renderOption(option, null));

  return (
    <div style={{ ...style }}>
      <div style={{ ...styles.question }}>{question}</div>
      {horizontalOptions ? (
        <FlexRow style={{ flexWrap: 'wrap', gap: '0.5rem' }}>
          {optionComponents}
        </FlexRow>
      ) : (
        <FlexColumn style={{ flexWrap: 'wrap', gap: '0.5rem' }}>
          {optionComponents}
        </FlexColumn>
      )}
    </div>
  );
};
