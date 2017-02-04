###############################################################################
##
##  Senze Crypto Library for My Senosr Server/Client v0.01
##  @Copyright 2014 MySensors Research Project
##  SCoRe Lab (www.scorelab.org)
##  University of Colombo School of Computing
##
##  Licensed under the Apache License, Version 2.0 (the "License");
##  you may not use this file except in compliance with the License.
##  You may obtain a copy of the License at
##
##      http://www.apache.org/licenses/LICENSE-2.0
##
##  Unless required by applicable law or agreed to in writing, software
##  distributed under the License is distributed on an "AS IS" BASIS,
##  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
##  See the License for the specific language governing permissions and
##  limitations under the License.
##
###############################################################################
import time
import os.path

from Crypto.Hash import SHA256
from Crypto.Hash import SHA
from Crypto.PublicKey import RSA
from Crypto.Cipher import AES
from base64 import b64encode, b64decode
from Crypto.Cipher import PKCS1_OAEP
from Crypto import Random


class myCrypto:
    user = ""
    pubKeyLoc = ""
    privKeyLoc = ""
    key = ""
    bs = 32

    def __init__(self, name):
        self.user = name
        # Set the public and private key locations
        self.pubKeyLoc = "." + name + "PubKey.pem"
        self.privKeyLoc = "." + name + "PrivKey.pem"

    def generateAES(self, pin):
        '''
    Generate 256 bits AES key.
    param: pin the shared secret
    '''
        try:
            self.bs = 32
            digest = SHA256.new(pin).digest()
            self.key = digest
            return True
        except:
            return False

    def pad(self, s):
        return s + (self.bs - len(s) % self.bs) * chr(self.bs - len(s) % self.bs)

    def unpad(self, s):
        return s[:-ord(s[len(s) - 1:])]

    def encrypt(self, raw):
        raw = self.pad(raw)
        # iv = Random.new().read(AES.block_size)
        # obj = AES.new('This is a key123', AES.MODE_CBC, 'This is an IV456')
        cipher = AES.new(self.key, AES.MODE_ECB)
        return b64encode(cipher.encrypt(raw))

    def decrypt(self, enc):
        try:
            enc = b64decode(enc)
            # iv = enc[:AES.block_size]
            # cipher = AES.new(self.key,AES.MODE_OFB)
            cipher = AES.new(self.key, AES.MODE_ECB)
            return self.unpad(cipher.decrypt(enc))
        except:
            return False

    def generateRSA(self, bits):
        '''
    Generate an RSA keypair with an exponent of 65537 in PEM format
    param: bits The key length in bits
    Save private key and public key in files
    openssl pkcs8 -topk8 -inform PEM -outform PEM -in .mysensorsPrivKey.pem -out private.pem -nocrypt
    '''
        newKey = RSA.generate(bits, e=65537)
        publicKey = newKey.publickey().exportKey("PEM")
        privateKey = newKey.exportKey("PEM")

        try:
            f = open(self.privKeyLoc, 'w')
            f.write(privateKey)
            f.close()

            f = open(self.pubKeyLoc, 'w')
            f.write(publicKey)
            f.close()
            return True

        except:
            return False

    def signSENZE(self, senze):
        from Crypto.Signature import PKCS1_v1_5
        '''
    param: senze to be signed
    return: senze with base64 encoded signature
    '''
        key = open(self.privKeyLoc, "r").read()
        rsakey = RSA.importKey(key)
        t = time.time()
        senze = '%s #time %s ^%s' % (senze, t, self.user)
        signer = PKCS1_v1_5.new(rsakey)
        digest = SHA256.new("".join(senze.split()))
        sign = signer.sign(digest)
        senze = '%s %s' % (senze, b64encode(sign))
        return senze

    def signData(self, data):
        from Crypto.Signature import PKCS1_v1_5
        '''
    param: package Data to be signed
    return: base64 encoded signature
    '''
        key = open(self.privKeyLoc, "r").read()
        rsakey = RSA.importKey(key)

        signer = PKCS1_v1_5.new(rsakey)
        digest = SHA256.new(data)
        sign = signer.sign(digest)
        return b64encode(sign)

    def verifySENZE(self, query, publicKey):
        from Crypto.Signature import PKCS1_v1_5
        '''
    Verifies with a public key from whom the data came that it was indeed
    signed by their private key
    param: public_key
    param: senze
    param: signature String signature to be verified
    return: Boolean. True if the signaetture is valid; False otherwise.
    '''
        rsakey = RSA.importKey(b64decode(publicKey))
        signer = PKCS1_v1_5.new(rsakey)
        digest = SHA256.new(query.getSENZE())
        # Assumes the data is base64 encoded to begin with
        if signer.verify(digest, b64decode(query.getSignature())):
            return True
        else:
            return False

    def verifySign(self, publicKey, signature, data):
        from Crypto.Signature import PKCS1_v1_5
        '''
    Verifies with a public key from whom the data came that it was indeed
    signed by their private key
    param: public_key
    param: signature String signature to be verified
    param: data
    return: Boolean. True if the signature is valid; False otherwise.
    '''
        rsakey = RSA.importKey(b64decode(publicKey))
        signer = PKCS1_v1_5.new(rsakey)
        digest = SHA256.new(data)
        # Assumes the data is base64 encoded to begin with
        if signer.verify(digest, b64decode(signature)):
            return True
        else:
            return False

    def loadRSAPubKey(self):
        '''
    Reads a public key from the file
    return: Base64 encoded public key
    '''
        publicKey = ""
        # if os.path.isfile(pubKeyLoc):
        publicKey = open(self.pubKeyLoc, "r").read()
        return b64encode(publicKey)
        # else:
        #   return publicKey

    def saveRSAPubKey(self, pubkey):
        '''
    Saves a public key
    param: public key
    '''
        try:
            # if not os.path.isfile(publicKeyLoc):
            f = open(self.pubKeyLoc, 'w')
            f.write(b64decode(pubkey))
            f.close()
            return True
        except:
            return False

    def encryptRSA(self, message):
        '''
    param: message String t o be encrypted
    return base64 encoded encrypted string
    '''
        # h = SHA.new(message)
        key = open(self.pubKeyLoc, "r").read()
        rsakey = RSA.importKey(key)
        #    rsakey = PKCS1_v1_5.new(rsakey)
        rsakey = PKCS1_OAEP.new(rsakey)
        # encrypted = rsakey.encrypt(b64encode(message))
        encrypted = rsakey.encrypt(message)
        return b64encode(encrypted)
        # return encrypted.encode('base64')

    def decryptRSA(self, package):
        '''
    param: package String to be decrypted
    return decrypted string
    '''
        key = open(self.privKeyLoc, "r").read()
        rsakey = RSA.importKey(key)

        '''
    dsize = SHA.digest_size
    sentinel = Random.new().read(15+dsize)      # Let's assume that average data length is 15
    #print sentinel
    cipher = PKCS1_v1_5.new(rsakey)
    message = cipher.decrypt(b64decode(package),sentinel)
    '''
        cipher = PKCS1_OAEP.new(rsakey)
        message = cipher.decrypt(b64decode(package))
        # return b64decode(message)
        return message


'''
test=myCrypto("mysensors")
test.generateAES("1234hello")
enc=test.encrypt("Hello123")
print enc
dec=test.decrypt(enc)
print dec

cry=myCrypto("mysensors")
if not os.path.isfile(cry.pubKeyLoc):
   cry.generateRSA(1024)
publicKey=cry.loadRSAPubKey()
#print publicKey

#data="LOGIN #name kasun #key 1234"
data="Hello Kasun"
signature=cry.signData(data);
print signature

#data="LOGIN #name kasun #key 1234"
if cry.verifySign(publicKey,signature, data):
   print "VERIFIED"
else:
   print "FAILED"

cry=myCrypto("mysensors")
data="Hello Kasun Hello Kasun Hello Kasun"
enc=cry.encryptRSA(data)
print enc
print "-----"
plain=cry.decryptRSA(enc)
print plain
'''
