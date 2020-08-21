package io.branchtalk.shared

import cats.effect.IO
import io.codearte.jfairy.Fairy
import io.codearte.jfairy.producer.{ BaseProducer, DateProducer }
import io.codearte.jfairy.producer.company.{ Company, CompanyProperties }
import io.codearte.jfairy.producer.net.NetworkProducer
import io.codearte.jfairy.producer.payment.CreditCard
import io.codearte.jfairy.producer.person.{ Person, PersonProperties }
import io.codearte.jfairy.producer.text.TextProducer

object Fixtures {

  private val fairy = IO(Fairy.create())

  val baseProducer: IO[BaseProducer] = fairy.map(_.baseProducer)
  def company(companyProperties: CompanyProperties.CompanyProperty*): IO[Company] =
    fairy.map(_.company(companyProperties: _*))
  val creditCard:      IO[CreditCard]      = fairy.map(_.creditCard)
  val dateProducer:    IO[DateProducer]    = fairy.map(_.dateProducer)
  val networkProducer: IO[NetworkProducer] = fairy.map(_.networkProducer)
  def person(personProperties: PersonProperties.PersonProperty*): IO[Person] =
    fairy.map(_.person(personProperties: _*))
  val textProducer: IO[TextProducer] = fairy.map(_.textProducer)

  val nameLike:      IO[String] = textProducer.map(_.limitedTo(3).latinSentence())
  val noWhitespaces: IO[String] = nameLike.map(_.replaceAll("[^A-Za-z0-9_-]", "-").toLowerCase)
}
