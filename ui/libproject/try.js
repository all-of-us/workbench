function validate_options(arg) {
  'use strict';
  alert('hey');
  if (!!arg.project || !!arg.version || !!arg.promote) {
    return false;
  }
  return true;
}