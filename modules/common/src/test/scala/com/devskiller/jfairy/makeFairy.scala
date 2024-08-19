package com.devskiller.jfairy

import com.devskiller.jfairy.data.*
import com.devskiller.jfairy.producer
import com.devskiller.jfairy.producer.*
import com.devskiller.jfairy.producer.company.*
import com.devskiller.jfairy.producer.net.*
import com.devskiller.jfairy.producer.payment.*
import com.devskiller.jfairy.producer.person.*
import com.devskiller.jfairy.producer.text.*

// workaround for default package
def makeFairy(localeString: String): Fairy = {

  // these psychopats used Guice for building this, and now I'm getting:
  //   com.google.inject.CreationException: Unable to create injector
  val randomGenerator: RandomGenerator = RandomGenerator()
  val baseProducer:    BaseProducer    = BaseProducer(randomGenerator)
  val dataMaster: DataMaster = MapBasedDataMaster(baseProducer).tap { dm =>
    dm.readResources("jfairy.yml")
    dm.readResources("jfairy_" + localeString + ".yml")
  }
  val textProducer:       TextProducer       = makeTextProducer(baseProducer, dataMaster)
  val timeProvider:       TimeProvider       = TimeProvider()
  val dateProducer:       DateProducer       = DateProducer(baseProducer, timeProvider)
  val networkProducer:    NetworkProducer    = makeNetworkProducer(baseProducer)
  val creditCardProvider: CreditCardProvider = CreditCardProvider(dataMaster, baseProducer, dateProducer)
  val (nationalIdentificationNumberFactory: NationalIdentificationNumberFactory,
       nationalIdentityCardNumberProvider:  NationalIdentityCardNumberProvider,
       vatIdentificationNumberProvider:     VATIdentificationNumberProvider,
       addressProvider:                     AddressProvider,
       passportNumberProvider:              PassportNumberProvider
  ) = localeString match {
    case "de" =>
      (
        producer.person.locale.NoNationalIdentificationNumberFactory(baseProducer, dateProducer),
        producer.person.locale.de.DeNationalIdentityCardNumberProvider(baseProducer),
        producer.company.locale.de.DeVATIdentificationNumberProvider(),
        producer.person.locale.de.DeAddressProvider(dataMaster, baseProducer),
        producer.person.locale.de.DePassportNumberProvider(baseProducer)
      )
    case "es" =>
      (
        producer.person.locale.NoNationalIdentificationNumberFactory(baseProducer, dateProducer),
        producer.person.locale.es.EsNationalIdentityCardNumberProvider(),
        producer.company.locale.es.EsVATIdentificationNumberProvider(),
        producer.person.locale.es.EsAddressProvider(dataMaster, baseProducer),
        producer.person.locale.es.EsPassportNumberProvider()
      )
    /*
    case "fr" =>
      (
        producer.person.locale.NoNationalIdentificationNumberFactory(baseProducer, dateProducer),
        producer.person.locale.fr.FrNationalIdentityCardNumberProvider(baseProducer),
        producer.company.locale.fr.FrVATIdentificationNumberProvider(baseProducer),
        producer.person.locale.fr.FrAddressProvider(dataMaster, baseProducer),
        producer.person.locale.fr.FrPassportNumberProvider()
      )
     */
    case "ka" =>
      (
        producer.person.locale.NoNationalIdentificationNumberFactory(baseProducer, dateProducer),
        producer.person.locale.ka.KaNationalIdentityCardNumberProvider(baseProducer),
        producer.company.locale.ka.KaVATIdentificationNumberProvider(baseProducer),
        producer.person.locale.ka.KaAddressProvider(dataMaster, baseProducer),
        producer.person.locale.ka.KaPassportNumberProvider(baseProducer)
      )
    case "pl" =>
      (
        producer.person.locale.NoNationalIdentificationNumberFactory(baseProducer, dateProducer),
        producer.person.locale.pl.PlNationalIdentityCardNumberProvider(dateProducer, baseProducer),
        producer.company.locale.pl.PlVATIdentificationNumberProvider(baseProducer),
        producer.person.locale.pl.PlAddressProvider(dataMaster, baseProducer),
        producer.person.locale.pl.PlPassportNumberProvider()
      )
    case "sv" =>
      val num = producer.person.locale.NoNationalIdentificationNumberFactory(baseProducer, dateProducer)
      (
        num,
        producer.person.locale.sv.SvNationalIdentityCardNumberProvider(dateProducer, baseProducer),
        producer.company.locale.sv.SvVATIdentificationNumberProvider(baseProducer, dateProducer, num),
        producer.person.locale.sv.SvAddressProvider(dataMaster, baseProducer),
        producer.person.locale.sv.SvPassportNumberProvider()
      )
    case "zh" =>
      (
        producer.person.locale.NoNationalIdentificationNumberFactory(baseProducer, dateProducer),
        producer.person.locale.zh.ZhNationalIdentityCardNumberProvider(baseProducer),
        producer.company.locale.zh.ZhVATIdentificationNumberProvider(),
        producer.person.locale.zh.ZhAddressProvider(dataMaster, baseProducer),
        producer.person.locale.zh.ZhPassportNumberProvider()
      )
    // en
    case _ =>
      (
        producer.person.locale.NoNationalIdentificationNumberFactory(baseProducer, dateProducer),
        producer.person.locale.en.EnNationalIdentityCardNumberProvider(baseProducer),
        producer.company.locale.en.EnVATIdentificationNumberProvider(baseProducer),
        producer.person.locale.en.EnAddressProvider(dataMaster, baseProducer),
        producer.person.locale.en.EnPassportNumberProvider()
      )
  }
  val companyFactory: CompanyFactory = new CompanyFactory {
    override def produceCompany(companyProperties: CompanyProperties.CompanyProperty*): CompanyProvider =
      DefaultCompanyProvider(baseProducer, dataMaster, vatIdentificationNumberProvider, companyProperties: _*)
  }
  val personFactory: PersonFactory = new PersonFactory {
    override def producePersonProvider(personProperties: PersonProperties.PersonProperty*): PersonProvider =
      DefaultPersonProvider(
        dataMaster,
        dateProducer,
        baseProducer,
        nationalIdentificationNumberFactory,
        nationalIdentityCardNumberProvider,
        addressProvider,
        companyFactory,
        passportNumberProvider,
        timeProvider,
        personProperties: _*
      )
  }
  val ibanFactory: IBANFactory = new IBANFactory {
    override def produceIBANProvider(properties: IBANProperties.Property*): IBANProvider =
      DefaultIBANProvider(baseProducer, dataMaster, properties: _*)
  }

  Fairy(textProducer,
        personFactory,
        networkProducer,
        baseProducer,
        dateProducer,
        creditCardProvider,
        companyFactory,
        ibanFactory
  )
}
