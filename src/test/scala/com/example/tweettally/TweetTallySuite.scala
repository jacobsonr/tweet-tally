package com.example.tweettally

import org.scalatest.FunSuite

class TweetTallySuite extends FunSuite {
  val emojis = new EmojiTree()
  emojis.insert("HER", "her")
  emojis.insert("HERE", "here")
  emojis.insert("THERE", "there")
  println(s"${emojis.toString}")

  test("No emoji") {
    assert(emojis.lookup("nothing") == Nil, "No emoji")
  }
  test("Only an emoji") {
    assert(emojis.lookup("HER") == List("her"))
  }

  test("Ignore match in another emoji") {
    assert(emojis.lookup("THERE") == List("there"))
  }
  test("Ignore match in another emojix") {
    assert(emojis.lookup("THERExx") == List("there"))
  }
  test("Take longest match") {
    assert(emojis.lookup("HERE") == List("here"))
  }
  test("Two emojis") {
    assert(emojis.lookup("HERExxTHERE").sorted == List("here", "there"))
  }
  test("Two connected emojis") {
    assert(emojis.lookup("HERETHERE").sorted == List("here", "there"))
  }
  test("Emoji at end") {
    assert(emojis.lookup("xxHERE") == List("here"))
  }
  test("Emoji at start") {
    assert(emojis.lookup("HERExx") == List("here"))
  }
  test("Emoji in middle") {
    assert(emojis.lookup("xxxHERExxx") == List("here"))
  }
}
