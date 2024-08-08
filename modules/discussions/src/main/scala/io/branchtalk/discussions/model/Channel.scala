package io.branchtalk.discussions.model

import cats.{ Order, Show }
import io.branchtalk.shared.model.*

final case class Channel(
  id:   ID[Channel],
  data: Channel.Data
) derives FastEq,
      ShowPretty
object Channel {

  final case class Data(
    urlName:        Channel.UrlName,
    name:           Channel.Name,
    description:    Option[Channel.Description],
    createdAt:      CreationTime,
    lastModifiedAt: Option[ModificationTime]
  ) derives FastEq,
        ShowPretty

  final case class Create(
    authorID:    ID[User],
    urlName:     Channel.UrlName,
    name:        Channel.Name,
    description: Option[Channel.Description]
  ) derives FastEq,
        ShowPretty

  final case class Update(
    id:             ID[Channel],
    editorID:       ID[User],
    newUrlName:     Updatable[Channel.UrlName],
    newName:        Updatable[Channel.Name],
    newDescription: OptionUpdatable[Channel.Description]
  ) derives FastEq,
        ShowPretty

  final case class Delete(
    id:       ID[Channel],
    editorID: ID[User]
  ) derives FastEq,
        ShowPretty

  final case class Restore(
    id:       ID[Channel],
    editorID: ID[User]
  ) derives FastEq,
        ShowPretty

  type UrlName = UrlName.Type
  object UrlName extends Newtype[String] {

    private val pattern = "[A-Za-z0-9_-]+".r

    override inline def validate(input: String): Boolean = pattern.matches(input)

    def unapply(urlName: UrlName): Some[String] = Some(urlName.unwrap)

    given Show[UrlName]  = unsafeMakeF[Show](Show[String])
    given Order[UrlName] = unsafeMakeF[Order](Order[String])
  }

  type Name = Name.Type
  object Name extends Newtype[String] {

    override inline def validate(input: String): Boolean = input.nonEmpty

    def unapply(name: Name): Some[String] = Some(name.unwrap)

    given Show[Name]  = unsafeMakeF[Show](Show[String])
    given Order[Name] = unsafeMakeF[Order](Order[String])
  }

  type Description = Description.Type
  object Description extends Newtype[String] {

    override inline def validate(input: String): Boolean = input.nonEmpty

    def unapply(description: Description): Some[String] = Some(description.unwrap)

    given Show[Description]  = unsafeMakeF[Show](Show[String])
    given Order[Description] = unsafeMakeF[Order](Order[String])
  }

  enum Sorting {
    case Newest
    case Alphabetically
  }
}
