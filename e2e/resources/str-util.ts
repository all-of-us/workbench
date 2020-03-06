/**
 * Get a random alphanumeric lowercase string
 */
const randAlphaString = (len) => {
  let s = '';
  while (s.length < len) { s += Math.random().toString(36).substr(2, len - s.length); }
  return s.toLowerCase();
};

module.exports = {
  randomString: randAlphaString
};
