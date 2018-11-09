package com.example.tweettally

import cats.implicits._
import TWStreamApp.{countHistorySize, logger}

object TweetStat {
  val emptyVector = (0 to countHistorySize).map(x => 0).toVector
}

/** Statistics on total counts from the stream of tweets
  */
case class TweetStat(

  /* Counts of tweets sent in particular recent seconds
   This isn't being used currently, but could be used
   to give tally's of tweets sent in each second,
   or summed in rotating storage for minute or hour totals.
   */
  counts: Vector[Int] = TweetStat.emptyVector, 

  count: Int = 0,              // Total number of tweets received
  delCount: Int = 1,           // Total number of delete messages received
  highTime: Int = 1,           // The latest second in which a tweet was sent
  lowTime: Int = Int.MaxValue, // The earliest second in which a tweet was sent
  lastHighTime: Int = 1,       // The previous value of highTime
  hasUrl: Int = 0,             // Number of tweets that contain urls
  hasPhoto: Int = 0,           // Number of tweets that contain photos
  hasEmoji: Int = 0,           // Number of tweets that contain emojis

  langs: Map[String, Int] = Map(),     // Counts of # of tweets by language
  hashTags: Map[String, Int] = Map(),  // ... by tag
  emojis: Map[String, Int] = Map(),    // ... by emoji
  domains: Map[String, Int] = Map()) { // ... by domain

  // Returns a string displaying the top 5 items in the supplied map, with counts.
  def top(m: Map[String, Int]): String =
    m.toList.sortBy(-_._2).take(5).map{case (k, v) => s"$k($v)"}.mkString(" ")

  // Returns a string representing what percent n is of the total count of tweets
  def percentOfTweets(n: Int) = s"${n*1000/count/10.0}%"

  override def toString = {
    val average = if (highTime > lowTime) count.toDouble / (highTime - lowTime) else 0
    s"""
Count: $count (+$delCount deletes)  (${average.toInt}/second, ${(average * 60).toInt}/minute, ${(average * 3600).toInt}/hour)
Urls: ${percentOfTweets(hasUrl)}, Photos: ${percentOfTweets(hasPhoto)}, Emojis: ${percentOfTweets(hasEmoji)}
Top emojis: ${top(emojis)}
Top domains: ${top(domains)}
Top tags: ${top(hashTags)}
Top languages: ${top(langs)}"""
  }

  /** Adds a TweetStat to this one and returns the sum in a new TweetStat
   *  All counts are summed, including the counts maps
   *  Min and max values are tracked for tweet times
   *
   * @param that TweetStat to add to this one
   */
  def +(that: TweetStat) = {
    //logger.info(s"Combining tweetstats in thread ${Thread.currentThread()}")
    val thisCounts = if (highTime > that.highTime) counts.updated((highTime + 1) % countHistorySize, 0) else counts
    TweetStat(
      thisCounts.zip(that.counts).map{case (x, y) => x + y},
      count + that.count,
      delCount + that.delCount,
      Math.max(highTime, that.highTime),
      Math.min(lowTime, that.lowTime),
      highTime,
      hasUrl + that.hasUrl,
      hasPhoto + that.hasPhoto,
      hasEmoji + that.hasEmoji,
      langs |+| that.langs,
      hashTags |+| that.hashTags,
      emojis |+| that.emojis,
      domains |+| that.domains
    )
  }
}
