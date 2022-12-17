package org.http4s.parser

import org.http4s.Http4sSuite
import org.http4s.internal.parsing.Rfc5322

class Rfc5322ParserSpec extends Http4sSuite {
  test("FWS parser") {
    val cases = List(
      " ",
      "  ",
      "\r\n  ",
      "   \r\n   "
    )
    cases.foreach(c => {
      assert(Rfc5322.FWS.parse(c).isRight)
    })
    val cases2 = List(
      "",
      "  \r\n",
      "\r\n"
    )
    cases2.foreach(c => {
      assert(Rfc5322.FWS.parse(c).isLeft)
    })
  }

  test("quoted-pair parser") {
    val cases = List(
      ("\\!", "!"),
      ("\\ ", " ")
    )
    cases.foreach(c => {
      assertEquals(Rfc5322.`quoted-pair`.parse(c._1).toOption.get._2, c._2)
    })
  }

  test("comment parser") {
    val cases = List(
      ("( comment1 comment2 ( comment3 ))", "( comment1 comment2 ( comment3 ))"),
      ("(    \r\n  comment)", "(    \r\n  comment)"),
      ("( \\! )", "( ! )"),
      ("(\\! )", "(! )"),
      ("( \\!)", "( !)"),
      ("(\\!)", "(!)"),
      ("( \\!   \\!   )", "( !   !   )"),
      ("( \\! comment )", "( ! comment )"),
      ("( \\! comment (((comment2))) ((comment3 \r\n (comment4 \\$))) )", "( ! comment (((comment2))) ((comment3 \r\n (comment4 $))) )")
    )
    cases.foreach(c => {
      assertEquals(Rfc5322.comment.parse(c._1).toOption.get._2, c._2)
    })
  }

  test("CFWS parser") {
    val cases = List(
      (" ", " "),
      (" ( comment ) ", " ( comment ) "),
      (" ( comment )  (comment 2) ", " ( comment )  (comment 2) ")
    )
    cases.foreach(c => {
      assertEquals(Rfc5322.CFWS.parse(c._1).toOption.get._2, c._2)
    })
  }

  test("atom parser") {
    val cases = List(
      ("abcd1234", "abcd1234"),
      (" abcd1234 (comment 2)", "abcd1234"),
      (" (comment 1) abcd1234 (comment 2)", "abcd1234")
    )
    cases.foreach(c => {
      assertEquals(Rfc5322.atom.parse(c._1).toOption.get._2, c._2)
    })
  }

  test("dot-atom-text parser") {
    val cases = List(
      ("a", "a"),
      ("a.b", "a.b"),
      ("abc.defg", "abc.defg")
    )
    cases.foreach(c => {
      assertEquals(Rfc5322.`dot-atom-text`.parse(c._1).toOption.get._2, c._2)
    })

    val failCases = List(
      ".abc"
    )
    failCases.foreach(c => {
      assertEquals(Rfc5322.`dot-atom-text`.parse(c).toOption, None)
    })
  }

  test("quoted-string parser") {
    val cases = List(
      ("\"a\"", "a"),
      (" (comment1) \"abc\\! dfg\\!\" (comment2) ", "abc! dfg!")
    )
    cases.foreach(c => {
      assertEquals(Rfc5322.`quoted-string`.parse(c._1).toOption.get._2, c._2)
    })
    val failCases = List(
      "a"
    )
    failCases.foreach(c => {
      assertEquals(Rfc5322.`quoted-string`.parse(c).toOption, None)
    })
  }

  test("domain-literal parser") {
    val cases = List(
      (" (comment1) [ 1 2 3 4 ] (comment2) ", "[ 1 2 3 4 ]"),
      ("[]", "[]"),
      ("[example.com]", "[example.com]")
    )
    cases.foreach(c => {
      assertEquals(Rfc5322.`domain-literal`.parse(c._1).toOption.get._2, c._2)
    })
  }
}
