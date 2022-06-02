import * as React from 'react';
import { CSSProperties } from 'react';

import { FlexColumn, FlexRow } from 'app/components/flex';
import { CheckBox, RadioButton } from 'app/components/inputs';
import { TooltipTrigger } from 'app/components/popups';
import { reactStyles, useId } from 'app/utils';

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
  disabled?: boolean;
  disabledText?: string;
}

export const MultipleChoiceQuestion = (props: {
  question: string;
  choices: MultipleChoiceOption[];
  selected: string | string[];
  onChange: (any) => void;
  style?: CSSProperties;
  multiple?: boolean;
}) => {
  const { choices, onChange, question, selected, multiple, style } = props;

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

  return (
    <FlexRow style={{ ...style, alignItems: 'center' }}>
      <div style={{ ...styles.question, flex: 1, paddingRight: '1rem' }}>
        {question}
      </div>
      <FlexRow style={{ flex: 1, flexWrap: 'wrap', gap: '0.5rem' }}>
        {choices.map((choice) => (
          <FlexColumn>
            <Option
              disabled={choice.disabled}
              disabledText={choice.disabledText}
              value={choice.value}
              label={choice.label}
              checked={
                multiple
                  ? selected.includes(choice.value)
                  : selected === choice.value
              }
              onChange={(e) => handleChange(e, choice.value)}
              multiple={multiple}
            />

            {choice.showInput &&
              ((multiple && selected.includes(choice.label)) ||
                selected === choice.label) && (
                <input
                  data-test-id='search'
                  style={{
                    marginBottom: '.5em',
                    marginLeft: '0.75rem',
                    width: '300px',
                  }}
                  type='text'
                  placeholder='Search'
                  value={choice.otherText}
                  onChange={choice.onChange}
                />
              )}
          </FlexColumn>
        ))}
      </FlexRow>
    </FlexRow>
  );
};
