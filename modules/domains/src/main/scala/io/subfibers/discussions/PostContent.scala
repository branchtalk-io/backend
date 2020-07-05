package io.subfibers.discussions

sealed trait PostContent
object PostContent {
  final case class Url(url:   PostedURL) extends PostContent
  final case class Text(text: PostedText) extends PostContent
}
