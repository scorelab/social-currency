import logging
import time
import scpp_switch


def calc(connector, progress):
    progress.set(0, 10)
    for i in range(10, 0, -1):
        progress.tick()
        connector.ack()  # can be ommitted in this program
        time.sleep(.2)
    scpp_switch.main()
