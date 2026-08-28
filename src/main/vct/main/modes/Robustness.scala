package vct.main.modes

import com.typesafe.scalalogging.LazyLogging
import vct.col.origin.BlameCollector
import vct.col.print.Ctx
import vct.col.rewrite.{
  AddIfOne,
  AddIfZero,
  InitialGeneration,
  RewriterBuilder,
  RobustnessDeepenWhile,
  RobustnessForLoopToWhileLoop,
  RobustnessToMethod,
  RobustnessSiteSelection,
  RobustnessSpin,
  RobustnessSpinStep,
}
import vct.main.Main.{EXIT_CODE_ERROR, EXIT_CODE_SUCCESS}
import vct.main.stages.{Output, Parsing, Resolution, Transformation}
import vct.options.Options
import vct.parsers.transform.ConstantBlameProvider
import vct.result.VerificationError.{SystemError, UserError}

import java.nio.file.{Path, Paths}
import scala.util.Random

case object Robustness extends LazyLogging {
  private val outputPath: Path = Paths.get("robustness-transformed.c")

  private val spinNames: Set[String] = Set("spin", "spin_config", "spin-config")

  private def parseTransform(name: String): RewriterBuilder =
    name.trim.toLowerCase match {
      case "" | "add-if-zero" | "add_if_zero" | "if0error" =>
        AddIfZero

      case "add-if-one" | "add_if_one" | "add-if1" | "add_if1" =>
        AddIfOne

      case "for-to-while" | "for_to_while" | "for2while" |
          "for-loop-to-while" | "for_loop_to_while" =>
        RobustnessForLoopToWhileLoop

      case "deepen-while" | "deepen_while" | "deepenwhile" =>
        RobustnessDeepenWhile

      case "to-method" | "to_method" | "tomethod" =>
        RobustnessToMethod

      case other =>
        throw new IllegalArgumentException(
          s"Unknown ROBUSTNESS_TRANSFORM=$other. " +
            "Use add-if-zero, add-if-one, for-to-while, deepen-while, or to-method, " +
            "optionally comma-separated, or spin_config for mixed random selection."
        )
    }

  private def envFlag(name: String): Boolean =
    sys.env.get(name).map(_.trim).exists { value =>
      val lower = value.toLowerCase
      lower == "1" || lower == "true" || lower == "yes"
    }

  private def envNames: Seq[String] = {
    val raw = sys.env.get("ROBUSTNESS_TRANSFORM").map(_.trim).filter(_.nonEmpty)
      .getOrElse("")
    raw.split("[,\\s]+").iterator.map(_.trim.toLowerCase).filter(_.nonEmpty)
      .toSeq
  }

  private def selectedBuilders(spin: Boolean): Seq[RewriterBuilder] = {
    val names = envNames.filterNot(spinNames.contains)
    if (names.isEmpty) {
      if (spin)
        Seq(
          AddIfZero,
          AddIfOne,
          RobustnessForLoopToWhileLoop,
          RobustnessDeepenWhile,
        )
      else
        Seq(AddIfZero)
    } else {
      names.map(parseTransform)
    }
  }

  private def spinEnabled(options: Options): Boolean =
    options.robustnessSpin || envFlag("ROBUSTNESS_SPIN") ||
    envNames.exists(spinNames.contains)

  private def spinSeed(options: Options): Long =
    options.robustnessSeed.orElse {
      sys.env.get("ROBUSTNESS_SEED").map(_.trim).filter(_.nonEmpty).map { raw =>
        try { raw.toLong }
        catch {
          case _: NumberFormatException =>
            throw new IllegalArgumentException(
              s"ROBUSTNESS_SEED must be a Long, got: $raw"
            )
        }
      }
    }.getOrElse(Random.nextLong())

  def runOptions(options: Options): Int = {
    logger.info("Robustness mode started")

    val collector = BlameCollector()
    val blameProvider = ConstantBlameProvider(collector)
    val spin = spinEnabled(options)
    val builders = selectedBuilders(spin)
    val repeat = math.max(1, options.robustnessRepeat)

    RobustnessSiteSelection.reset()

    val passes =
      if (spin) {
        val seed = spinSeed(options)
        RobustnessSpin.reset(seed, builders.map(RobustnessSpin.kindOf))
        logger.info("Robustness mode: spin (random kind + random site)")
        logger.info(s"Robustness spin seed: $seed")
        logger.info(
          s"Robustness spin pool: ${builders.map(_.key).mkString(",")}"
        )
        Seq.fill(repeat)(RobustnessSpinStep)
      } else {
        // Same-kind (or cycling) N-fold apply on the resolved AST, site 0
        // unless ADD_IF_ZERO_SITE / ADD_IF_ONE_SITE / FOR_TO_WHILE_SITE is set.
        logger.info("Robustness mode: sequential")
        Seq.tabulate(repeat)(i => builders(i % builders.size))
      }

    logger.info(s"Robustness transforms: ${builders.map(_.key).mkString(",")}")
    logger.info(s"Robustness repeat: $repeat")

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
        if (spin) {
          val trace = RobustnessSpin.trace
          if (trace.isEmpty)
            logger.info("Robustness spin trace: (empty)")
          else
            logger.info("Robustness spin trace:\n" + trace.mkString("\n"))
        }
        logger.info("Robustness transformation completed")
        logger.info("Output written to robustness-transformed.c")
        EXIT_CODE_SUCCESS
    }
  }
}
