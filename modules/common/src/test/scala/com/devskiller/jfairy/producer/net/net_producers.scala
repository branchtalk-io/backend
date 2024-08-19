package com.devskiller.jfairy.producer.net

import com.devskiller.jfairy.producer.*

// workaround for default package
def makeNetworkProducer(baseProducer: BaseProducer): NetworkProducer =
  NetworkProducer(IPNumberProducer(baseProducer))
