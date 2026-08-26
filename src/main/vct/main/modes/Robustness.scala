package vct.main.modes

import com.typesafe.scalalogging.LazyLogging
import vct.col.origin.BlameCollector
import vct.col.print.Ctx
import vct.col.rewrite.{
  AddIfOne,
  AddIfZero,
  InitialGeneration,
  RewriterBuilder,
  RobustnessForLoopToWhileLoop,
}
import vct.main.Main.{EXIT_CODE_ERROR, EXIT_CODE_SUCCESS}
import vct.main.stages.{Output, Parsing, Resolution, Transformation}
import vct.options.Options
import vct.parsers.transform.ConstantBlameProvider
import vct.result.VerificationError.{SystemError, UserError}

import java.nio.file.{Path, Paths}

case object Robustness extends LazyLogging {
  private val outputPath: Path = Paths.get("robustness-transformed.c")

  private def parseTransform(name: String): RewriterBuilder =
    name.trim.toLowerCase match {
      case "" | "add-if-zero" | "add_if_zero" | "if0error" =>
        AddIfZero

      case "add-if-one" | "add_if_one" | "add-if1" | "add_if1" =>
        AddIfOne

      case "for-to-while" | "for_to_while" | "for2while" |
          "for-loop-to-while" | "for_loop_to_while" =>
        RobustnessForLoopToWhileLoop

      case other =>
        throw new IllegalArgumentException(
          s"Unknown ROBUSTNESS_TRANSFORM=$other. " +
            "Use add-if-zero, add-if-one, or for-to-while, " +
            "optionally comma-separated."
        )
    }

  private def selectedRobustnessPasses: Seq[RewriterBuilder] = {
    val raw = sys.env.get("ROBUSTNESS_TRANSFORM").map(_.trim).filter(_.nonEmpty)
      .getOrElse("add-if-zero")
    val names = raw.split("[,\\s]+").iterator.map(_.trim).filter(_.nonEmpty)
      .toSeq
    if (names.isEmpty)
      Seq(AddIfZero)
    else
      names.map(parseTransform)
  }

  def runOptions(options: Options): Int = {
    logger.info("Robustness mode started")

    val collector = BlameCollector()
    val blameProvider = ConstantBlameProvider(collector)
    val builders = selectedRobustnessPasses
    val repeat = math.max(1, options.robustnessRepeat)
    // SemTransforms-style N-fold apply: cycle the listed transforms across
    // `repeat` applications on the already-resolved AST (no C round-trip).
    val passes = Seq.tabulate(repeat)(i => builders(i % builders.size))
    logger.info(s"Robustness transforms: ${builders.map(_.key).mkString(",")}")
    logger.info(s"Robustness repeat: $repeat")

    vct.col.rewrite.RobustnessSiteSelection.reset()

    // Parse and resolve once, then apply the selected robustness rewrite(s)
    // `repeat` times so later applications see the already-mutated AST.
    // Generated sizeof_* helpers stay in the resolved AST for pointer-size
    // reasoning, but C layout omits them (see Function.isGeneratedSizeOfHelper).
    // Inter-pass typecheck is skipped for repeat > 1: 100 nested wraps would
    // otherwise re-check the same well-formedness 100 times.
    val stages = Parsing.ofOptions[InitialGeneration](options, blameProvider)
      .thenRun(Resolution.ofOptions[InitialGeneration](options, blameProvider))
      .thenRun(new Transformation(
        onPassEvent = Nil,
        passes = passes,
        optimizeUnsafe = options.devUnsafeOptimization || repeat > 1,
      )).thenRun(Output(
        out = Some(outputPath),
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
