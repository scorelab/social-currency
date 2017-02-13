import hashlib


class minning_algo:


    def __init__(self):
        pass
        #print "call mining algo class"



    def getCoin(self,arg):
        coin = hashlib.sha1(arg.encode("UTF-8")).hexdigest()
        ##return coin[:20]
        return coin