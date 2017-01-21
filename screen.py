#!/usr/bin/env python3
import sys
import struct
import numpy as np
from datetime import datetime
from datetime import timedelta
import os

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


displayLogPath = sys.argv[1]

for tScreen, displayState in zip(*readScreen(displayLogPath)):
    time = datetime.fromtimestamp(tScreen)
    displayStateString = 'Off' if displayState == 1 \
            else 'On' if displayState == 2 else displayState

    print(time, displayStateString)

