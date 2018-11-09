package com.example.tweettally

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

import cats.effect._
import cats.implicits._
import com.typesafe.config.{Config, ConfigFactory}
import fs2.Stream
import fs2.io.stdout
import fs2.text.{lines, utf8Encode}

import io.circe.{Decoder, Json}
import io.circe.fs2.decoder
import io.circe.generic.auto._
import jawnfs2._
import org.http4s.{Method, Request, Uri}
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.client.oauth1
import org.slf4j.LoggerFactory

import TWStreamApp.logger

object TWStreamApp extends IOApp {
  val config: Config = ConfigFactory.load("application.conf")
  val logger = LoggerFactory.getLogger("TweetTally")

  lazy val emojiFile = config.getString("emojiFile")
  val emojis = new EmojiTree(emojiFile)

  lazy val consumerKey = config.getString("consumerKey")
  lazy val consumerSecret = config.getString("consumerSecret")
  lazy val accessToken = config.getString("accessToken")
  lazy val accessSecret = config.getString("accessSecret")
  lazy val countHistorySize = config.getInt("countHistorySize")

  def run(args: List[String]) =
    (new TWStream[IO]).run.as(ExitCode.Success)
}

class TWStream[F[_]](implicit F: ConcurrentEffect[F], cs: ContextShift[F]) {
  implicit val f = io.circe.jawn.CirceSupportParser.facade

  /** These values are created by a Twitter developer web app.
   *  OAuth signing is an effect due to generating a nonce for each `Request`.
   */
  def sign(consumerKey: String, consumerSecret: String, accessToken: String, accessSecret: String)
          (req: Request[F]): F[Request[F]] = {
    val consumer = oauth1.Consumer(consumerKey, consumerSecret)
    val token    = oauth1.Token(accessToken, accessSecret)
    oauth1.signRequest(req, consumer, callback = None, verifier = None, token = Some(token))
  }

  /** Create a http client, sign the incoming `Request[F]`, stream the `Response[IO]`, and
   *  `parseJsonStream` the `Response[F]`.
   *  `sign` returns a `F`, so we need to `Stream.eval` it to use a for-comprehension.
   */
  def jsonStream(consumerKey: String, consumerSecret: String, accessToken: String, accessSecret: String)
            (req: Request[F]): Stream[F, Json] =
    for {
      client <- BlazeClientBuilder(ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(4))).stream
      sr  <- Stream.eval(sign(consumerKey, consumerSecret, accessToken, accessSecret)(req))
      res <- client.stream(sr).flatMap(_.body.chunks.parseJsonStream)
    } yield res

  /** Decoder to decode json tweets */
  implicit val decodeTweet: Decoder[TweetMessage] =
    List[Decoder[TweetMessage]](
      Decoder[Tweet].widen,
      Decoder[Delete].widen
    ).reduceLeft(_ or _)

  /** If we've hit a new high value for time in seconds, returns a string with
   *  console logging information.  Otherwise an empty string.
   *
   *  @param ts running total of tweet stream statistics
   */
  def report(ts: TweetStat): String =
    if (ts.highTime > ts.lastHighTime) s"${ts.toString}\n"
    else ""

  /** Stream the sample statuses.
   *  Enter in your four Twitter API values in application.conf
   */
  def stream(blockingEC: ExecutionContext) = {
    val req = Request[F](Method.GET, Uri.uri("https://stream.twitter.com/1.1/statuses/sample.json"))
    val s: Stream[F, Json] = jsonStream(TWStreamApp.consumerKey,
      TWStreamApp.consumerSecret,
      TWStreamApp.accessToken,
      TWStreamApp.accessSecret)(req)
    val tweets: Stream[F, TweetMessage] = s.through(decoder[F, TweetMessage])
    val tts = tweets.map(_.toTweetStat)
    val tally = tts.scan(TweetStat())(_ + _)
    tally.map(report).through(utf8Encode).to(stdout(blockingEC))
  }

  /** We're going to be writing to stdout, which is a blocking API.  We don't
   *  want to block our main threads, so we create a separate pool.  We'll use
   *  `fs2.Stream` to manage the shutdown for us.
   */
  def blockingEcStream: Stream[F, ExecutionContext] =
    Stream.bracket(F.delay(Executors.newFixedThreadPool(4)))(pool =>
        F.delay(pool.shutdown()))
      .map(ExecutionContext.fromExecutorService)

  /** Compile our stream down to an effect to make it runnable */
  def run: F[Unit] =
    blockingEcStream.flatMap { blockingEc =>
      stream(blockingEc)
    }.compile.drain

}
