import logging
import ThreadsConnector


class LoggerToWindowHandler(logging.Handler):
    """ Provide a logging handler """

    def __init__(self, conn):
        """ Create handler, remember threads connector """
        logging.Handler.__init__(self)
        self.conn = conn

    def emit(self, record):
        """ Process a logs message """
        self.conn.put_message([ThreadsConnector.MESSAGE_LOG, self.format(record)])
        self.conn.ack()


class LoggerToWindow:
    def __init__(self, conn, fmt='%(asctime)s %(message)s', datefmt='%H:%M:%S'):
        """ Create handler with formatter, remember threads connector """
        self.h = LoggerToWindowHandler(conn)
        self.h.setFormatter(logging.Formatter(fmt, datefmt))

    def attach(self):
        """ Attach handler to the logging system """
        logging.getLogger().addHandler(self.h)

    def detach(self):
        """ Detach logger from the logging system """
        logging.getLogger().removeHandler(self.h)
