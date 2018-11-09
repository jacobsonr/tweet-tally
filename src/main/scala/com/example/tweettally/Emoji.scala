package com.example.tweettally

import scala.collection.mutable.Map
import io.circe.parser._
import io.circe.generic.auto._
import TWStreamApp.logger

/** EmojiTree is a tree mapping strings to emojis.
 *  Each node of the tree contains a map of characters to EmojiTrees.
 *
 *  @param name contains the name of an emoji matched by the string leading
 *  to the particular node.
 *  To lookup an Emoji from a string, the maps are followed character by
 *  character, and the longest matching string is returned.
 */
class EmojiTree(val name: Option[String] = None) {
  val map: Map[Char, EmojiTree] = scala.collection.mutable.Map()

  /** Load emoji data from the supplied json file.  Insert all emojis
   *  from the file into this tree.
   *  @param emojiFileName path to emoji json file
   */
  def this(emojiFileName: String) = {
    this(None)
    decode[List[Emoji]](scala.io.Source.fromFile(emojiFileName).getLines.mkString("\n")) match {
      case Left(failure) => {
        logger.error("Invalid emoji json file")
      }
      case Right(emojis) => {
        logger.info(s"Loaded ${emojis.length} emojis.")
        emojis.foreach(e => insert(e.toUtf8, e.name.getOrElse(e.unified)))
      }
    }
  }

  /** Find the emoji matched by the longest possible string at the start
   *  of the given string.  If any match is found, returns the name and depth
   *  of the deepest possible match.
   *
   *  @param s search string, look for matching emojis at the start of this string
   */
  def apply(s: String): (Option[String], Int) = {
    if (s.isEmpty) (name, 0) // If we're at the end of the string, return this node.
    else {
      if (map contains s.head) {
        val (dname, depth) = map(s.head)(s.tail) // Follow the tree down another level
        dname match {
          case Some(_) => (dname, depth + 1) // If we found a deeper match, return it
          case None => (name, 0) // Otherwise return this node.
        }
      } else {
        (name, 0) // The next character of s doesn't match, so return this node.
      }
    }
  }

  /** Returns a list of all matching emojis in this string
   *  We will take the longest possible match from each position.
   *  Characters matched to an emoji will not be considered for other matches.
   *
   *  @param s the string to search for emojis
   */
  def lookup(s: String): List[String] = {

    def lookupAcc(s: String, acc: List[String]): List[String] = {
      if (s.isEmpty) acc
      else apply(s) match {
        case (Some(name), depth) => lookupAcc(s.drop(depth), name :: acc)
        case (None, _) => lookupAcc(s.tail, acc)
      }
    }

    lookupAcc(s, Nil)
  }

  /** Inserts an emoji into this tree
   *
   *  @param s string encoding of emoji to be inserted
   *  @param name name of emoji to be inserted
   */
  def insert(s: String, name: String): Unit = {
    if (!s.isEmpty) {
      if (map contains s.head) {
        if (s.length == 1) { // At the end of the string, add the emoji's name
          val n = new EmojiTree(Some(name))
          map += (s.head -> n)
          n.map ++= map(s.head).map // Keep existing children (longer strings)
        } else {
          map(s.head).insert(s.tail, name) // Insert into existing child
        }
      } else {
        if (s.length == 1) { // At the end of the string, add the emoji's name
          map += (s.head -> new EmojiTree(Some(name)))
        } else {
          val n = new EmojiTree(None)
          map += (s.head -> n)
          n.insert(s.tail, name) // Insert into a new child
        }
      }
    }
  }

  override def toString = {
    s"<'$name': [${map.map(x => s"(${x.toString})").mkString}]>"
  }

}

case class Emoji(name: Option[String], unified: String) {

  // Convert unicode to UTF8 encoding
  val toUtf8: String = {
    val unicode = unified.split("-")
    new String(unicode.flatMap { code =>
      val uval = Integer.parseInt(code, 16)
      Character.toChars(uval)
    })
  }
  
}
