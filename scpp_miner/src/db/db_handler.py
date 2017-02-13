import datetime

from pymongo import MongoClient


class db_handler:
    def __init__(self):
        client = MongoClient('localhost', 27017)
        self.db = client.scpp_miner
        self.collection = self.db.miner_detail

    def addMinerDetail(self, quarry, coin, formatedate):
        #print quarry
        miner_object = {"M_S_ID": "M_1", "S_ID": int(quarry["#S_ID"]), "S_PARA": quarry["#S_PARA"],"COIN": str(coin), "NO_COIN": int(1), "date": formatedate}
        #print miner_object
        self.collection.insert(miner_object)
        return 'ADD DATA SUCCESSFULLY'

    '''
    return all miner_detail table data
   '''
    def getAllMinerDetails(self):
        md = self.db.miner_detail.find()
        return md

    #get all block chain details
    def getAllBlockChainDetail(self):
        bc = self.db.block_chain.find()
        return bc

    #get spcific block
    def getRootBlockChainDetail(self,coin):
        bc = self.db.block_chain.find({"_id": str(coin)})
        return bc

    #Delete specific block
    def delectCoinDetail(self, coin):
        dc = self.db.block_chain.delete_one({"_id": str(coin)})
        return dc

    # added new method  create block chain_structure
    def addCoinWiseTransaction(self, senz, coin, format_date):
        self.collection = self.db.block_chain
        coinValexists = self.collection.find({"_id": str(coin)}).count()
        #print('coin exists : ', coinValexists)
        if (coinValexists > 0):
            #print('coin hash exists')
            newTransaction = {"$push": {"TRANSACTION": {"SENDER": senz.attributes["#SENDER"],
                                                        "RECIVER": senz.attributes["#RECIVER"],
                                                        "T_NO_COIN": int(1),
                                                        "DATE": datetime.datetime.utcnow()
                                                        }}}
            self.collection.update({"_id": str(coin)}, newTransaction)
        else:
            flag = senz.attributes["#f"];
            print flag
            if (flag == "ccb"):
                #print('new coin mined othir minner')
                root = {"_id": str(coin)
                    , "S_ID": int(senz.attributes["#S_ID"]), "S_PARA": senz.attributes["#S_PARA"],
                        "FORMAT_DATE": format_date,
                        "NO_COIN": int(1),
                        "TRANSACTION": [{"MINER": senz.attributes["#M_S_ID"],
                                         "RECIVER": senz.attributes["#RECIVER"],
                                         "T_NO_COIN": int(1),
                                         "DATE": datetime.datetime.utcnow()
                                         }
                                        ]
                        }
                self.collection.insert(root)
            else:
                #print('new coin mined')
                root = {"_id": str(coin)
                    , "S_ID": int(senz.attributes["#S_ID"]), "S_PARA": senz.attributes["#S_PARA"],
                        "FORMAT_DATE": format_date,
                        "NO_COIN": int(1),
                        "TRANSACTION": [{"MINER": "M_1",
                                         "RECIVER": senz.sender,
                                         "T_NO_COIN": int(1),
                                         "DATE": datetime.datetime.utcnow()
                                         }
                                        ]
                        }
                self.collection.insert(root)

        return 'DONE'



    # added verification failed trasaction hear
    def faildVerification(self, senz, coin, format_date):
        self.collection = self.db.faild_chain
        coinValexists = self.collection.find({"_id": str(coin)}).count()
        #print('coin exists : ', coinValexists)
        if (coinValexists > 0):
            #print('coin hash exists')
            newTransaction = {"$push": {"TRANSACTION": {"SENDER": senz.attributes["#SENDER"],
                                                        "RECIVER": senz.attributes["#RECIVER"],
                                                        "T_NO_COIN": int(1),
                                                        "DATE": datetime.datetime.utcnow()
                                                        }}}
            self.collection.update({"_id": str(coin)}, newTransaction)
        else:
            flag = senz.attributes["#f"];
            print flag
            if (flag == "ccb"):
                #print('new coin mined othir miner')
                root = {"_id": str(coin)
                    , "S_ID": int(senz.attributes["#S_ID"]), "S_PARA": senz.attributes["#S_PARA"],
                        "FORMAT_DATE": format_date,
                        "NO_COIN": int(1),
                        "TRANSACTION": [{"MINER": senz.attributes["#M_S_ID"],
                                         "RECIVER": senz.attributes["#RECIVER"],
                                         "T_NO_COIN": int(1),
                                         "DATE": datetime.datetime.utcnow()
                                         }
                                        ]
                        }
                self.collection.insert(root)
            elif(flag == "b_ct_ack"):
                #print('p2p Traction fail')
                root = {"_id": str(coin) ,
                        "FORMAT_DATE":datetime.datetime.utcnow() ,
                        "NO_COIN": int(1),
                        "TRANSACTION": [{"SENDER":senz.attributes["#COIN_SENDER"],
                                         "RECIVER": senz.attributes["#COIN_RECIVER"],
                                         "T_NO_COIN": int(1),
                                         "DATE": datetime.datetime.utcnow()
                                         }
                                        ]
                        }
                self.collection.insert(root)

        return 'DONE'




    #remove not_verified_detail_from_db
    def removeNotVerificationBlock(self,senz ,coin,coin_sender,coin_reciver):
        ##print "Not Verified Transaction"
        self.collection = self.db.block_chain
        pipe = [
            {"$match": {"_id": str(coin)}},
            {"$unwind": "$TRANSACTION"},
            {
                "$group": {
                    "_id": "$_id",
                    "last_transaction_date": {"$max": "$TRANSACTION.DATE"}
                }
            }
        ]

        # run aggregate pipeline
        cursor = self.collection.aggregate(pipeline=pipe)
        docs = list(cursor)

        '''print "DATA" , docs
        print "DATA", docs[0]["last_transaction_date"] , docs[0]["_id"]'''

        # run update
        self.collection.update_one(
            {"_id": docs[0]["_id"]},
            {
                "$pull": {
                    "TRANSACTION": {
                        "DATE": docs[0]["last_transaction_date"]
                    }
                }
            }
        )









