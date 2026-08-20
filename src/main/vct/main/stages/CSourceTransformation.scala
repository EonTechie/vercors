package vct.main.stages

import hre.stages.Stage

import vct.col.ast.Program
import vct.col.origin.FileSpanningOrigin
import vct.col.print.Ctx
import vct.col.rewrite.{
  CSourceRewriterBuilder,
  Generation,
  Rewritten,
}

import vct.parsers.ParseResult
import vct.parsers.transform.BlameProvider

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}

case class CSourceTransformation[
  G <: Generation
](
   blameProvider: BlameProvider,
   pass: CSourceRewriterBuilder,
 ) extends Stage[
  ParseResult[G],
  ParseResult[Rewritten[G]],
] {

  override def friendlyName: String =
    s"C Source Transformation: ${pass.key}"

  override def progressWeight: Int =
    1

  override def run(
                    in: ParseResult[G]
                  ): ParseResult[Rewritten[G]] = {

    implicit val o =
      FileSpanningOrigin

    val program =
      Program[G](
        in.decls
      )(
        blameProvider()
      )

    val rewritten =
      pass
        .apply[G]()
        .dispatch(program)

    /*
     * IMPORTANT:
     *
     * Write the transformed program HERE,
     * before Resolution converts CFunctionDefinition
     * into generic Procedure nodes.
     *
     * CFunctionDefinition's C printer preserves
     * requires / ensures / context_everywhere.
     */
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
      "[Robustness] pre-resolution transformed C written to robustness-source.c"
    )

    ParseResult(
      rewritten.declarations,
      in.expectedErrors,
    )
  }
}