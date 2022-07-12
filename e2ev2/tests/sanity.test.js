test('adds 1 + 2 to equal 3', () => {
  expect(1 + 2).toBe(3)
})

test('waits for addition', async () => {
  const sum = await new Promise(resolve => setTimeout(() => resolve(6 + 2), 10))
  expect(sum).toBe(8)
})
