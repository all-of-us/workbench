export const needsAttributes = node => {
  const isAge =
    node.get('type', '').match(/^DEMO.*/i)
    && node.get('subtype', '').match(/AGE/i);

  return isAge;
};
