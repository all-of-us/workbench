/**
 * Defines the React 16 Adapter for Enzyme.
 *
 * @link http://airbnb.io/enzyme/docs/installation/#working-with-react-16
 * @copyright 2017 Airbnb, Inc.
 */
const enzyme = require("enzyme");
const Adapter = require("enzyme-adapter-react-16");

const {setupCustomValidators} = require('./src/app/services/setup');
const {stubPopupDimensions} = require('./src/app/components/popups');
const {overridePollingDelay} = require('./src/app/utils/leo-runtime-initializer');

setupCustomValidators();
stubPopupDimensions();
// TODO(RW-5851): Revert this temporary hack. Set the poll value high enough that it will never complete within a unit test run.
overridePollingDelay(/* 1 day */ 24 * 60 * 60 * 1000);
enzyme.configure({ adapter: new Adapter() });
