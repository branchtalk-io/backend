package io.subfibers.discussions.models

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import io.subfibers.shared.models.{ CreationTime, ID, ModificationTime }

final case class Topic(
  id:             ID[Topic],
  urlName:        Topic.UrlName,
  name:           Topic.Name,
  description:    Option[Topic.Description],
  createdAt:      CreationTime,
  lastModifiedAt: Option[ModificationTime]
)
object Topic {

  @newtype final case class UrlName(value:     String Refined MatchesRegex["[A-Za-z0-9_-]+"])
  @newtype final case class Name(value:        NonEmptyString)
  @newtype final case class Description(value: NonEmptyString)

  final case class Create(
    urlName:     Topic.UrlName,
    name:        Topic.Name,
    description: Option[Topic.Description]
  )

  final case class Update(
    id:          ID[Topic],
    urlName:     Topic.UrlName,
    name:        Topic.Name,
    description: Option[Topic.Description]
  )
}
