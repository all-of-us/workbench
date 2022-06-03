import * as React from 'react';
import { CSSProperties } from 'react';

import { FlexColumn, FlexRow } from 'app/components/flex';
import { CheckBox, RadioButton } from 'app/components/inputs';
import { TooltipTrigger } from 'app/components/popups';
import { reactStyles, useId } from 'app/utils';

import { ClrIcon } from './icons';

const styles = reactStyles({
  question: { fontWeight: 'bold' },
  answer: { margin: '0.0rem 0.25rem' },
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
  onClick?: (any) => void;
  disabled?: boolean;
  disabledText?: string;
  subOptions?: MultipleChoiceOption[];
  showSubOptions?: boolean;
}

export const MultipleChoiceQuestion = (props: {
  question: string;
  options: MultipleChoiceOption[];
  selected: string | string[];
  onChange: (any) => void;
  style?: CSSProperties;
  multiple?: boolean;
}) => {
  const { options, onChange, question, selected, multiple, style } = props;

  const handleChange = (e, label) => {
    if (multiple) {
      const result = e
        ? [...(selected as string[]), label]
        : (selected as string[]).filter((r) => r !== label);
      onChange(result);
    } else {
      onChange(e.target.value);
    }
  };

  const renderOption = (option: MultipleChoiceOption) => {
    return (
      <FlexColumn style={option.subOptions && { width: '100%' }}>
        <FlexRow style={{ alignItems: 'center' }}>
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
              onClick={option.onClick}
            />
          )}
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
            onChange={(e) => handleChange(e, option.value)}
            multiple={multiple}
          />
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
              onChange={(e) => option.onChange(e.target.value)}
            />
          )}
        {option.showSubOptions && option.subOptions && (
          <FlexRow
            style={{
              flex: 1,
              flexWrap: 'wrap',
              gap: '0.5rem',
              marginLeft: '2.0rem',
            }}
          >
            {option.subOptions.map(renderOption)}
          </FlexRow>
        )}
      </FlexColumn>
    );
  };

  return (
    <div style={{ ...style }}>
      <div style={{ ...styles.question }}>{question}</div>
      <FlexRow style={{ flexWrap: 'wrap', gap: '0.5rem' }}>
        {options.map(renderOption)}
      </FlexRow>
    </div>
  );
};
