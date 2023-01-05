import { withStyle } from 'app/utils/index';

export const styles = {
  formSection: {
    marginTop: '1.5rem',
    minWidth: '43.5rem',
  },
};

export const FormSection = withStyle(styles.formSection)('div');
