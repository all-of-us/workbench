import * as React from 'react';
import { useState } from 'react';

import { FlexColumn } from './flex';
import { MultiToastMessage } from './multi-toast-message.model';
import { PaginationNavigation } from './pagination-navigation';
import { ToastBanner } from './toast-banner';

interface Props {
  messages: MultiToastMessage[];
  zIndex?: number;
  onDismiss: (messageId: string) => void;
}

export const MultiToastBanner = ({ messages, zIndex, onDismiss }: Props) => {
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
              <PaginationNavigation
                {...{
                  currentIndex,
                  setCurrentIndex,
                  numElements: messages.length,
                  singularName: 'message',
                }}
              />
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
        zIndex={zIndex}
      />
    </FlexColumn>
  );
};
