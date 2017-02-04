import tkSimpleDialog
import tkMessageBox
import Tkinter
import ProgressBarView
import ScrolledText
import logging
import ThreadsConnector
import Queue
import gettext
import sys
import scpp_switch

from twisted.internet import reactor

_ = gettext.gettext


class ActionWindow(tkSimpleDialog.Dialog):
    def __init__(self, parent, title, text):
        """ Create dialog, remember interface objects """
        # Postpone parent's init bacause it starts modal dialog immediately
        # tkSimpleDialog.Dialog.__init__(self, parent, title)
        self.aw_parent = parent
        self.aw_title = title
        self.aw_text = text
        self.log = logging.getLogger(self.__class__.__name__)

    def setConnector(self, conn):
        """ Remember a thread connector """
        self.conn = conn

    def setProgressBar(self, progress):
        """ Remember a progress bar controller """
        self.progress_ctrl = progress

    def go(self):
        """ Create and start dialog """
        tkSimpleDialog.Dialog.__init__(self, self.aw_parent, self.aw_title)

    def wait_window(self, wnd):
        """ Customize appearence of window, setup periodic call and call parent's wait_window """
        self.fixWindowLayout()
        self.aw_alarm = self.after(100, self.periodicCheckForMessages)
        tkSimpleDialog.Dialog.wait_window(self, wnd)

    def destroy(self):
        """ Terminate periodic process and call parent's destroy """

        self.after_cancel(self.aw_alarm)
        tkSimpleDialog.Dialog.destroy(self)

    def periodicCheckForMessages(self):
        """ Check if there are new messages and dispatch them """
        try:
            while 1:
                (code, item) = self.conn.get_message()
                if code == ThreadsConnector.MESSAGE_LOG:
                    self.onLogMessage(item)
                elif code == ThreadsConnector.MESSAGE_PROGRESS:
                    self.onProgress()
                elif code in (ThreadsConnector.MESSAGE_EXIT_CANCEL, ThreadsConnector.MESSAGE_EXIT_ERROR,
                              ThreadsConnector.MESSAGE_EXIT_OK):
                    self.onCalculationsExitMessage(item)
                else:
                    self.log.error('Unknown ActionWindow message: %s, %s' % (code, item))
        except Queue.Empty:
            pass
        self.aw_alarm = self.after(100, self.periodicCheckForMessages)

    def onLogMessage(self, text):
        """ Display logs message """
        w = self.logwnd
        w.configure(state=Tkinter.NORMAL)
        w.insert(Tkinter.END, text)
        w.insert(Tkinter.END, "\n")
        w.see(Tkinter.END)
        w.configure(state=Tkinter.DISABLED)

    def onCalculationsExitMessage(self, text):
        """ Visualize that calculations are finished """
        self.status_label.configure(text=text)
        self.progress_bar.updateProgress(100, 100)
        self.button_cancel.configure(state=Tkinter.DISABLED)
        self.button_ok.configure(state=Tkinter.NORMAL)
        self.bind('<Escape>', self.cancel)

    def onProgress(self):
        """ Visualize progress of calculations """
        (cur, limit) = self.progress_ctrl.get()
        self.progress_bar.updateProgress(cur, limit)

    def body(self, master):
        """ Pack body of window """
        self.packText(master)
        self.packProgressBar(master)
        self.packStatusText(master)
        self.packLogWindow(master)

    def packText(self, master):
        """ Pack text message """
        Tkinter.Label(master, text=self.aw_text, anchor=Tkinter.NW, justify=Tkinter.LEFT).pack(fill=Tkinter.X)

    def packProgressBar(self, master):
        """ Pack progress bar """
        self.progress_bar = ProgressBarView.ProgressBarView(master)
        self.progress_bar.pack(fill=Tkinter.X)

    def packLogWindow(self, master):
        """ Pack logs window """
        self.logwnd = ScrolledText.ScrolledText(master, width=60, height=12, state=Tkinter.DISABLED)
        self.logwnd.pack(fill=Tkinter.BOTH, expand=1)

    def packStatusText(self, master):
        """ Pack a status text """
        self.status_label = Tkinter.Label(master, text=_('Task is in progress'), anchor=Tkinter.NW,justify=Tkinter.LEFT)
        self.status_label.pack(fill=Tkinter.X)

    def buttonbox(self):
        """ Pack buttons """
        tkSimpleDialog.Dialog.buttonbox(self)
        # Get buttons by accessing children of last packed frame
        (self.button_ok, self.button_cancel) = self.pack_slaves()[-1].pack_slaves()
        self.button_ok.configure(state=Tkinter.DISABLED)
        self.bind('<Escape>', lambda e: 'break')



    def fixWindowLayout(self):
        """ Make window content resizable, set minimal sizes of window to avoid disappearing of GUI elements """
        # [0] is a wrapping frame for the window body
        self.pack_slaves()[0].pack_configure(fill=Tkinter.BOTH, expand=1)
        self.update_idletasks()
        reqheight = self.winfo_reqheight()
        reqwidth = self.button_ok.winfo_reqwidth() + self.button_cancel.winfo_reqwidth()
        self.minsize(reqwidth + 10, reqheight + 10)

    def ok(self, event=None):
        """ Handle 'ok' button. Can be called only after end of calculations """
        if not (self.conn.isRunning()):
            scpp_switch.stop_switch()
            tkSimpleDialog.Dialog.ok(self)
            sys.exit()


    def cancel(self, event=None):
        """ Handle 'cancel' button and window close """
        if not (self.conn.isRunning()):
            tkSimpleDialog.Dialog.cancel(self)
            return  # return
        if tkMessageBox.askyesno(title=_('Cancelling operation'), message=_('Shut Down Switch?'), parent=self):
            self.conn.cancel()
            tkSimpleDialog.Dialog.cancel(self)
            scpp_switch.stop_switch()
