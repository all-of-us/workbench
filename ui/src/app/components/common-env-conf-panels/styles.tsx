import colors, { addOpacity, colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';

export const styles = reactStyles({
  baseHeader: {
    color: colors.primary,
    fontSize: '16px',
    lineHeight: '1.5rem',
    margin: 0,
  },
  sectionHeader: {
    marginBottom: '12px',
    marginTop: '12px',
  },
  bold: {
    fontWeight: 700,
  },
  label: {
    fontWeight: 600,
    marginRight: '.75rem',
  },
  labelAndInput: {
    alignItems: 'center',
  },
  controlSection: {
    backgroundColor: String(addOpacity(colors.white, 0.75)),
    borderRadius: '3px',
    padding: '1.125rem',
  },
  presetMenuItem: {
    color: colors.primary,
    fontSize: '14px',
  },
  formGrid2: {
    display: 'grid',
    gridTemplateColumns: 'repeat(2, 1fr)',
    gridGap: '1.5rem',
    alignItems: 'center',
  },
  formGrid3: {
    display: 'grid',
    gridTemplateColumns: 'repeat(3, 1fr)',
    gridGap: '1.5rem',
    alignItems: 'center',
  },
  sectionTitle: {
    fontWeight: 600,
    marginBottom: '0.75rem',
  },
  inputNumber: {
    backgroundColor: colors.white,
    padding: '1.125rem .75rem',
    width: '3rem',
  },
  errorMessage: {
    backgroundColor: colorWithWhiteness(colors.highlight, 0.5),
    marginTop: '0.75rem',
    color: colors.primary,
    fontSize: '14px',
    padding: '0.75rem',
    borderRadius: '0.5em',
  },
  environmentInformedActionPanelWrapper: {
    backgroundColor: colorWithWhiteness(colors.accent, 0.85),
    // Not using shorthand here because react doesn't like it when you mix shorthand and non-shorthand,
    // and the border color changes when the runtime does
    borderWidth: '1px',
    borderStyle: 'solid',
    borderColor: colorWithWhiteness(colors.dark, 0.5),
    borderRadius: '5px',
    color: colors.dark,
  },
  costComparison: {
    padding: '.375rem .75rem',
    width: '400px',
  },
  costsDrawnFrom: {
    fontSize: '14px',
    paddingTop: '.495rem',
  },
  deleteLink: {
    alignSelf: 'center',
    fontSize: '16px',
    textTransform: 'uppercase',
  },
  confirmWarning: {
    backgroundColor: colorWithWhiteness(colors.warning, 0.9),
    border: `1px solid ${colors.warning}`,
    borderRadius: '5px',
    display: 'grid',
    gridColumnGap: '.6rem',
    gridRowGap: '1.05rem',
    fontSize: '14px',
    fontWeight: 500,
    padding: '.75rem',
    marginTop: '1.5rem',
    marginBottom: '1.5rem',
  },
  confirmWarningText: {
    color: colors.primary,
    margin: 0,
  },
  gpuCheckBox: {
    marginRight: '10px',
  },
  gpuCheckBoxRow: {
    alignItems: 'center',
    gap: '10px',
  },
  gpuSection: {
    gap: '10px',
    marginTop: '1.5rem',
  },
  sparkConsoleHeader: {
    color: '#333F52',
    fontSize: '14px',
    fontWeight: 600,
    margin: 0,
  },
  sparkConsoleSection: {
    backgroundColor: colors.light,
    border: '1px solid #4D72AA',
    borderRadius: '5px',
    fontSize: '14px',
    padding: '21px 17px',
  },
  sparkConsoleLaunchButton: {
    border: '1px solid #4D72AA',
    borderRadius: '2px',
    display: 'inline-block',
    marginTop: '17px',
    padding: '10px 21px',
  },
  diskRow: {
    gap: '8px',
  },
  diskRadio: {
    height: '24px',
  },
  diskLabel: {
    fontWeight: 500,
  },
  disabledCloudProfile: {
    marginRight: '0.5rem',
    backgroundColor: '#ededed',
    color: '#7b828e',
    padding: '1rem 2rem 1rem 0.5rem',
    borderRadius: '0.5rem',
  },
});
