package vct.main.stages

import hre.stages.Stage

import vct.col.ast.Program
import vct.col.origin.FileSpanningOrigin
import vct.col.print.Ctx
import vct.col.rewrite.{
  CStatementPassBuilder,
  Generation,
}

import vct.parsers.ParseResult
import vct.parsers.transform.BlameProvider

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

case class CSourceTransformation[
  G <: Generation
](
   blameProvider: BlameProvider,
   pass: CStatementPassBuilder,
 ) extends Stage[
  ParseResult[G],
  ParseResult[G],
] {

  override def friendlyName: String =
    s"C Source Transformation: ${pass.key}"

  override def progressWeight: Int =
    1

  override def run(
                    in: ParseResult[G]
                  ): ParseResult[G] = {

    implicit val o =
      FileSpanningOrigin

    println("[DEBUG] PROGRAM OLUSTURMA BASLIYOR")

    val program =
      Program[G](
        in.decls
      )(
        blameProvider()
      )

    println("[DEBUG] PROGRAM OLUSTURMA BITTI")

    println("[DEBUG] ADDIFZERO DISPATCH BASLIYOR")

    val rewritten =
      pass
        .applyX[G]()
        .dispatch(program)

    println("[DEBUG] ADDIFZERO DISPATCH BITTI")

    val outputPath =
      Paths.get(
        "robustness-source.c"
      )

    val writer =
      Files.newBufferedWriter(
        outputPath,
        StandardCharsets.UTF_8,
      )

    try {

      val ctx =
        Ctx(
          syntax = Ctx.C
        ).namesIn(rewritten)

      rewritten.write(writer)(ctx)

    } finally {
      writer.close()
    }

    println(
      "[Robustness] transformed C written to robustness-source.c"
    )

    ParseResult(
      rewritten.declarations,
      in.expectedErrors,
    )
  }
}