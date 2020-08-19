from matplotlib import pyplot as plt
import pandas as pd

s = pd.Series([1,2,3,4], index=['a','b','c', 'e'])

s.plot.bar(figsize=(10,10))
plt.xlabel('x-label')
plt.ylabel('y-label')
plt.title("Hello Notebook")
