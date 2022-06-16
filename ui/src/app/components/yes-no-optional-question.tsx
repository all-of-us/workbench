import * as React from 'react';
import { CSSProperties } from 'react';

import { YesNoPreferNot } from 'generated/fetch';

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
      {...{ onChange, question, selected, style }}
      horizontalOptions={true}
      options={[
        { label: 'Yes', value: YesNoPreferNot.YES },
        { label: 'No', value: YesNoPreferNot.NO },
        {
          label: 'Prefer not to answer',
          value: YesNoPreferNot.PREFERNOTTOANSWER,
        },
      ]}
    />
  );
};
