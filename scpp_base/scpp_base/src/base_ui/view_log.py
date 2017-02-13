import Tkinter
import tkSimpleDialog
import ScrolledText
import gettext

_ = gettext.gettext


class ViewLog(tkSimpleDialog.Dialog):
    """ Display logs messages of a program """

    def __init__(self, parent, log):
        """ Create and display window. Log is CumulativeLogger. """
        self.log = log
        tkSimpleDialog.Dialog.__init__(self, parent, _('Log Entries'))

    def body(self, master):
        """ Create dialog body """
        master.pack_configure(fill=Tkinter.BOTH, expand=1)
        t = ScrolledText.ScrolledText(master, width=60, height=37)
        t.insert(Tkinter.END, self.log.getText())
        t.configure(state=Tkinter.DISABLED)
        t.see(Tkinter.END)
        t.pack(fill=Tkinter.BOTH)

    def buttonbox(self):
        """ Create custom buttons """
        w = Tkinter.Button(self, text=_('Back'), width=10, command=self.cancel, default=Tkinter.ACTIVE,
                           background='red')
        w.pack(side=Tkinter.RIGHT)
