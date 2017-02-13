from Crypto.Hash import SHA256
from Crypto.PublicKey import RSA
from Crypto.Signature import PKCS1_v1_5
from base64 import b64encode

import os.path
import logging

from config.config import clientname

logger = logging.getLogger(__name__)
logger.setLevel(logging.INFO)

filehandler = logging.FileHandler('logs/miner.log')
filehandler.setLevel(logging.INFO)

# create a logging format
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - \
                                                            %(message)s')
filehandler.setFormatter(formatter)
# add the handlers to the logger
logger.addHandler(filehandler)


def init_keys():
    """
    Initilize keys from here, device name exists in the 'name' file. Verify
    weather name file exists. If not exits we have to generate keys(public key
    and private key). We are storing keys in .keys/ directory in project root

    We are doing
        1. Create .keys directoy and name file
        2. Generate rsa keys
        3. Save ras keys in .keys directory
    """

    def init_dirs(senzy_name):
        """
        Create '.keys' directory and 'name' file if not exits. We have to write
        senzy name in name file.

        Args:
            senzy_name - name of the senz client(username)
        """
        if not os.path.exists('.keys/name'):
            # first we have to create .keys/ directory if not exists
            try:
                os.makedirs('.keys')
            except OSError:
                logger.info('keys exists')

            # create name file from here
            senzy_name_file = open('.keys/name', 'w')
            senzy_name_file.write(senzy_name)
            senzy_name_file.close()

            # test
            # generate keys
            key_pair = RSA.generate(1024, e=65537)
            public_key = key_pair.publickey().exportKey("PEM")
            private_key = key_pair.exportKey("PEM")

            # save keys in pem file
            save_key('publicKey.pem', public_key)
            save_key('privateKey.pem', private_key)
            # test over
        else:
            logger.info('keys exists')

    def save_key(file_name, key):
        """
        Save key in .pem file. We are saving both public key and private key
        from here. Folloing are the name and location of the keys
            1. public key - .keys/publicKey.pem
            2. private key - .keys/privateKey.pem
        """
        key_file = open('.keys/' + file_name, 'w')
        key_file.write(key)
        key_file.close()

    # TODO read senzy name from config file
    senzy_name = clientname  # 'switch'
    init_dirs(senzy_name)
    '''
    # generate keys
    key_pair = RSA.generate(1024, e=65537)
    public_key = key_pair.publickey().exportKey("PEM")
    private_key = key_pair.exportKey("PEM")

    # save keys in pem file
    save_key('publicKey.pem', public_key)
    save_key('privateKey.pem', private_key)
    '''


def get_pubkey():
    """
    Reads a public key from the file. Public key stored in .keys/publicKey.pem
    file in project roor directory

        Returns:
            pubkey - Base64 encoded public key
    """
    pubkey = open('.keys/publicKey.pem', "r").read()

    return b64encode(pubkey)


def sign_senz(senz):
    """
    Digitally sing the senz message. We have to append the digital signatutre
    of the message to senz paylod before sending the senz. Senz message would
    be comes like below

        #SHARE
            #msg #time <time>
        @receiver
        ^sender

    We have to caculate digital signature of the message and append the
    signature to the end of the senz. Finalized senz message would be looks
    like below

        #SHARE
            #msg #time <time>
        @receiver
        ^sender <digital signature>

    Args:
        senz: Senz message

    Returns:
        digitally signed senz message
    """
    # load private key
    key = open('.keys/privateKey.pem', "r").read()
    rsakey = RSA.importKey(key)

    # sign senz
    signer = PKCS1_v1_5.new(rsakey)
    digest = SHA256.new("".join(senz.split()))
    signature = signer.sign(digest)
    signed_senz = "%s %s" % (senz, b64encode(signature))

    return signed_senz
