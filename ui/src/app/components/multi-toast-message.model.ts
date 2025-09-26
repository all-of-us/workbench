import React from 'react';

import { ToastType } from './toast-banner';

export interface MultiToastMessage {
  id: string;
  title: string;
  message: React.ReactNode;
  toastType: ToastType;
  footer?: string | JSX.Element;
}
