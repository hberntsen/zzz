import sys
import struct
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.dates as md

file = open(sys.argv[1], 'rb')

recordsize = 2*8+3*4

read = file.read(recordsize)

t = []
x = []
y = []
z = []
bg = []


lastmotion = None

while len(read) > 0:
    data = struct.unpack('>qqfff', read)
    # print('data:', data )
    #not really because of the added time zone correcton
    unixtime = data[0]/1e3 + 2*3600

    t.append(unixtime)
    x.append(data[2])
    y.append(data[3])
    z.append(data[4])

    norm = np.sqrt(data[2]*data[2] + data[3]*data[3] + data[4]*data[4])
    if norm > 10.8 or norm < 10.4:
        if lastmotion and unixtime - lastmotion > 5*60:
            bg.append((lastmotion, False))
        elif not bg:
            bg.append((unixtime, True))
        elif not bg[-1][1]:
            bg.append((unixtime, True))

        lastmotion = unixtime
    # elif not bg:
        # pass
    # elif bg[-1][1]:
        # bg.append((unixtime, False))

    read = file.read(recordsize)

x = np.array(x)
y = np.array(y)
z = np.array(z)
norms = np.sqrt(x*x+ y*y + z*z)
pitch = 180 * np.arctan(x/np.sqrt(y*y + z*z)) / np.pi
roll = 180* np.arctan(y/np.sqrt(x*x+z*z))/np.pi

print(np.min(t))
print(np.max(t))


t_matplotlib = list(map(md.epoch2num, t))
# plt.plot(t,x)
# plt.plot(t,y)
# plt.plot(t,z)
plt.plot(t_matplotlib, norms)
plt.plot(t_matplotlib,pitch)
plt.plot(t_matplotlib,roll)
# plt.plot(list(map(md.epoch2num, restx)),resty)

ax = plt.gca()
ax.xaxis_date()
xfmt = md.DateFormatter('%Y-%m-%d %H:%M:%S')
ax.xaxis.set_major_formatter(xfmt)
plt.xticks(np.arange(md.epoch2num(np.min(t)), md.epoch2num(np.max(t)), 1/48), rotation=90)

prevTime = np.min(t)
print(bg)
for b in bg:
    color = 'green' if b[1] else 'red'
    plt.axvspan(md.epoch2num(prevTime),md.epoch2num(b[0]),facecolor=color, alpha=0.5)
    prevTime = b[0]

plt.show()
