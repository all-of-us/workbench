import { screen } from '@testing-library/react';

export const findRTTitle = async () => {
  return screen.queryByText(/responsible conduct of research training/i);
};
export const queryForRTTitle = () => {
  return screen.queryByText(/responsible conduct of research training/i);
};
export const findCTTitle = async () => {
  return screen.queryByText(/controlled tier training/i);
};

export const queryForCTTitle = () => {
  return screen.queryByText(/controlled tier training/i);
};
