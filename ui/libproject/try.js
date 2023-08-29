function validate_options(arg) {
  'use strict';
  console.log('hey');
  if (!!arg.project || !!arg.version || !!arg.promote) {
    return false;
  }
  return true;
}