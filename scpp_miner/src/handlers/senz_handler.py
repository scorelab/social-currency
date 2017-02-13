import socket
import threading
import tkMessageBox

import datetime
from pyblake2 import blake2b

from db.db_handler import db_handler
from minner_algo_handler.minning_algo import minning_algo
from utils.senz_parser import *
from utils.crypto_utils import *
from config.config import *

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

filehandler = logging.FileHandler('logs/miner.log')
filehandler.setLevel(logging.INFO)

# create a logging format
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
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
        #print "Hanlder ", senz.attributes, senz.type, senz.receiver, senz.sender

        logger.info('senz received %s' % senz.type)
        dbh = db_handler()

        if (senz.type == 'PUT'):
            #print "Coin value :", senz.attributes["#COIN_VALUE"]
            senze = 'UNSHARE #COIN_VALUE '
            senz = str(senze) + "@%s  ^%s" % (senz.sender, clientname)
            signed_senz = sign_senz(senz)
            logger.info('read senz: %s' % signed_senz)
            self.transport.write(signed_senz)

        elif (senz.type == "SHARE"):
            flag = senz.attributes["#f"]
            if (flag == "cc"):

                format_date =datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S").replace(" ", "")
                cah = minning_algo()
                coin = cah.getCoin(senz.attributes["#S_PARA"]+""+format_date+""+senz.sender)

                senze_c = 'PUT #COIN %s #TIME %s ' % (coin,format_date)
                senz_c = str(senze_c) + "@%s  ^%s" % (senz.sender, clientname)
                signed_senzc = sign_senz(senz_c)

                dbh.addMinerDetail(senz.attributes, coin ,format_date)
                # add_detail_to_block_chain
                dbh.addCoinWiseTransaction(senz, coin, format_date)

                logger.info('Auto Excute: %s' % signed_senzc)
                self.transport.write(signed_senzc)
                self.sendTDetailsToBase(senz,coin,format_date)
                self.sendTDetailsToMiners(senz, coin, format_date)

            if (flag == "ccb"):
                logger.info('Recived New  new coin Mining COin detail::%s' % senz)
                coin = senz.attributes["#COIN"]
                format_date = senz.attributes["#FORMAT_DATE"]
                verify_state = self.verifyCoin(coin, senz.attributes["#S_PARA"], format_date,senz.attributes["#RECIVER"])
                if (verify_state == True):
                    dbh.addCoinWiseTransaction(senz, coin, format_date)
                else:
                    dbh.faildVerification(senz, coin, format_date)

            if (flag == "b_vct"):
                logger.info('Recived tranaction block verification request ::%s' % senz)
                coin = senz.attributes["#COIN"]
                coin_sender = senz.attributes["#COIN_SENDER"]

                #check coin privious sender and reciver - privous block


                #if valied update probability value
                self.updateProbabilityState(senz);

            #App to Miner Trasaction request accept
            if (flag == "ctr"):
                logger.info('Request Massage p2p Transaction :: %s' % senz)
                senze_c = 'PUT #MSG %s ' % ("ShareDone")
                senz_c = str(senze_c) + "@%s  ^%s" % (senz.sender, clientname)
                signed_senzc = sign_senz(senz_c)
                self.transport.write(signed_senzc)


            if (flag == "b_ct_ack"):
                logger.info('Transaction fail ACK:: %s' % senz) #detail should remove from db
                coin = senz.attributes["#COIN"];
                coin_sender = senz.attributes["#COIN_SENDER"]
                coin_reciver = senz.attributes["#COIN_RECIVER"]
                dbh.removeNotVerificationBlock(senz,coin,coin_sender,coin_reciver)
                dbh.faildVerification(senz, coin, "")



        # print senz.type=="DATA" and senz.receiver !=None
        elif (senz.type == "DATA" and senz.receiver != None):
            flag = senz.attributes["#f"]
            coin = senz.attributes["#COIN"]
            time = senz.attributes["#time"]
            reciver = senz.attributes["#RECIVER"];
            #print "Reciver (Data)",reciver
            if (reciver != ""):
                if (flag == "b_ct"):
                    logger.info('Doing p2p Transaction ::%s' % senz)
                    dbh.addCoinWiseTransaction(senz,coin,time)

                #recived Coin
                if(flag == "ct"):
                    if (reciver == "node1"):
                        logger.info('Recived Coin ::%s' % senz)
                        #check valitdity (final block check)
                        #if ok
                        senze_c = 'PUT #MSG %s ' % ("Transaction_Success")
                        senz_c = str(senze_c) + "@%s  ^%s" % (senz.sender, clientname)
                        signed_senzc = sign_senz(senz_c)
                        self.transport.write(signed_senzc)

                        #check coin own coin or not
                        coin_origin = self.checkCoinOrigin(coin)
                        if(coin_origin == True):
                            #delect coin and broadcast it
                            senze_c = 'DELETE #MSG %s #COIN %s ' % ("DELETE COIN" , str(coin))
                            senz_c = str(senze_c) + "@%s  @%s ^%s" % ("node3","baseNode", clientname)
                            signed_senzc = sign_senz(senz_c)
                            self.transport.write(signed_senzc)
                        else:
                            #if not own coin store folder and update block chain
                            dbh.addCoinWiseTransaction(senz, coin, time)
                            self.creatCoinScript(senz, coin)

                    else:
                        dbh.addCoinWiseTransaction(senz, coin, time)

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
        #print "post Handelr"
        logger.info("Post Handled")
        return

    def coinValueReguest(self):
        senze = 'SHARE #COIN_VALUE  #f cv @baseNode '
        senz = str(senze) + " ^%s" % (clientname)
        signed_senz = sign_senz(senz)
        logger.info('read senz: %s' % signed_senz)

        # Create a TCP/IP socket
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        # Connect the socket to the port where the server is listening
        server_address = (serverhost, serverport)
        sock.connect(server_address)
        sock.sendto(signed_senz, server_address)
        data = sock.recvfrom(4096)
        x = parse(data[0])

        # print "wada" , x.attributes["#COIN_VALUE"]
        tkMessageBox.showinfo("Message - Coin Rate  ", "Coin Value  :" + x.attributes["#COIN_VALUE"] + "$")
        sock.close()

        thread = threading.Thread(target=self.restartProdocole, args=())
        thread.start()
        return

    def restartProdocole(self):
        import miner

        try:
            miner.start()
        except:
            pass


    #send DetailTO base
    def sendTDetailsToBase(self, senz ,coin,format_date):
        senze_c = 'SHARE #M_S_ID  #f "td" #NO_COIN #RECIVER #S_ID #S_PARA #COIN #FORMAT_DATE '
        senz_c = str(senze_c) + "@%s  ^%s" % ("baseNode", clientname)
        signed_senzc = sign_senz(senz_c)
        self.transport.write(signed_senzc)

        senze_d = 'DATA #M_S_ID %s #f %s #NO_COIN %s #RECIVER %s #S_ID %s #S_PARA %s #COIN %s #FORMAT_DATE %s ' % ("M_1", "td","1",senz.sender, senz.attributes["#S_ID"],senz.attributes["#S_PARA"],coin,format_date)
        senz_d = str(senze_d) + "@%s  ^%s" % ("baseNode", clientname)
        signed_senzd = sign_senz(senz_d)
        self.transport.write(signed_senzd)

        senze_u = 'UNSHARE #M_S_ID  #f #NO_COIN #RECIVER #S_ID #S_PARA #COIN #FORMAT_DATE '
        senz_u = str(senze_u) + "@%s  ^%s" % ("mysensors", clientname)
        signed_senzu = sign_senz(senz_u)
        self.transport.write(signed_senzu)



    #send detail to M2
    def sendTDetailsToMiners(self, senz ,coin,format_date):
        '''senze_c = 'SHARE #M_S_ID  #f "ccb" #NO_COIN #RECIVER #S_ID #S_PARA #COIN #FORMAT_DATE '
        senz_c = str(senze_c) + "@%s  ^%s" % ("node3", clientname)
        signed_senzc = sign_senz(senz_c)
        self.transport.write(signed_senzc)'''

        senze_d = 'SHARE #msg %s #M_S_ID %s #f %s #NO_COIN %s #RECIVER %s #S_ID %s #S_PARA %s #COIN %s #FORMAT_DATE %s ' % (
        "new_coin_mining","M_1", "ccb", "1", senz.sender, senz.attributes["#S_ID"], senz.attributes["#S_PARA"], coin, format_date)
        senz_d = str(senze_d) + "@%s  ^%s" % ("node3", clientname)
        signed_senzd = sign_senz(senz_d)
        self.transport.write(signed_senzd)


        senze_u = 'UNSHARE #M_S_ID  #f #NO_COIN #RECIVER #S_ID #S_PARA #COIN #FORMAT_DATE '
        senz_u = str(senze_u) + "@%s  ^%s" % ("mysensors", clientname)
        signed_senzu = sign_senz(senz_u)
        self.transport.write(signed_senzu)






    # verify_coin_before_add_block_chain
    def verifyCoin(self, coin, s_para, format_date, sender):
        #print "verify_coin"
        cah = minning_algo()
        coin2 = cah.getCoin(s_para + "" + format_date + "" + sender)
        if (coin == coin2):
            return True
        else:
            return False


    #if coin is verified update probaility value
    def updateProbabilityState(self,senz):
        senz_p = 'PUT #f %s #PROB_VALUE %d ' % ("b_vct", 1)
        senz_p = str(senz_p) + "@%s  ^%s" % (senz.sender, clientname)
        signed_senz_p = sign_senz(senz_p)
        self.transport.write(signed_senz_p)

    # check coin origin
    def checkCoinOrigin(self, coin):
        #print "verify_coincoin origin check"
        dbh = db_handler()
        r_deatail = dbh.getRootBlockChainDetail(coin)
        print r_deatail

        for document in r_deatail:
            #print  document["TRANSACTION"][0]["MINER"]
            m1 = document["TRANSACTION"][0]["MINER"]

        if (m1 == "M_1"):
            dbh.delectCoinDetail(coin)
            return True
        else:
            #print "Store coin"
            return False



    #create coin script
    def creatCoinScript(self,senz , coin):
        # reducse size to coin name sting
        coin_name = blake2b(digest_size=10)
        coin_name.update(coin)
        # coin_name.hexdigest()
        #print "coin name :", coin_name.hexdigest() + ".scpp"

        if not os.path.exists('.coins'):
            # first we have to create .coins/ directory if not exists
            try:
                os.makedirs('.coins')
            except OSError:
                logger.info('coins exists')

        f = open('.coins/'+coin_name.hexdigest() + ".scpp", "w+")
        f.write("ID :%s\r\n" % coin)

        curdate = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        #print "Request TIME :", curdate;
        f.write("TIME :%s\n" % curdate + "")

        f.write("IN  :%s\n" % "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDkDbcW0W/7rrvokEf1LVELjIt5KsUwU/3Gq2x+qwpD9RJ1aQhCYsA3ds5ED0pLZzz5vpDwAFMn0zIBEbEKIhdCEocyQhPIQf26G05uXhV6NLWtZqp0wZClx6awYZG9ux9oahF39j+/OJXW4hA2NFEndyH0HF8Cvzadj7x6eZF8rwIDAQAB")
        f.write("OUT :%s\n" % "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDkMB9aRpJ+nxRvIoludQZYqlzvKLjVgH0PLjBB0TuyWXOb01mSq09Fqq/Em1RGzoUKvysxKwMuQy27fS5Bc+68pDhkZEuH3T2Okk8NH6XJgvO0ftnfE4IVHlLCHrtzUXjWBzlIHoZAmGfZ9OnFMWYrJrxEeF/apBWxZtDCFmuMgwIDAQAB")

        f.write("AMOUNT :%d\r\n" % 1)
        f.write("Signature :%s\r\n" % senz.signature)
        f.close()