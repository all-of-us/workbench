import * as React from 'react';

import { FlexRow } from 'app/components/flex';
import colors from 'app/styles/colors';

import { ArrowLeft, ArrowRight } from './icons';

export const PaginationNavigation = (props: {
  currentIndex: number;
  setCurrentIndex: (index: number) => void;
  numElements: number;
  singularName?: string;
}) => {
  const { currentIndex, setCurrentIndex, numElements, singularName } = props;

  return (
    <FlexRow
      style={{
        marginTop: '0.5rem',
        alignItems: 'center',
        justifyContent: 'space-between',
      }}
    >
      <FlexRow style={{ alignItems: 'center', gap: '0.5rem' }}>
        <ArrowLeft
          size={16}
          title={`Previous ${singularName || 'element'}`}
          style={{
            cursor: currentIndex > 0 ? 'pointer' : 'not-allowed',
            color: currentIndex > 0 ? colors.accent : colors.disabled,
          }}
          onClick={() => currentIndex > 0 && setCurrentIndex(currentIndex - 1)}
        />
        <span style={{ fontSize: '12px', color: colors.primary }}>
          {currentIndex + 1} of {numElements}
        </span>
        <ArrowRight
          size={16}
          title={`Next ${singularName || 'element'}`}
          style={{
            cursor: currentIndex < numElements - 1 ? 'pointer' : 'not-allowed',
            color:
              currentIndex < numElements - 1 ? colors.accent : colors.disabled,
          }}
          onClick={() =>
            currentIndex < numElements - 1 && setCurrentIndex(currentIndex + 1)
          }
        />
      </FlexRow>
    </FlexRow>
  );
};
