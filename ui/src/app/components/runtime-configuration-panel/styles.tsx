import colors, { addOpacity, colorWithWhiteness } from 'app/styles/colors';
import { reactStyles } from 'app/utils';

export const styles = reactStyles({
  baseHeader: {
    color: colors.primary,
    fontSize: '16px',
    lineHeight: '1rem',
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
    marginRight: '.5rem',
  },
  labelAndInput: {
    alignItems: 'center',
  },
  controlSection: {
    backgroundColor: String(addOpacity(colors.white, 0.75)),
    borderRadius: '3px',
    padding: '.75rem',
  },
  presetMenuItem: {
    color: colors.primary,
    fontSize: '14px',
  },
  formGrid2: {
    display: 'grid',
    gridTemplateColumns: 'repeat(2, 1fr)',
    gridGap: '1rem',
    alignItems: 'center',
  },
  formGrid3: {
    display: 'grid',
    gridTemplateColumns: 'repeat(3, 1fr)',
    gridGap: '1rem',
    alignItems: 'center',
  },
  sectionTitle: {
    fontWeight: 600,
    marginBottom: '0.5rem',
  },
  inputNumber: {
    backgroundColor: colors.white,
    padding: '.75rem .5rem',
    width: '2rem',
  },
  errorMessage: {
    backgroundColor: colorWithWhiteness(colors.highlight, 0.5),
    marginTop: '0.5rem',
    color: colors.primary,
    fontSize: '14px',
    padding: '0.5rem',
    borderRadius: '0.5em',
  },
  costPredictorWrapper: {
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
    padding: '.25rem .5rem',
    width: '400px',
  },
  costsDrawnFrom: {
    borderLeft: `1px solid ${colorWithWhiteness(colors.dark, 0.5)}`,
    fontSize: '12px',
    padding: '.33rem .5rem',
    width: '200px',
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
    gridColumnGap: '.4rem',
    gridRowGap: '.7rem',
    fontSize: '14px',
    fontWeight: 500,
    padding: '.5rem',
    marginTop: '1rem',
    marginBottom: '1rem',
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
    marginTop: '1rem',
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
});
