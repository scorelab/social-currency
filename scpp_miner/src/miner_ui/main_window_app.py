import tkFileDialog
import tkMessageBox
import gettext
import logging
import os
import multiprocessing

from Tkinter import *
from PIL import ImageTk, Image
from handlers.senz_handler import SenzHandler
from miner_ui import view_log
from miner_ui.block_chain_data_view import BlockChainDataView
from miner_ui.data_view import DataView

_ = gettext.gettext
logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)


class MainWindowApp:

    def __init__(self, log):
        """ Remember cumulative logs, get logger """
        self.root = Tk()
        self.topBar = Frame(self.root, border=1, relief=GROOVE)
        self.sideBar = Frame(self.root, border=1, relief=GROOVE)
        self.log = log
        self.logger = logging.getLogger(self.__class__.__name__)

    def run(self):
        """ Create and run GUI """

        self.root.columnconfigure(0, weight=1)  # center component
        self.root.geometry('{}x{}'.format(500, 375))
        self.root.resizable(width=False, height=False)
        self.root.title(_('SCPP Coin Miner - M1'))

        # set the window icon
        img = PhotoImage(file='img/scpp_global.png')
        self.root.tk.call('wm', 'iconphoto', self.root._w, img)

        path = "img/scpp_miner.png"
        # Creates a Tkinter-compatible photo image, which can be used everywhere Tkinter expects an image object.
        img = ImageTk.PhotoImage(Image.open(path))

        self.topBar.grid(row=0, column=0, columnspan=2, sticky=E + W + N + S)
        self.topBar.columnconfigure(0, weight=1)

        l0 = Label(self.topBar, image=img).grid(row=1, column=0, columnspan=2, sticky=W, padx=5, pady=30)
        self.sideBar.grid(row=0, column=1, columnspan=2, sticky=E + W + N + S)
        self.sideBar.columnconfigure(0, weight=1)

        b1 = Button(self.sideBar, text=_('Check Coin Value'), command=self.getCoinValue, width=20,
                    background='green').grid(row=1, column=0, pady=5, padx=5)

        b2 = Button(self.sideBar, text=_('Mining Transaction Details'), command=self.onDatabaseLog, width=20)
        b3 = Button(self.sideBar, text=_('Block Chain Data View'), command=self.onBlockChainLog, width=20)
        b4 = Button(self.sideBar, text=_('View Log File'), command=self.onViewLog, width=20)
        b5 = Button(self.sideBar, text=_('Open Wallet'), command=self.openWallet, width=20)
        b6 = Button(self.sideBar, text=_('Define Mapping Rule'), command=self.minnerRuleDefine, width=20)
        b7 = Button(self.sideBar, text=_('Exit'), command=self.onExit, width=10, background='red')

        b2.grid(row=2, column=0, pady=5, padx=5)
        b3.grid(row=3, column=0, pady=5, padx=5)
        b4.grid(row=4, column=0, pady=5, padx=5)
        b5.grid(row=5, column=0, pady=5, padx=5)
        b6.grid(row=6, column=0, pady=5, padx=5)
        b7.grid(row=7, column=0, pady=5, padx=5, sticky=E)


        self.center(self.root)
        self.root.mainloop()

    def onExit(self):
        """ Process 'Exit' command """
        # reactor.callFromThread(reactor.stop)
        logger.info(_('client shutDown..!'))
        os._exit(0)

    def onDatabaseLog(self):
        """ Process 'View DB enrties' command """
        root1 = Tk()
        root1.title(_('Transaction Detail View'))
        root1.resizable(width=False, height=False)
        DataView(root1)

    def onBlockChainLog(self):
        """ Process 'View DB enrties' command """
        root1 = Tk()
        root1.title(_('Block Chain Data View - M1'))
        root1.resizable(width=False, height=False)
        BlockChainDataView(root1)

    def getCoinValue(self):
        """ Process 'Start' command """
        sh = SenzHandler(None)
        t = multiprocessing.Process(target=sh.coinValueReguest(), args=())
        t.start()

    def onViewLog(self):
        """ Process 'View Log' command """
        view_log.ViewLog(self.root, self.log)

    def openWallet(self):
        file = tkFileDialog.askopenfile(parent=self.root, initialdir="./.coins", mode='rb', title='SCPP Coins')
        if file != None:
            data = file.read()
            file.close()
            #print "I got %d bytes from this file." % len(data)
            tkMessageBox.showinfo("SCPP  Coin Scrypt", data)


    def minnerRuleDefine(self):
        """ Process 'Miner Rule Defined' command """
        # print 'Not Yet implement / sample DB table create'
        tkMessageBox.showinfo("Message", "Not Yet implement")

    def center(self, toplevel):
        toplevel.update_idletasks()
        w = toplevel.winfo_screenwidth()
        h = toplevel.winfo_screenheight()
        size = tuple(int(_) for _ in toplevel.geometry().split('+')[0].split('x'))
        x = w / 2 - size[0] / 2
        y = h / 2 - size[1] / 2
        toplevel.geometry("%dx%d+%d+%d" % (size + (x, y)))
