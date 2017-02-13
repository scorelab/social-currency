import sys
import os
import logging

from models.senz import *

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)
filehandler = logging.FileHandler('logs/stock_exchange.logs')
filehandler.setLevel(logging.INFO)

# create a logging format
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
filehandler.setFormatter(formatter)
# add the handlers to the logger
logger.addHandler(filehandler)


def parse(message):
    """
    Parse incoming senz messages and create Senz objects. Senz message would be
    looks like below

        SHARE
            #bal
            #nm
            #nic
            #acc <acc>
            #time <time>
        @agent1
        ^mysensors <sginature>

    We have to valid senz messages from here as well. If invalid type of
    message receives we have to raise and exception

    Args:
        message - senz message

    Returns:
        Senz object
    """
    senz = Senz()
    tokens = message.split()

    # senz type comes in first place
    senz.type = tokens.pop(0)

    # senz signature comes at the end
    senz.signature = tokens.pop()

    i = 0
    while i < len(tokens):
        token = tokens[i]
        if token.startswith('@'):
            # this is receiver
            senz.receiver = token[1:]
        elif token.startswith('^'):
            # this is sender
            senz.sender = token[1:]
        elif token.startswith('#'):
            if tokens[i + 1].startswith('#') or tokens[i + 1].startswith('@') \
               or tokens[i + 1].startswith('^'):
                senz.attributes[token] = ''
            else:
                senz.attributes[token] = tokens[i + 1]
                i += 2
                continue

        i += 1
    logger.info(senz.type)
    logger.info(senz.signature)
    logger.info(senz.sender)
    logger.info(senz.receiver)
    logger.info(senz.attributes)

    '''print senz.type
    print senz.signature
    print senz.sender
    print senz.receiver
    print senz.attributes'''

    return senz

'''
parse('SHARE #bal #trans #nic 234 #time tim @agent ^myz <sig>')
parse('SHARE #bal #trans #nic #acc 4345234 #time tim @agent ^myz <sig>')
--------------------------------------------------
parse('PUT #COIN_VALUE  value  @sender ^myz <sig>')
'''
