package vct.main.modes

import com.typesafe.scalalogging.LazyLogging
import hre.stages.FunctionStage
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

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Path, Paths}

case object Robustness extends LazyLogging {
  private val outputPath: Path = Paths.get("robustness-transformed.c")
  private val generatedSizeOfPrototype =
    """\bpure\s+int\s+sizeof_[A-Za-z0-9_]*\s*\([^)]*\)\s*;""".r

  private def stripGeneratedSizeOfHelpers(text: String): (String, Int) = {
    val out = new StringBuilder
    var cursor = 0
    var removed = 0

    while (cursor < text.length) {
      val start = text.indexOf("/*@", cursor)
      if (start < 0) {
        out.append(text.substring(cursor))
        cursor = text.length
      } else {
        val end = text.indexOf("@*/", start + 3)
        if (end < 0) {
          out.append(text.substring(cursor))
          cursor = text.length
        } else {
          out.append(text.substring(cursor, start))
          val blockEnd = end + 3
          val block = text.substring(start, blockEnd)

          if (generatedSizeOfPrototype.findFirstIn(block).nonEmpty) {
            removed += 1
            cursor = blockEnd
            while (
              cursor < text.length && text.charAt(cursor).isWhitespace
            )
              cursor += 1
          } else {
            out.append(block)
            cursor = blockEnd
          }
        }
      }
    }

    (out.toString, removed)
  }

  private def filterRobustnessOutput(path: Path): Unit = {
    if (Files.exists(path)) {
      val text = Files.readString(path, StandardCharsets.UTF_8)
      val (filtered, removed) = stripGeneratedSizeOfHelpers(text)
      if (removed > 0) {
        Files.write(path, filtered.getBytes(StandardCharsets.UTF_8))
        logger.info(s"Removed $removed generated sizeof helper(s) from $path")
      }
    }
  }

  private def selectedRobustnessPass: RewriterBuilder =
    sys.env.get("ROBUSTNESS_TRANSFORM").map(_.trim.toLowerCase) match {
      case None | Some("") | Some("add-if-zero") | Some("add_if_zero") |
          Some("if0error") =>
        AddIfZero

      case Some("add-if-one") | Some("add_if_one") | Some("add-if1") |
          Some("add_if1") =>
        AddIfOne

      case Some("for-to-while") | Some("for_to_while") | Some("for2while") |
          Some("for-loop-to-while") | Some("for_loop_to_while") =>
        RobustnessForLoopToWhileLoop

      case Some(other) =>
        throw new IllegalArgumentException(
          s"Unknown ROBUSTNESS_TRANSFORM=$other. " +
            "Use add-if-zero, add-if-one, or for-to-while."
        )
    }

  def runOptions(options: Options): Int = {
    logger.info("Robustness mode started")

    val collector = BlameCollector()
    val blameProvider = ConstantBlameProvider(collector)
    val robustnessPass = selectedRobustnessPass
    logger.info(s"Robustness transform: ${robustnessPass.key}")

    // Adjusted for robustness mode: run AddIfZero after resolution, then emit
    // the selected post-resolution mutant as C for re-verification.
    val stages = Parsing.ofOptions[InitialGeneration](options, blameProvider)
      .thenRun(Resolution.ofOptions[InitialGeneration](options, blameProvider))
      .thenRun(new Transformation(
        onPassEvent = Nil,
        passes = Seq(robustnessPass),
        optimizeUnsafe = options.devUnsafeOptimization,
      )).thenRun(Output(
        out = Some(outputPath),
        syntax = Ctx.C,
        splitDecls = false,
      )).thenRun(FunctionStage((_: Seq[hre.io.LiteralReadable]) =>
        filterRobustnessOutput(outputPath)
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
