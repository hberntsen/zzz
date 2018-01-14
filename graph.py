import sys
import struct
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.dates as md
from datetime import datetime
from datetime import timedelta
import pytz
import os
from math import ceil

tz = pytz.timezone('Europe/Amsterdam')

def getbg(allT, allPitch, allRoll, threshold = 2, clusterPeriod = 5*60):
    bg = []
    lastmotion = None

    pitchRoll = np.stack((allPitch, allRoll), axis=-1)
    allT = np.array(allT)

    startIndex = 0
    for i, tEnd in enumerate(allT):
        # compare to 10 seconds ago
        while allT[startIndex] < tEnd - 10:
            startIndex+=1

        p1 = pitchRoll[startIndex]
        p2 = pitchRoll[i]
        distance = np.linalg.norm(p1-p2)

        if startIndex == i:
            continue

        if distance > threshold:
            if lastmotion and tEnd - lastmotion > clusterPeriod:
                bg.append((lastmotion, False))
            if not bg:
                bg.append((tEnd, True))
            elif not bg[-1][1]: #only append first time
                bg.append((tEnd, True))

            lastmotion = tEnd

    if allT[-1] - lastmotion < clusterPeriod:
        bg.append((allT[-1], False))
    else:
        bg.append((tEnd, True))

    return bg


def readAccelerometer(logPath):
    file = open(logPath, 'rb')

    versionData = file.read(4)
    version = struct.unpack('>i', versionData)[0]

    if version == 2:
        recordsize = 1*8+3*4
        dataFormat = '>qfff'
        xyzOffset = 0
    else:
        recordsize = 2*8+3*4
        dataFormat = '>qqfff'
        xyzOffset = 1
        file.close()
        file = open(logPath, 'rb')


    read = file.read(recordsize)

    t = []
    x = []
    y = []
    z = []

    while len(read) > 0:
        data = struct.unpack(dataFormat, read)
        # print('data:', data )
        #in utc time
        unixtime = data[0]/1e3

        t.append(unixtime)
        x.append(data[1+xyzOffset])
        y.append(data[2+xyzOffset])
        z.append(data[3+xyzOffset])
        read = file.read(recordsize)

    x = np.array(x)
    y = np.array(y)
    z = np.array(z)
    pitch = 180 * np.arctan(x/np.sqrt(y*y + z*z)) / np.pi
    roll = 180* np.arctan(y/np.sqrt(x*x+z*z))/np.pi
    return (t, pitch, roll)

def readScreen(logPath):
    file = open(logPath, 'rb')

    versionData = file.read(4)
    version = struct.unpack('>i', versionData)[0]

    if version != 2:
        file.close()
        file = open(logPath, 'rb')

    recordsize = 8+1

    read = file.read(recordsize)

    t = []
    displayStates = []

    while len(read) > 0:
        data = struct.unpack('>qb', read)
        # print('data:', data )
        #in utc time
        unixtime = data[0]/1e3

        t.append(unixtime)
        displayStates.append(data[1])
        read = file.read(recordsize)

    return (t, displayStates)


logPath = sys.argv[1]
outFormat = sys.argv[2]
t, pitch, roll = readAccelerometer(logPath)

bg = getbg(t, pitch, roll)

t_matplotlib = list(map(md.epoch2num, t))

plt.figure(figsize=(20,3))
plt.plot(t_matplotlib,pitch, linewidth=0.5, color='blue')
plt.plot(t_matplotlib,roll, linewidth=0.5, color='green')

def getDateRange(times):
    minT = datetime.fromtimestamp(np.min(times))
    maxT = datetime.fromtimestamp(np.max(times))
    if maxT.day == minT.day:
        oneDay = timedelta(days=1)
        startDate = minT - oneDay
        startDate = datetime(startDate.year, startDate.month, startDate.day, 21)
    else:
        startDate = minT
        startDate = datetime(minT.year, minT.month, minT.day, 21, 30)

    endDate = datetime(maxT.year, maxT.month, maxT.day, 9,0)

    if minT < startDate or maxT > endDate:
        startDate = datetime(minT.year, minT.month, minT.day, minT.hour, minT.minute // 15 * 15)
        endDate = datetime(maxT.year, maxT.month, maxT.day, maxT.hour) + timedelta(minutes=ceil(maxT.minute / 15) * 15)

    return (startDate, endDate)


startDate, endDate = getDateRange(t)

ax = plt.gca()
ax.xaxis_date()
xfmt = md.DateFormatter('%H:%M', tz=tz)
ax.xaxis.set_major_formatter(xfmt)
# print(len(t))
plt.xlim((md.epoch2num(startDate.timestamp()), md.epoch2num(endDate.timestamp())))
plt.ylim((-20, 20))
plt.xticks(np.append(np.arange(
    md.epoch2num(startDate.timestamp()),
    md.epoch2num(endDate.timestamp()),
    1000001/96000000), md.epoch2num(endDate.timestamp())))
for label in ax.xaxis.get_ticklabels()[1::2]:
    label.set_visible(False)
# plt.xticks(rotation=90)
plt.title(startDate.strftime('%a %Y-%m-%d') + ' – ' + endDate.strftime('%a %Y-%m-%d'))


prevTime = np.min(t)
for b in bg:
    color = 'lightgreen' if b[1] else 'orange'
    plt.axvspan(md.epoch2num(prevTime),md.epoch2num(b[0]),facecolor=color,
            alpha=0.20, zorder=-100, edgecolor='gray')
    prevTime = b[0]

displayLogPath = logPath[:-len('accelerometer')] + 'screen'
if os.path.exists(displayLogPath):
    tScreen, displayStates = readScreen(displayLogPath)
    prevTime = tScreen[0]
    prevState = displayStates[0]
    for tScreen, displayState in zip(tScreen[1:], displayStates[1:]):
        if displayState != prevState:
            STATE_OFF = 1
            STATE_ON = 2
            if displayState == STATE_OFF:
                plt.axvspan(md.epoch2num(prevTime), md.epoch2num(tScreen),
                        facecolor='red', alpha=0.9, zorder=-90, edgecolor= 'red')
            prevTime = tScreen
            prevState = displayState

resetsLogPath = logPath[:-len('accelerometer')] + 'resets'
if os.path.exists(resetsLogPath):
    tsReset, _ = readScreen(resetsLogPath)
    for tReset in tsReset:
        plt.axvspan(md.epoch2num(tReset), md.epoch2num(tReset+30),
                facecolor='purple', alpha=0.9, zorder=10, edgecolor='purple')
# plt.show()
plt.savefig(logPath + '.' + outFormat, bbox_inches='tight', dpi=175)
