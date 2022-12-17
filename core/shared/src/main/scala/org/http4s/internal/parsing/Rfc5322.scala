package org.http4s.internal.parsing

import cats.parse.Parser
import cats.parse.Rfc5234.{alpha, crlf, digit, dquote, vchar, wsp}
import cats.parse.Parser.{char, charIn, string, void}

object Rfc5322 {
  val FWS: Parser[Unit] = (wsp.rep0 *> crlf).backtrack.?.with1 *> void(wsp.rep)
  val ctext: Parser[Char] = charIn(((33 to 39) ++ (42 to 91) ++ (93 to 126)).map(_.toChar))
  val `quoted-pair`: Parser[String] = char('\\') *> (vchar.string | string(wsp))
  val ccontent: Parser[String] = Parser.defer(ctext.map(_.toString) | `quoted-pair` | comment)
  val comment: Parser[String] = Parser.defer(char('(').string ~
    (FWS.string.?.map(_.getOrElse("")).with1 ~ ccontent)
      .backtrack
      .map(p => p._1 + p._2)
      .rep0.map(_.mkString) ~ FWS.string.?.map(_.getOrElse("")) ~ char(')').string)
  .map { case (((s1, s2), s3), s4) => s1 + s2 + s3 + s4 }
  val CFWS: Parser[String] = ((FWS.string.?.map(_.getOrElse("")).with1 ~ comment)
    .backtrack
    .map(p => p._1 + p._2)
    .rep
    .map(_.toList.mkString) ~ FWS.string.?.map(_.getOrElse("")))
    .map{ case (s1, s2) => s1 + s2 } | string(FWS)

  val atext: Parser[String] = (alpha | digit | charIn(
    '!', '#', '$', '%', '&', '\'', '*', '+', '-', '/', '=', '?', '^', '_', '`', '{', '|', '}', '~'
  )).string
    val atom: Parser[String] = CFWS.?.with1 *> atext.rep.map(_.toList.mkString) <* CFWS.?
    val `dot-atom-text`: Parser[String] = (atext.rep.map(_.toList.mkString) ~
      (char('.').string ~ atext.rep.map(_.toList.mkString)).map(p => p._1 + p._2).rep0.map(_.mkString))
      .map(p => p._1 + p._2)
    val `dot-atom`: Parser[String] = CFWS.?.with1 *> `dot-atom-text` <* CFWS.?
    val qtext: Parser[String] = charIn((Seq(33) ++ (35 to 91) ++ (93 to 126)).map(_.toChar)).string
    val qcontent: Parser[String] = qtext | `quoted-pair`
    val `quoted-string`: Parser[String] = CFWS.?.with1 *> dquote *> (FWS.string.?.map(_.getOrElse("")).with1 ~ qcontent)
      .map(p => p._1 + p._2).rep0.map(_.mkString) <* FWS.? <* dquote <* CFWS.?
    val word: Parser[String] = atom | `quoted-string`
    val phrase: Parser[String] = word.rep.map(_.toList.mkString)
    val `display-name`: Parser[String] = phrase
    val `local-part`: Parser[String] = `dot-atom` | `quoted-string`
    val dtext: Parser[String] = charIn(((33 to 90) ++ (94 to 126)).map(_.toChar)).map(_.toString)
    val `domain-literal`: Parser[String] = CFWS.?.with1 *> (
      char('[').string ~
      (FWS.string.?.map(_.getOrElse("")).with1 ~ dtext).backtrack.map(p => p._1 + p._2).rep0.map(_.mkString) ~
        FWS.string.?.map(_.getOrElse("")) ~
        char(']').string
      ).map{ case (((s1, s2), s3), s4) => s1 + s2 + s3 + s4} <* CFWS.?
    val domain: Parser[String] = `dot-atom` | `domain-literal`
    val `addr-spec`: Parser[String] = (`local-part` ~ char('@').string ~ domain).map{
      case ((s1, s2), s3) => s1 + s2 + s3
    }
    val `angle-addr`: Parser[String] = (CFWS.?.with1 *> char('<').string ~ `addr-spec` ~ char('>').string <* CFWS.?)
      .map{ case ((s1, s2), s3) => s1 + s2 + s3}
    val `name-addr`: Parser[String] = (`display-name`.?.with1 ~ `angle-addr`).map(p => p._1.getOrElse("") + p._2)
    val mailbox: Parser[String] = `name-addr`.backtrack | `addr-spec`
}
