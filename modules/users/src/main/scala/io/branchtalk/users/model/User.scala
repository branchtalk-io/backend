package io.branchtalk.users.model

import cats.{ Order, Show }
import cats.effect.Sync
import io.branchtalk.shared.model.*

final case class User(
  id:   ID[User],
  data: User.Data
) derives FastEq,
      ShowPretty
object User {

  final case class Data(
    email:          User.Email, // validate email
    username:       User.Name,
    description:    Option[User.Description],
    password:       Password,
    permissions:    Permissions,
    createdAt:      CreationTime,
    lastModifiedAt: Option[ModificationTime]
  ) derives FastEq,
        ShowPretty

  final case class Create(
    email:       User.Email,
    username:    User.Name,
    description: Option[User.Description],
    password:    Password
  ) derives FastEq,
        ShowPretty

  final case class Update(
    id:                ID[User],
    moderatorID:       Option[ID[User]],
    newUsername:       Updatable[User.Name],
    newDescription:    OptionUpdatable[User.Description],
    newPassword:       Updatable[Password],
    updatePermissions: List[Permission.Update]
  ) derives FastEq,
        ShowPretty

  final case class Delete(
    id:          ID[User],
    moderatorID: Option[ID[User]]
  ) derives FastEq,
        ShowPretty

  final case class Restore(
    id:          ID[User],
    moderatorID: Option[ID[User]]
  ) derives FastEq,
        ShowPretty

  type Email = Email.Type
  object Email extends Newtype[String] {

    private val pattern = "(.+)@(.+)".r

    override inline def validate(input: String): Boolean = pattern.matches(input)

    def unapply(email: Email): Some[String] = Some(email.unwrap)

    given Show[Email]  = unsafeMakeF[Show](Show[String])
    given Order[Email] = unsafeMakeF[Order](Order[String])
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

  enum Filter {
    case HasPermission(permission: Permission)
    case HasPermissions(permissions: Permissions)
    case NameContains(query: String)
  }

  enum Sorting derives FastEq, ShowPretty {
    case Newest
    case NameAlphabetically
    case EmailAlphabetically
  }
}
