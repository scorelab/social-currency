import sys
import os
import logging

from db.db_handler import *
from utils.crypto_utils import sign_senz
from config.config import *

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)
filehandler = logging.FileHandler('logs/stock_exchange.logs')
filehandler.setLevel(logging.INFO)

# create a logging format
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s -  %(message)s')
filehandler.setFormatter(formatter)
# add the handlers to the logger
logger.addHandler(filehandler)


class SenzHandler():
    """
    Handler incoming senz messages from here. We are dealing with following
    senz types
        1. GET
        2. PUT
        3. SHARE
        4. DATA
        5. DELETE
        6. UNSHARE

    According to the senz type different operations need to be carry out
    """

    def __init__(self, transport):
        """
        Initilize udp transport from here. We can use transport to send message
        to udp socket

        Arg
            trnsport - twisted transport instance
        """
        self.transport = transport

    def handleSenz(self, senz):
        """
        Handle differennt types of senz from here. This function will be called
        asynchronously. Whenc senz message receives this function will be
        called by twisted thread(thread safe mode via twisted library)
        """

        #print "Hanlder "  ,senz.attributes ,senz.type ,senz.receiver,senz.sender
        logger.info('senz received %s' % senz.type)
        dbh = db_handler()

        # tempory adding function
        if (senz.receiver == None):
            dbh.testData()

        # print senz.type=="DATA" and senz.receiver !=None
        if (senz.type == "DATA" and senz.receiver != None):
            flag = senz.attributes["#f"]
            if(flag=="ct"):
                logger.info('Doing p2p Transaction ::%s' % senz)
                #print (senz.attributes)
                dbh.addCoinWiseTransaction(senz.attributes)

            else:
                dbh.addCoinWiseTransaction(senz.attributes)  # ddd added coinWiseTransaction method
        elif (senz.type == "SHARE"):
            # print dbh.calulateCoinsValue()
            flag = senz.attributes["#f"]
            if(flag=="cv"):
                senze = 'PUT #COIN_VALUE %s ' % (dbh.calulateCoinsValue())
                senz = str(senze) + "@%s  ^%s" % (senz.sender, clientname)
                signed_senz = sign_senz(senz)
                logger.info('Auto Excute: %s' % signed_senz)
                self.transport.write(signed_senz)
            if(flag=="ctr"):
                logger.info('Request Massage Transaction :: %s' % senz)

        elif (senz.type == "DELETE"):
            coin = senz.attributes["#COIN"]
            dbh.delectCoinDetail(coin)

        elif (senz.type=="UNSHARE"):
            pass

    def postHandle(self, arg):
        """
        After handling senz message this function will be called. Basically
        this is a call back funcion
        """
        logger.info("Post Handled")
