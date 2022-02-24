// common validators for validate.js

export const required = { presence: { allowEmpty: false } };
export const notTooLong = (maxLength) => ({
  length: {
    maximum: maxLength,
    tooLong: 'must be %{count} characters or less',
  },
});
