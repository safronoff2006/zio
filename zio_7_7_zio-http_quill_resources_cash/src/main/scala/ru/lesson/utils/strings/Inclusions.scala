package ru.lesson.utils.strings

import scala.annotation.tailrec

object Inclusions {
  def contain(word: String, text: String): Int = {
    val n = word.length

    @tailrec
    def recursSolve(acc: Int, word: String, sb: String): Int = sb match {
      case _ if sb.length < n => acc
      case _ if sb.substring(0, n) == word => recursSolve(acc + 1, word, sb.tail)
      case _ => recursSolve(acc, word, sb.tail)
    }
    recursSolve(0, word, text)
  }
}
