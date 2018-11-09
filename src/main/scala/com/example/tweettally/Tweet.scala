package com.example.tweettally
import TweetStat.emptyVector

/** Classes for tweets and components of tweets that we are interested in
 *  These are used to decode the json stream
 */
case class Url(display_url: String)

case class MUrl(display_url: String, media_url: String)

case class Tags(text: String)

case class Entities(urls: List[Url], media: Option[List[MUrl]], hashtags: List[Tags]) {
  val links = media match {
    case None => urls.map(_.display_url)
    case Some(media) => (urls.map(_.display_url) ++ media.map(_.display_url) ++
        media.map(_.media_url)).map(url => if (url.startsWith("http://")) url.drop(7) else url)
  }
  val domains = links.map(_.takeWhile(_ != '/'))
}

sealed trait TweetMessage { // Can either be a tweet, or a delete message
  def toTweetStat: TweetStat
}

case class Tweet(text: String, lang: String, entities: Entities, timestamp_ms: Long) extends TweetMessage {
  // We could get the tags by hand, but will just use the json values instead
  // val words = text.split("\\s+").toList
  // val tags = words.filter(w => w.length > 1 && w.head == '#').map(_.drop(1))

  lazy val tags = entities.hashtags.map(_.text)
  lazy val linkDomains = entities.domains
  lazy val hasPhoto = linkDomains.contains("pic.twitter.com") || linkDomains.contains("instagram.com")
  lazy val emojis = TWStreamApp.emojis.lookup(text)
  lazy val time = (timestamp_ms / 1000).toInt

  /** Creates a TweetStat from this Tweet.
   *  Essentially, it's the total stats if this were the only tweet.
   */
  def toTweetStat = TweetStat(
    emptyVector.updated(time & 15, 1),
    1, 0, time, time, time,
    if (linkDomains.size > 0) 1 else 0,
    if (hasPhoto) 1 else 0,
    if (emojis.size > 0) 1 else 0,
    Map(lang -> 1),
    tags.map((_, 1)).toMap, // We'll only count one of multiple matching entities in a single tweet
    emojis.map((_, 1)).toMap,
    linkDomains.map((_, 1)).toMap
  )
}

case class DeleteInfo(timestamp_ms: String)

case class Delete(delete: DeleteInfo) extends TweetMessage {
  def toTweetStat = TweetStat()
}
