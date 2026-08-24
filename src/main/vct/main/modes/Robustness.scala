package vct.main.modes

import com.typesafe.scalalogging.LazyLogging
import vct.col.origin.BlameCollector
import vct.col.print.Ctx
import vct.col.rewrite.{AddIfZero, InitialGeneration}
import vct.main.Main.{EXIT_CODE_ERROR, EXIT_CODE_SUCCESS}
import vct.main.stages.{Output, Parsing, Resolution, Transformation}
import vct.options.Options
import vct.parsers.transform.ConstantBlameProvider
import vct.result.VerificationError.{SystemError, UserError}

import java.nio.file.Paths

case object Robustness extends LazyLogging {
  def runOptions(options: Options): Int = {
    logger.info("Robustness mode started")

    val collector = BlameCollector()
    val blameProvider = ConstantBlameProvider(collector)

    // Adjusted for robustness mode: run AddIfZero after resolution, then emit
    // the selected post-resolution mutant as C for re-verification.
    val stages = Parsing.ofOptions[InitialGeneration](options, blameProvider)
      .thenRun(Resolution.ofOptions[InitialGeneration](options, blameProvider))
      .thenRun(new Transformation(
        onPassEvent = Nil,
        passes = Seq(AddIfZero),
        optimizeUnsafe = options.devUnsafeOptimization,
      )).thenRun(Output(
        out = Some(Paths.get("robustness-transformed.c")),
        syntax = Ctx.C,
        splitDecls = false,
      ))

    stages.run(options.inputs) match {
      case Left(err: UserError) =>
        logger.error(err.text)
        EXIT_CODE_ERROR

      case Left(err: SystemError) => throw err

      case Right(_) =>
        logger.info("Robustness transformation completed")
        logger.info("Output written to robustness-transformed.c")
        EXIT_CODE_SUCCESS
    }
  }
}
