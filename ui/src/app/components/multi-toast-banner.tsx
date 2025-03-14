import * as React from 'react';
import { useState } from 'react';

import colors from 'app/styles/colors';

import { FlexColumn, FlexRow } from './flex';
import { ArrowLeft, ArrowRight } from './icons';
import { MultiToastMessage } from './multi-toast-message.model';
import { ToastBanner } from './toast-banner';

interface Props {
  messages: MultiToastMessage[];
  baseZIndex?: number;
  onDismiss: (messageId: string) => void;
}

export const MultiToastBanner = ({
  messages,
  baseZIndex = 1000,
  onDismiss,
}: Props) => {
  const [currentIndex, setCurrentIndex] = useState(0);

  if (messages.length === 0) {
    return null;
  }

  const currentMessage = messages[currentIndex];
  const hasMultipleMessages = messages.length > 1;

  return (
    <FlexColumn>
      <ToastBanner
        key={currentMessage.id}
        title={currentMessage.title}
        message={
          <FlexColumn>
            <div>{currentMessage.message}</div>
            {hasMultipleMessages && (
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
                    title='Previous message'
                    style={{
                      cursor: currentIndex > 0 ? 'pointer' : 'not-allowed',
                      color: currentIndex > 0 ? colors.accent : colors.disabled,
                    }}
                    onClick={() =>
                      currentIndex > 0 && setCurrentIndex((prev) => prev - 1)
                    }
                  />
                  <span style={{ fontSize: '12px', color: colors.primary }}>
                    {currentIndex + 1} of {messages.length}
                  </span>
                  <ArrowRight
                    size={16}
                    title='Next message'
                    style={{
                      cursor:
                        currentIndex < messages.length - 1
                          ? 'pointer'
                          : 'not-allowed',
                      color:
                        currentIndex < messages.length - 1
                          ? colors.accent
                          : colors.disabled,
                    }}
                    onClick={() =>
                      currentIndex < messages.length - 1 &&
                      setCurrentIndex((prev) => prev + 1)
                    }
                  />
                </FlexRow>
              </FlexRow>
            )}
          </FlexColumn>
        }
        footer={currentMessage.footer}
        onClose={() => {
          onDismiss(currentMessage.id);
          setCurrentIndex(
            Math.min(currentIndex, Math.max(messages.length - 2, 0))
          );
        }}
        toastType={currentMessage.toastType}
        zIndex={baseZIndex}
      />
    </FlexColumn>
  );
};
