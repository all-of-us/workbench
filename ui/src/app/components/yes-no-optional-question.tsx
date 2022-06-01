import * as React from 'react';
import { CSSProperties } from 'react';

import { MultipleChoiceQuestion } from './multiple-choice-question';

export const YesNoOptionalQuestion = (props: {
  question: string;
  selected: string;
  onChange: (any) => void;
  style?: CSSProperties;
}) => {
  const { question, selected, onChange, style } = props;

  return (
    <MultipleChoiceQuestion
      question={question}
      choices={[
        { name: 'Yes' },
        { name: 'No' },
        { name: 'Prefer not to answer' },
      ]}
      selected={selected}
      onChange={onChange}
      style={style}
    />
  );
};
