package vct.main.modes


/* Robustness, VerCors'un çalışma modlarından biri. */


/* LazyLogging, ekrana/log dosyasına mesaj yazabilmemizi sağlıyor. */
 import com.typesafe.scalalogging.LazyLogging

/*
VerCors bir problem bulduğunda:

  Bu problemin sorumlusu hangi kod / transformation / AST parçası?

gibi bilgileri takip etmek için blame mekanizmasını kullanıyor.

*/

import vct.col.origin.BlameCollector

// Ctx programın hangi syntax ile yazılacağını belirtirken kullanılıyor.
import vct.col.print.Ctx

// sadece aynı package'tan birden fazla şeyi import etmenin yolu.
import vct.col.rewrite.{
  AddIfZero,
  InitialGeneration, // VerCors AST'nin farklı dönüşüm aşamalarını tip seviyesinde takip ediyor.
  // InitialGeneration: VerCors AST’sinin hangi dönüşüm aşamasında olduğunu belirten bir tip.
  // // Henüz ilk/orijinal AST nesilleri üzerinde çalışıyoruz
}

//Program işletim sistemine sonunda bir sayı döndürür.
import vct.main.Main.{EXIT_CODE_ERROR, EXIT_CODE_SUCCESS}
/*
başarılı → success code
başarısız → error code
*/




// Bunlar bizim işlem zincirimizin parçaları.
// Örneğin hangi C dosyasını verdiği gibi bilgiler options içinde bulunuyor.
import vct.main.stages.{
  CSourceTransformation, // C kaynak kodu üzerinde bir rewrite pass çalıştırmak için VerCors’un kullandığı stage.
  Output,
  Parsing,
  Resolution,
}
// Kullanıcının VerCors'u çalıştırırken verdiği seçenekleri temsil ediyor.
import vct.options.Options

// BlameCollector'ı VerCors'un beklediği biçimde transformation aşamalarına vermemizi sağlıyor.
import vct.parsers.transform.ConstantBlameProvider

import vct.result.VerificationError.{SystemError, UserError}
/*
UserError = kullanıcının verdiği program/input ile ilgili normal hata.

SystemError = VerCors'un kendi içinde beklenmeyen ciddi problem.

 */

// Dosya yolu oluşturmak için Java'nın hazır sınıfı.
import java.nio.file.Paths

// object'in Scala'da pattern matching gibi yerlerde kullanımı kolaylaştırılmış hali.
case object Robustness extends LazyLogging {
// Robustness, LazyLogging özelliğini alıyor. O yüzden:
//
//logger.info(...)
//
//kullanabiliyoruz.

  // Bu fonksiyon sonunda bir tam sayı döndürür.
  def runOptions(options: Options): Int = {

    logger.info("Robustness mode started")


    //  collector adında bir nesne(değişken) oluşturuyoruz. val, bunun daha sonra başka bir nesneyle değiştirilmemesi anlamına gelir.
    val collector =
      BlameCollector()


  // collector'ı VerCors pipeline'ının kullanabileceği blameProvider haline getiriyoruz.

    val blameProvider =
      ConstantBlameProvider(
        collector
      )
/*
BlameCollector
      ↓
ConstantBlameProvider
      ↓
VerCors stages

Bu transformation'ın kendisi değil.

VerCors altyapısının ihtiyaç duyduğu hata/konum takip mekanizması.

 */

    // stages adında bir işlem zinciri oluşturuyoruz.
    val stages =
      Parsing  // Kullanıcının verdiği kaynak kodu oku ve VerCors'un anlayabileceği AST yapısına dönüştür.
        // InitialGeneration: VerCors AST’sinin hangi dönüşüm aşamasında olduğunu belirten bir tip.
        .ofOptions[InitialGeneration](  // “Parsing sonucundaki ilk nesil AST ile çalışıyorum.”
          options,
          blameProvider,
        )
        .thenRun( // Önceki işlem bittikten sonra bunu çalıştır.
          CSourceTransformation[ // Bu aşamada C kaynak programına yönelik bir transformation çalıştırıyoruz.
            InitialGeneration
          ](
            blameProvider, // VerCors'un hata/blame altyapısını veriyoruz.
            AddIfZero, // C programının AST'sine AddIfZero rewrite pass'ini uygula.
          )
        )
        .thenRun( // sonra Resolution kabaca programdaki bağlantıları çözüyor.
          Resolution.ofOptions[
            InitialGeneration
          ](
            options,
            blameProvider,
          )
        )
        .thenRun(
          Output(
            out =
              Some( // some= değer var dmek -> belirli bir output dosyam var ve yolu bu.
                Paths.get(
                  "robustness-transformed.c"
                )
              ),
            syntax = Ctx.C, // Çıktının: c olarak yazılmasını söylüyor. Yani AST'nin Scala/debug görünümünü yazmıyoruz. Tekrar C kodu üretiyoruz.
            splitDecls = false, // Declaration'ların çıktı sırasında ayrıca bölünmesini istemiyoruz.
          )
        )

    stages.run( // üstte sadece zinciri tanımlamıştık, Gerçek çalıştırma burada:
      options.inputs // Kullanıcının verdiği input dosyalarını al ve yukarıda hazırladığım pipeline'dan geçir.
    ) match { // Scala'da match, sonuca göre farklı şey yapmaktır.

      case Left(err: UserError) =>  // Kullanıcı/input kaynaklı hata olduysa
        logger.error(
          err.text
        )
        EXIT_CODE_ERROR

      case Left(err: SystemError) => // Eğer VerCors'un kendi içinde ciddi bir hata oluşursa:
        throw err   // Hatayı gizleme; programı bu hatayla durdur.

      case Right(_) =>
        logger.info(
          "Robustness transformation completed"
        )
        logger.info(
          "Output written to robustness-transformed.c"
        )
        EXIT_CODE_SUCCESS
    }
  }
}