import time
import sys
import os
import logging
import multiprocessing
import tkMessageBox

from Tkinter import Tk
import base_ui.main_window_app
import gettext
from twisted.internet.protocol import DatagramProtocol
from twisted.internet import reactor, threads
from utils.senz_parser import parse
from base_ui import cumulative_logger
from utils.crypto_utils import *
from handlers.senz_handler import *
from config.config import *

_ = gettext.gettext

logging.basicConfig()  # comment this stop console logger print
logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

if not (os.path.exists('logs')):
    os.mkdir('logs')

filehandler = logging.FileHandler('logs/stock_exchange.logs')
filehandler.setLevel(logging.INFO)
# create a logging format
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
filehandler.setFormatter(formatter)
# add the handlers to the logger
logger.addHandler(filehandler)


class SenzcProtocol(DatagramProtocol):
    """
    Protocol will connects to udp port(which server runs on). When packet(semz)
    comes to server we have to asynchornosly handle them. We are starting
    thread save twisted thread on GET, SHARE and PUT senz
    """

    def __init__(self, host, port):
        """
        initiliaze senz server host and port

        Args:
            host - server host
            port - server port
        """
        self.host = host
        self.port = port

    def startProtocol(self):
        """
        Call when twisted udp protocol starts, Few actions need to be done from
        here
            1. First need to connect to udp socket from here.
            2. Then need to share public key to server via SHARE senz
            3. Finall need to start looping call to send ping messages to
               server in every 30 mins
        """
        logger.info('client started')
        self.transport.connect(self.host, self.port)

        # share public key on start
        self.share_pubkey()

    def stopProtocol(self):
        """
        Call when datagram protocol stops. Need to clear global connection if
        exits from here
       """
        reactor.callFromThread(reactor.stop)
        logger.info(_('client stopped( switch not connected)'))
        root = Tk()
        root.withdraw()
        tkMessageBox.showinfo("Message", "Switch not connected or not Start , try later")
        os._exit(0)

    def datagramReceived(self, datagram, host):
        """
        Call when datagram recived, datagrams are senz messages in our scenario
        We have to handle receiveing senz from here. Senz handling part will be
        delegated to SenzHandler

        Args:
            datagra - senz message
            host - receving host
        """
        logger.info(_('datagram received %s' % datagram))

        # handle receved datagram(senz)
        self.handle_datagram(datagram)

    def share_pubkey(self):
        """
        Send public key of the senzy to server via SHARE senz. We have to
        digitally senz the senz before sending to server.
        SHARE senz message would be like below

            SHARE:
                #pubkey <pubkey>
                #time <time>
            @mysensors
            ^<sender> <digital signature>
        """
        # TODO get sender and receiver config

        # send pubkey to server via SHARE senz
        pubkey = get_pubkey()
        receiver = servername
        sender = clientname

        senz = "SHARE #pubkey %s #time %s @%s ^%s" % (pubkey, time.time(), receiver, sender)
        signed_senz = sign_senz(senz)

        # print(signed_senz)
        self.transport.write(signed_senz)

    def handle_datagram(self, datagram):
        """
        Handle receving senz from here, we have to do
            1. Parse the datagram and obtain senz
            2. We have to ignore ping messages from server
            3. We have to handler GET, SHARE, PUT senz messages via SenzHandler
        """

        if datagram == 'PING':
            # we ingnore ping messages
            # logger.info('ping received')
            pass  # temporry stop pin message
        else:
            # parse senz first
            senz = parse(datagram)

            # start threads for GET, PUT, DATA, SHARE senz  , UNSHARE
            handler = SenzHandler(self.transport)
            d = threads.deferToThread(handler.handleSenz, senz)
            d.addCallback(handler.postHandle)


def init():
    """
    Init client certificates from here. All keys will be stored in .keys/
    directory in project root. We have to verify thie content of that directory
    while initializing the keys
    """
    # init keys via crypto utils
    init_keys()


def start():
    """
    Start upd senz protocol from here. It means connecting to senz server. We
    have to provide server host and port details form here.(read from config)
    """

    init()

    # TODO get host and port from config
    host = serverhost
    port = serverport

    # start ptotocol
    protocol = SenzcProtocol(host, port)
    reactor.listenUDP(0, protocol)
    reactor.run(installSignalHandlers=False)


if __name__ == '__main__':
    global t, t1
    t = multiprocessing.Process(target=start, args=())
    t.start()

    cl = cumulative_logger.CumulativeLogger()
    logger.info(_('Starting the SCPP Stock Exchange...!'))
    t1 = multiprocessing.Process(target=base_ui.main_window_app.MainWindowApp(cl).run(), args=())
    t1.start()
