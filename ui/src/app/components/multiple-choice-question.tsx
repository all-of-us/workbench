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

interface Option {
  checked: boolean;
  value: any;
  label: string;
  style?: CSSProperties;
  onChange: (any) => void;
  multiple?: boolean;
  disabled?: boolean;
  disabledText?: string;
}

const Option = (props: Option) => {
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
            {...{ id, disabled, checked, onChange }}
            data-test-id='nothing-to-report'
            manageOwnState={false}
          />
        ) : (
          <RadioButton
            {...{ id, disabled, checked, onChange, value }}
            data-test-id='nothing-to-report'
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

interface MultipleChoiceQuestionProps {
  question: any;
  options: MultipleChoiceOption[];
  selected: any | any[];
  onChange: (any) => void;
  style?: CSSProperties;
  multiple?: boolean;
  horizontalOptions?: boolean;
}

export const MultipleChoiceQuestion = (props: MultipleChoiceQuestionProps) => {
  const {
    horizontalOptions,
    options,
    onChange,
    question,
    selected,
    multiple,
    style,
  } = props;

  const handleQuestionChange = (
    e,
    label,
    parentValue?: any,
    childValues?: any[]
  ) => {
    // If the question is a multi-select, e represents whether the
    // selected option is selected (true|false).
    // If the question is a single select, e represents an event where e.target.value represents
    // the value of the option selected.
    if (multiple) {
      let updatedSelectedOptions = e
        ? [...(selected as string[]), label]
        : (selected as string[]).filter((r) => r !== label);

      // If adding a child and parent is not currently checked, add the parent too
      if (
        e &&
        parentValue &&
        updatedSelectedOptions.indexOf(parentValue) === -1
      ) {
        updatedSelectedOptions.push(parentValue);
      }

      // If unchecking a parent also uncheck all of its children
      if (!e && childValues) {
        updatedSelectedOptions = updatedSelectedOptions.filter(
          (item) => !childValues.includes(item)
        );
      }
      onChange(updatedSelectedOptions);
    } else {
      onChange(e.target.value);
    }
  };

  const renderOption = (option: MultipleChoiceOption, parentValue?: any) => {
    const {
      disabled,
      disabledText,
      label,
      onChange: onChangeOption,
      onChangeOtherText,
      onExpand,
      otherText,
      otherTextMaxLength,
      showInput,
      showSubOptions,
      subOptions,
      value,
    } = option;
    return (
      <FlexColumn style={subOptions && { width: '100%' }}>
        <FlexRow style={{ alignItems: 'center' }}>
          <Option
            {...{ disabled, disabledText, label, multiple, value }}
            checked={multiple ? selected.includes(value) : selected === value}
            onChange={(e) => {
              handleQuestionChange(
                e,
                value,
                parentValue,
                subOptions?.map((so) => so.value)
              );
              onChangeOption?.(e);
            }}
          />
          {subOptions && (
            <ClrIcon
              shape='angle'
              style={{
                marginRight: '0.5rem',
                cursor: 'pointer',
                transform: showSubOptions ? 'rotate(180deg)' : 'rotate(0deg)',
              }}
              onClick={onExpand}
            />
          )}
        </FlexRow>

        {showInput &&
          ((multiple && selected.includes(value)) || selected === value) && (
            <input
              data-test-id='search'
              style={{
                marginBottom: '.5em',
                marginLeft: '0.75rem',
                width: '300px',
              }}
              type='text'
              value={otherText || ''}
              maxLength={otherTextMaxLength}
              onChange={(e) => onChangeOtherText?.(e.target.value)}
            />
          )}
        {showSubOptions &&
          subOptions &&
          (horizontalOptions ? (
            <FlexRow
              style={{
                flex: 1,
                flexWrap: 'wrap',
                gap: '0.5rem',
                marginLeft: '2.0rem',
              }}
            >
              {subOptions.map((so) => renderOption(so, value))}
            </FlexRow>
          ) : (
            <FlexColumn
              style={{
                flex: 1,
                gap: '0.5rem',
                marginLeft: '2.0rem',
              }}
            >
              {subOptions.map((so) => renderOption(so, value))}
            </FlexColumn>
          ))}
      </FlexColumn>
    );
  };

  const optionComponents = options.map((option) => renderOption(option, null));

  return (
    <div {...{ style }}>
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
