package io.subfibers.discussions.models

import io.subfibers.ADT

sealed trait PostContent extends ADT
object PostContent {
  final case class Url(url:   PostedURL) extends PostContent
  final case class Text(text: PostedText) extends PostContent
}
