package com.devskiller.jfairy.producer.text

import com.devskiller.jfairy.data.*
import com.devskiller.jfairy.producer.*
import com.devskiller.jfairy.producer.text.*

// workaround for default package
def makeTextProducer(baseProducer: BaseProducer, dataMaster: DataMaster): TextProducer =
  TextProducer(TextProducerInternal(dataMaster, baseProducer), baseProducer)
