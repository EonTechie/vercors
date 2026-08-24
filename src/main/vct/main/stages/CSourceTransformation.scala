
// // Bu dosya transformation algoritması değil; transformation'ı VerCors pipeline'ına bağlayan stage.
package vct.main.stages

// Stage = VerCors pipeline'ındaki işlem adımlarının kullandığı hazır altyapı.
import hre.stages.Stage

import vct.col.ast.Program

import vct.col.origin.FileSpanningOrigin // “Bu oluşturduğum node'u dosyanın genelini kapsayan bir source origin ile ilişkilendir.”

import vct.col.print.Ctx // Ctx VerCors'un AST'yi yazdırırken hangi syntax'ı kullanacağını belirleyen context'i.

import vct.col.rewrite.{
  CStatementPassBuilder,  // bizim oluşturduğumuz interface.  ->  Transformation'ların ortak biçimde stage'e verilebilmesini sağlıyor.
  Generation,  // Generation ise AST type sisteminin kullandığı tür.
}

// // ParseResult = parser'ın sonucu; declaration'ları ve parsing sırasında taşınan ek bilgileri içerir.
import vct.parsers.ParseResult


import vct.parsers.transform.BlameProvider

import java.nio.charset.StandardCharsets
import java.nio.file.{Files, Paths}
/*
Paths → output dosya yolunu oluşturmak
Files → dosya writer'ı açmak
StandardCharsets.UTF_8 → UTF-8 encoding
 */

// Bu bizim yazdığımız stage class'ı.
case class CSourceTransformation[
  G <: Generation
](  // Bu stage oluşturulurken üç bilgi verilebilir.
   blameProvider: BlameProvider,  // Program oluştururken lazım.
   pass: CStatementPassBuilder,  // “Hangi transformation'ı çalıştıracaksın?”
   repeat: Int = 1,

    // // Bu Stage ParseResult alır, içindeki AST'yi değiştirir ve yine ParseResult döndürür; pipeline tipi bozulmaz.
 ) extends Stage[
  ParseResult[G],
  ParseResult[G],
] {
/*
pass = AddIfZero

olabilir.

Ama class AddIfZero'ya özel değil.
// pass = bu stage'in çalıştıracağı transformation builder'ı; AddIfZero olmak zorunda değil.
 */

  // friendlyName = pipeline/loglarda bu stage'in okunabilir adı; pass.key hangi transformation olduğunu ekler.
  override def friendlyName =
    s"C Source Transformation: ${pass.key}"

  // progressWeight = Stage altyapısının ilerleme hesabı için kullandığı ağırlık; transformation mantığı değildir.

  override def progressWeight: Int =
    1

  // run = stage'in gerçek çalışma noktası; parser sonucunu alır, AST'yi dönüştürür ve yeni ParseResult döndürür.
  override def run(
                    in: ParseResult[G]
                  ): ParseResult[G] = {


    // implicit o = yeni AST node'larının istediği Origin'i Scala'nın otomatik sağlayabilmesi için FileSpanningOrigin'i context'e koyar.
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
        sys.env.getOrElse(
          "ROBUSTNESS_OUTPUT",
          "robustness-source.c",
        )
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

    } finally writer.close()

    println(
      "[Robustness] transformed C written to robustness-source.c"
    )

    ParseResult(
      rewritten.declarations,
      in.expectedErrors,
    )
  }
}