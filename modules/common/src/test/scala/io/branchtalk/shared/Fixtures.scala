package io.branchtalk.shared

import cats.effect.IO
import com.devskiller.jfairy.Fairy
import com.devskiller.jfairy.producer.{ BaseProducer, DateProducer }
import com.devskiller.jfairy.producer.company.{ Company, CompanyProperties }
import com.devskiller.jfairy.producer.net.NetworkProducer
import com.devskiller.jfairy.producer.payment.CreditCard
import com.devskiller.jfairy.producer.person.{ Person, PersonProperties }
import com.devskiller.jfairy.producer.text.TextProducer

object Fixtures {

  private val fairy = Fairy.create().pure[IO]

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
