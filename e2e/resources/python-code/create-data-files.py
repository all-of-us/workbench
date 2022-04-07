import os
import datetime

def timestamp():
    return datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")

print('Generating files at '+ timestamp())
for i in range(6):
    newfile = open(f"data{i}.txt", "wb")
    newfile.write(os.urandom(30 * 1000 * 1000))    # generate 30MB random content file
    newfile.close()
