# vim: set fileencoding=utf8 :
import sys
import struct
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.dates as md
from datetime import datetime
from datetime import timedelta
import pytz

tz = pytz.timezone('Europe/Amsterdam')

logPath = sys.argv[1]
file = open(logPath, 'rb')

recordsize = 2*8+3*4

read = file.read(recordsize)

t = []
x = []
y = []
z = []

while len(read) > 0:
    data = struct.unpack('>qqfff', read)
    # print('data:', data )
    #in utc time
    unixtime = data[0]/1e3

    t.append(unixtime)
    x.append(data[2])
    y.append(data[3])
    z.append(data[4])
    read = file.read(recordsize)



x = np.array(x)
y = np.array(y)
z = np.array(z)
# norms = np.sqrt(x*x+ y*y + z*z)

# print(x.mean())
# print(y.mean())
# print(z.mean())


t_matplotlib = list(map(md.epoch2num, t))

plt.figure(figsize=(20,3))
plt.plot(t,x)
plt.plot(t,y)
plt.plot(t,z)
# plt.plot(t_matplotlib, norms)

def getDateRange(times):
    minT = datetime.fromtimestamp(np.min(t))
    maxT = datetime.fromtimestamp(np.max(t))
    print(minT, ', ', maxT)
    if maxT.day == minT.day:
        oneDay = timedelta(days=1)
        startDate = minT - oneDay
        startDate = datetime(startDate.year, startDate.month, startDate.day, 21)
    else:
        startDate = minT
        startDate = datetime(minT.year, minT.month, minT.day, 21, 30)

    endDate = datetime(maxT.year, maxT.month, maxT.day, 9,0)

    return (startDate, endDate)


startDate, endDate = getDateRange(t)

ax = plt.gca()
ax.set_yscale('log')
# ax.xaxis_date()
# xfmt = md.DateFormatter('%H:%M', tz=tz)
# ax.xaxis.set_major_formatter(xfmt)
# print(len(t))
# plt.xlim((md.epoch2num(startDate.timestamp()), md.epoch2num(endDate.timestamp())))
# plt.xticks(np.arange(md.epoch2num(startDate.timestamp()), md.epoch2num(endDate.timestamp()), 1/48))
plt.title(startDate.strftime('%a %Y-%m-%d') + ' – ' + endDate.strftime('%a %Y-%m-%d'))


prevTime = np.min(t)
plt.show()
# plt.savefig(logPath + '.pdf', bbox_inches='tight')
