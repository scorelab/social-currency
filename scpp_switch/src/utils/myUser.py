###############################################################################
##
##  User Library for My Senosr Server/Client v0.01
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

import sys
import re
import os.path
import mmap
import hashlib

from pymongo import MongoClient
from utils.myCrypto import *


class myUser:
    database = ""
    usrDoc = ""
    """ All database related operations are here
        putUser    - Check the availability of a user name and create it
        delUser    - Remove the given user
        login      - Read the pin/public key and handle the login
        Share      - Sharing the sensors
        UnShare    - remove the share

   """

    def __init__(self, db, name):
        # Set the pointers to the database and user document
        self.database = db
        self.name = name
        doc = self.database.find_one({"name": self.name})
        if (doc):
            self.usrDoc = doc
        else:
            doc = ""

    def addUser(self, name, senze, pubkey, signature):
        # find user with same useraname and pubkey
        doc = self.database.find_one({"name": name, "pubkey": pubkey})
        if (doc):
            # if user name is already taken
            # means send registartion senz by already registerd user
            return 'REGISTERED'

        # find user with same username
        doc = self.database.find_one({"name": name})
        if (doc):
            # if user name is already taken
            return 'FAIL'
        else:
            # The system support public key based authentication.
            # It saves public key
            user = {"name": name, "senze": senze, "pubkey": pubkey, "signature": signature}
            post_id = self.database.insert(user)
            return 'DONE'

    def delUser(self, name, pubkey):
        # Owners can remove himself
        doc = self.database.find_one({"name": name, "pubkey": pubkey})
        print name
        print pubkey
        if not (doc):
            # if the given user is not available
            return False
        else:
            post_id = self.database.remove({"name": name, "pubkey": pubkey})
            return True

    def findUsers(self, u):
        friends = []
        users = u.split(',')
        for user in users:
            doc = self.database.find_one({"name": user})
            if doc:
                if user not in friends:
                    friends.append(user)
            else:
                doc = ""
        return str(','.join(friends))

    def login(self, key, sig, server):
        # doc=db.find_one({"name":self.name,"skey":key})
        if (self.usrDoc):
            # PIN is compared with hash of the key
            if 'skey' in self.usrDoc:
                s = hashlib.sha1()
                s.update(key)
                key = b64encode(s.digest())
                if self.usrDoc['skey'] == key:
                    return True
            # Hash key is sent
            elif 'hkey' in self.usrDoc:
                hkey = self.usrDoc['hkey']
                s = hashlib.sha1()
                s.update(hkey + sig)
                tkey = b64encode(s.digest())
                if tkey == key:
                    return True
            # Signature will be verified with the public key
            elif 'publickey' in self.usrDoc:
                cry = myCrypto(server)
                if cry.verifySign(self.usrDoc['publickey'], sig, key):
                    return True
        return False

    def loadPublicKey(self):
        if (self.usrDoc):
            if 'pubkey' in self.usrDoc:
                s = self.usrDoc['pubkey']
                return str(s)
        return ''

    def loadFriends(self, sensor):
        friends = []
        if (self.usrDoc):
            for name in self.usrDoc:
                sensors = self.usrDoc[name]
                # print type(sensors)
                if type(sensors) is list:
                    if sensor in sensors:
                        friends.append(name)
        return str(','.join(friends))

    def loadData(self, name):
        if (self.usrDoc):
            if name in self.usrDoc:
                s = self.usrDoc[name]
                return str(','.join(s))
        return ''

    def logout(self):
        # This will call when user logout
        if (self.usrDoc):
            self.usrDoc = ""
            return True
        else:
            return False

    """
   Following function will add recipient names to
   the sensor array in the user dictionary.
   It also add the sensor name to the recipient array in
   the recipient dictionary.
   """

    def share(self, recipient, sensors):
        # User should loged
        if not (self.usrDoc): return False
        # Recipient should be available
        doc = self.database.find_one({"name": recipient})
        if not doc: return False

        for sensor in sensors:
            # Check the sensor is already in the shared list
            if not self.isShare(recipient, [sensor]):
                # check that the sensor was shared for someone else
                if sensor in self.usrDoc.keys():
                    self.usrDoc[sensor].append(recipient)
                else:
                    self.usrDoc[sensor] = [recipient]

            # Tag recipient document
            # Check that user was shared anything else
            if self.name in doc.keys():
                if not sensor in doc[self.name]:
                    doc[self.name].append(sensor)
            else:
                doc[self.name] = [sensor]

        post_id = self.database.save(doc)
        post_id = self.database.save(self.usrDoc)
        return True

    """
   Following function will remove recipient names from
   the sensor array in the user dictionary.
   It also remove the sensor name from the recipient array in
   the recipient dictionary
   """

    def unShare(self, recipient, sensors):
        # User must be loged
        if not (self.usrDoc): return False
        # Recipient should available
        doc = self.database.find_one({"name": recipient})
        if not doc: return False

        for sensor in sensors:
            # Check that the logged user was shared a sensor
            # If so remove recipient name from the senor dictionary
            if self.isShare(recipient, [sensor]):
                self.usrDoc[sensor].remove(recipient)

                # Remove shared tag from recipient document
                # Check that user was shared anything else
                if self.name in doc.keys():
                    if sensor in doc[self.name]:
                        doc[self.name].remove(sensor)

            # Check that the recipient was shared a sensor
            # If so remove the sensor name from the recipient dictionary
            if self.isAllow(recipient, [sensor]):
                self.usrDoc[recipient].remove(sensor)

                # Remove shared tag from recipient document
                # Check that user was shared anything else
                if sensor in doc.keys():
                    if self.name in doc[sensor]:
                        doc[sensor].remove(self.name)

        post_id = self.database.save(doc)
        post_id = self.database.save(self.usrDoc)
        return True

    # GET Method Check
    def isShare(self, recipient, sensors):
        #print self.usrDoc
        if not (self.usrDoc): return False
        for sensor in sensors:
            if not sensor in self.usrDoc.keys(): return False
            if not recipient in self.usrDoc[sensor]: return False
        return True

    # data method Check
    def isAllow(self, sender, sensors):
        if not (self.usrDoc): return False
        if not sender in self.usrDoc.keys(): return False
        for sensor in sensors:
            if not sensor in self.usrDoc[sender]: return False
        return True

    def countDocs(self):
        if not (self.usrDoc): return False
        return self.database.find().count()


'''
#Create connection to the Mongo DB
client = MongoClient('localhost', 27017)
#Creating the database

db = client['mysensors']
collection = db['users']
# Access the user collection from the mysensors database
usrDB = db.users
usr=myUser(usrDB,'d2')
#print usr.countDocs()
#f=usr.loadFriends('tp')
#print f
f=usr.findUsers('d2,d1,kasun,device1,nimal,d3')
print f

rep=usr.loadData('name')
pub=usr.loadPublicKey()
print pub
print rep
'''
