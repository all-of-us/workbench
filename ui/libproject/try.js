function validate_options(arg) {
  'use strict';
  if (!!arg.project || !!arg.version || !!arg.promote) {
    return false;
  }
  return true;
}