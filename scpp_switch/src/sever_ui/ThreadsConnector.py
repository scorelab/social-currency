# ThreadsConnector controls communications of GUI and calculations threads
# Class ThreadsConnector can be splitted on several classes.
# But I think that it is not required.

import logging
import threading
import ActionWindow
import LoggerToWindow
import Queue
import sys
import traceback
import gettext
import ProgressBarWithAck
import scpp_switch

_ = gettext.gettext

MESSAGE_LOG = 'logs'  # Message to logger
MESSAGE_EXIT_CANCEL = 'cancel'  # Calculations are cancelled
MESSAGE_EXIT_OK = 'ok'  # .. succesfully finished
MESSAGE_EXIT_ERROR = 'error'  # .. finished with error
MESSAGE_PROGRESS = 'progress'  # Progress is updated

# Texts are private, do not access them from other classes
TEXT_EXIT_CANCEL = _('Operation has been cancelled by user')
TEXT_EXIT_ERROR = _('Operation has been terminated on error')
TEXT_EXIT_OK = _('Operation has been finished succesfully')


class ThreadsConnectorTerminateException(Exception):
    pass


class ThreadsConnector:
    def __init__(self):
        """ Initialize object """
        self.messages = Queue.Queue()
        self.running = 1
        self.silent_ack = 0

    def put_message(self, msg):
        """ Just a wrapper to Queue.put_nowait() """
        self.messages.put_nowait(msg)

    def get_message(self):
        """ Just a wrapper to Queue.get_nowait() """
        return self.messages.get_nowait()

    def cancel(self):
        """ Terminate calculations thread """
        self.running = 0

    def isRunning(self):
        """ Return true value if calculations are running now """
        return self.running

    def ack(self):
        """ Monitor activeness of calculations thread """
        if not (self.running):
            # Raise exception only once and
            # only if current thread is the cakcukations thread.
            # Check for thread is required because logger calls ack(),
            # and gui thread can call caller before calculations thread.
            if not (self.silent_ack):
                if self.calc_thread == threading.currentThread():
                    self.silent_ack = 1
                    raise ThreadsConnectorTerminateException(TEXT_EXIT_CANCEL)

    def start(self, group, target, name, args, kw):
        """ Create and start calculations thread """
        self.calc_thread = threading.Thread(group, self.wrap_calc, name, (target, args, kw))
        self.calc_thread.start()

    def wrap_calc(self, target, args, kw):
        """ Function start() continues, but now in calculations thread """
        log = logging.getLogger(self.__class__.__name__)
        try:
            kw['connector'] = self
            kw['progress'] = self.progress
            target(*args, **kw)
            log.info(TEXT_EXIT_OK)
            self.put_message([MESSAGE_EXIT_OK, TEXT_EXIT_OK])
        except ThreadsConnectorTerminateException:
            # Cancelled
            log.info(TEXT_EXIT_CANCEL)
            self.put_message([MESSAGE_EXIT_CANCEL, TEXT_EXIT_CANCEL])
        except:
            # All other exception
            log.info(TEXT_EXIT_ERROR)
            (exc_type, exc_value, exc_traceback) = sys.exc_info()
            e_seq = traceback.format_exception(exc_type, exc_value, exc_traceback)
            e_text = ''.join(e_seq)
            log.error(e_text)
            self.put_message([MESSAGE_EXIT_ERROR, TEXT_EXIT_ERROR])
        self.running = 0

    def runInGui(self, wnd, conn, group=None, target=None, name=None, args=(), kwargs={}):
        """ Run calculations, using window as a progress indicator
        "wnd" is an object of class ''ActionWindow
        all other parameters are passed to threading.Thread
    """
        cl = LoggerToWindow.LoggerToWindow(conn)
        cl.attach()
        wnd.setConnector(self)
        self.progress = ProgressBarWithAck.ProgressBarWithAck(self)
        wnd.setProgressBar(self.progress)
        # start function call
        self.start(group, target, name, args, kwargs)

        wnd.go()
        cl.detach()
