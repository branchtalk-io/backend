package io.branchtalk.notifications.model

import cats.{ Order, Show }
import hearth.kindlings.avroderivation.{ AvroDecoder, AvroEncoder }
import io.branchtalk.shared.model.*
import io.branchtalk.shared.model.AvroSupport.{ *, given }

final case class Notification(
  id:   ID[Notification],
  data: Notification.Data
) derives FastEq,
      ShowPretty
object Notification {

  final case class Data(
    recipientID: ID[User],
    kind:        Notification.Kind,
    sourcePostID:    Option[ID[Post]],
    sourceCommentID: Option[ID[Comment]],
    sourceUserID:    Option[ID[User]],
    message:     Notification.Message,
    createdAt:   CreationTime,
    readAt:      Option[ModificationTime]
  ) derives FastEq,
        ShowPretty

  // Commands
  final case class MarkRead(
    id:     ID[Notification],
    userID: ID[User]
  ) derives FastEq,
        ShowPretty

  final case class MarkAllRead(
    recipientID: ID[User]
  ) derives FastEq,
        ShowPretty

  enum Kind derives FastEq, ShowPretty {
    case PostReply
    case CommentReply
    case NewPostInChannel
  }
  @SuppressWarnings(Array("org.wartremover.warts.ToString"))
  object Kind {

    private val byName: Map[String, Kind] = Kind.values.map(k => k.toString.toLowerCase(branchtalkLocale) -> k).toMap

    @SuppressWarnings(Array("org.wartremover.warts.Throw"))
    def fromString(s: String): Kind =
      byName.getOrElse(s.toLowerCase(branchtalkLocale), throw new NoSuchElementException(show"Unknown Kind: $s"))

    given Show[Kind] = _.toString.toLowerCase(branchtalkLocale)

    given AvroCodec[Kind] with {
      private val E = summon[AvroEncoder[String]]
      private val D = summon[AvroDecoder[String]]
      def schema:              org.apache.avro.Schema = E.schema
      def encode(value: Kind): Any                    = E.encode(value.toString.toLowerCase(branchtalkLocale))
      def decode(value: Any):  Kind                   = fromString(D.decode(value))
    }
  }

  type Message = Message.Type
  object Message extends Newtype[String] {
    def unapply(message: Message): Some[String] = Some(message.unwrap)

    given Show[Message]  = unsafeMakeF[Show](Show[String])
    given Order[Message] = unsafeMakeF[Order](Order[String])
  }

  enum Sorting derives FastEq, ShowPretty {
    case Newest
  }
}
