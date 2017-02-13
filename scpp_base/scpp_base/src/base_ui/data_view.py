from Tkinter import Frame, N, S, W, E, Button
from ttk import Treeview

from db.db_handler import db_handler


class DataView(Frame):
    def __init__(self, parent):
        self.root = parent
        Frame.__init__(self, parent)
        self.CreateUI()
        self.LoadTable()
        self.grid(sticky=(N, S, W, E))
        parent.grid_rowconfigure(0, weight=1)
        parent.grid_columnconfigure(0, weight=1)

    def CreateUI(self):
        tv = Treeview(self)
        tv['columns'] = ('no_of_coin', 's_id', 'm_s_id')
        tv.heading("#0", text='Date', anchor='c')
        tv.column("#0", anchor="c")
        tv.heading('no_of_coin', text='No Of Coin')
        tv.column('no_of_coin', anchor='center', width=100)
        tv.heading('s_id', text='Service ID')
        tv.column('s_id', anchor='center', width=100)
        tv.heading('m_s_id', text='Miner ID')
        tv.column('m_s_id', anchor='center', width=100)
        tv.grid(sticky=(N, S, W, E))
        self.treeview = tv
        self.grid_rowconfigure(0, weight=1)
        self.grid_columnconfigure(0, weight=1)

        btn = Button(self, text=('Back'), command=self.back, width=10, background='red')
        btn.grid(sticky=(S + E))

    def LoadTable(self):
        dbh = db_handler()
        t_deatail = dbh.getAllTransactionDetails()
        self.rows = t_deatail.count()
        #print t_deatail, self.rows
        for document in t_deatail:
            #only mining details are printed in table.no p2p transactions
            #print document["NO_COIN"], document["S_ID"], document["TRANSACTION"][0]["DATE"], document["TRANSACTION"][0]["MINER"]
            self.treeview.insert('', 'end', text=document["TRANSACTION"][0]["DATE"].date(),
                                 values=(document["NO_COIN"], document["S_ID"], document["TRANSACTION"][0]["MINER"]))

    def back(self):
        self.root.destroy()
