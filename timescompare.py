import sys
import struct
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.dates as md

file = open(sys.argv[1], 'rb')

recordsize = 2*8+3*4

read = file.read(recordsize)

t1 = []
t2 = []

while len(read) > 0:
    data = struct.unpack('>qqfff', read)
    # print('data:', data )
    t1.append(data[0])
    t2.append(data[1]/1e6)
    read = file.read(recordsize)

x = range(len(t1))
t1 = np.array(t1) - t1[0]
t2 = np.array(t2) - t2[0]

plt.plot(t1, x)
plt.plot(t2, x)

plt.show()
