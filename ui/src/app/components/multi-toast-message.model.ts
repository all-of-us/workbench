import { ToastType } from './toast-banner';

export interface MultiToastMessage {
  id: string;
  title: string;
  message: string | JSX.Element;
  toastType: ToastType;
  footer?: string | JSX.Element;
}
