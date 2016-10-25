import sys
import struct
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.dates as md
from datetime import datetime
from datetime import timedelta
import pytz

tz = pytz.timezone('Europe/Amsterdam')

def getbg(t, x, y, z, norms):
    bg = []
    lastmotion = None

    std = np.std(norms)
    mean = np.mean(norms)

    for t, x, y ,z, norm in zip(t, x, y, z, norms):
        if norm > mean + std or norm < mean - std:
            if lastmotion and t - lastmotion > 5*60:
                bg.append((lastmotion, False))
            elif not bg:
                bg.append((t, True))
            elif not bg[-1][1]:
                bg.append((t, True))

            lastmotion = t
        # elif not bg:
            # pass
        # elif bg[-1][1]:
            # bg.append((unixtime, False))
    return bg

def getbg2(allT, allPitch, allRoll):
    bg = []
    lastmotion = None

    pitchRoll = np.sum(np.stack((allPitch, allRoll), axis=-1), axis=1)

    diffs = np.abs(np.diff(pitchRoll))
    k = 1

    diffSums = []
    allT = np.array(allT)

    startIndex = 0
    #skip first because of diffs
    for i, tEnd in enumerate(allT[1:]):
        while allT[startIndex] < tEnd - 10:
            startIndex+=1

        localPitchRoll = pitchRoll[startIndex:i]
        travel = np.sum(diffs[startIndex: i])
        rise = i
        diffSums.append(diff)

        if diff > mean + std or diff < mean - std:
            if lastmotion and t - lastmotion > 5*60:
                bg.append((lastmotion, False))
            elif not bg:
                bg.append((t, True))
            elif not bg[-1][1]:
                bg.append((t, True))

            lastmotion = t
        # elif not bg:
            # pass
        # elif bg[-1][1]:
            # bg.append((unixtime, False))
    return [bg, diffSums]

def getbg3(allT, allPitch, allRoll, threshold = 2, clusterPeriod = 5*60):
    bg = []
    distances = []
    lastmotion = None

    pitchRoll = np.stack((allPitch, allRoll), axis=-1)
    allT = np.array(allT)

    startIndex = 0
    for i, tEnd in enumerate(allT):
        while allT[startIndex] < tEnd - 10:
            startIndex+=1

        p1 = pitchRoll[startIndex]
        p2 = pitchRoll[i]
        distance = np.linalg.norm(p1-p2)
        distances.append(distance)

        if startIndex == i:
            continue

        if distance > threshold:
            if lastmotion and tEnd - lastmotion > clusterPeriod:
                bg.append((lastmotion, False))
            elif not bg:
                bg.append((tEnd, True))
            elif not bg[-1][1]:
                bg.append((tEnd, True))

            lastmotion = tEnd

    if allT[-1] - lastmotion < clusterPeriod:
        bg.append((allT[-1], False))
    else:
        bg.append((tEnd, True))

    return [bg, distances]



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
norms = np.sqrt(x*x+ y*y + z*z)
pitch = 180 * np.arctan(x/np.sqrt(y*y + z*z)) / np.pi
roll = 180* np.arctan(y/np.sqrt(x*x+z*z))/np.pi


# bg, diffSums = getbg2(t, x, y, z, norms)
bg, diffSums = getbg3(t, pitch, roll)

t_matplotlib = list(map(md.epoch2num, t))

plt.figure(figsize=(20,3))
# plt.plot(t,x)
# plt.plot(t,y)
# plt.plot(t,z)
# plt.plot(t_matplotlib, norms)
# plt.plot(t_matplotlib, diffSums)
plt.plot(t_matplotlib,pitch)
plt.plot(t_matplotlib,roll)
# plt.plot(list(map(md.epoch2num, restx)),resty)

def getDateRange(times):
    minT = datetime.fromtimestamp(np.min(t))
    maxT = datetime.fromtimestamp(np.max(t))
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
ax.xaxis_date()
xfmt = md.DateFormatter('%H:%M', tz=tz)
ax.xaxis.set_major_formatter(xfmt)
# print(len(t))
plt.xlim((md.epoch2num(startDate.timestamp()), md.epoch2num(endDate.timestamp())))
plt.ylim((-30, 30))
plt.xticks(np.arange(md.epoch2num(startDate.timestamp()), md.epoch2num(endDate.timestamp()), 1/48))
# plt.xticks(rotation=90)
plt.title(startDate.strftime('%a %Y-%m-%d') + ' – ' + endDate.strftime('%a %Y-%m-%d'))


prevTime = np.min(t)
for b in bg:
    color = 'lightgreen' if b[1] else 'red'
    plt.axvspan(md.epoch2num(prevTime),md.epoch2num(b[0]),facecolor=color,
            alpha=0.25, zorder=-100, edgecolor= 'black')
    prevTime = b[0]

# plt.show()
plt.savefig(logPath + '.pdf', bbox_inches='tight')
