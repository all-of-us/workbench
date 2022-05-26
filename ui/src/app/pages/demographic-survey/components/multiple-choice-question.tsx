import * as React from 'react';
import { CSSProperties } from 'react';

import { FlexColumn, FlexRow } from 'app/components/flex';
import { CheckBox, RadioButton } from 'app/components/inputs';
import { TooltipTrigger } from 'app/components/popups';
import { useId } from 'app/utils';

import { styles } from './styles';

const Option = (props: {
  checked: boolean;
  option: string;
  style?: CSSProperties;
  onChange: (any) => void;
  multiple?: boolean;
  disabled?: boolean;
  disabledText?: string;
}) => {
  const { checked, disabled, disabledText, multiple, onChange, option } = props;
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
            value={option}
          />
        )}
        <label htmlFor={id} style={styles.answer}>
          {option}
        </label>
      </FlexRow>
    </TooltipTrigger>
  );
};

interface MultipleChoiceOption {
  name: string;
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

  const handleChange = (e, name) => {
    if (multiple) {
      const result = e
        ? [...(selected as string[]), name]
        : (selected as string[]).filter((r) => r !== name);
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
              option={choice.name}
              checked={
                multiple
                  ? selected.includes(choice.name)
                  : selected === choice.name
              }
              onChange={(e) => handleChange(e, choice.name)}
              multiple={multiple}
            />

            {choice.showInput &&
              ((multiple && selected.includes(choice.name)) ||
                selected === choice.name) && (
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
