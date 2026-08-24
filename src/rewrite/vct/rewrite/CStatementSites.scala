package vct.col.rewrite

// ArrayBuffer = içine sonradan eleman ekleyebildiğimiz liste.
// Biz candidate site’ları buldukça buna ekliyoruz.
import scala.collection.mutable.ArrayBuffer

import vct.col.ast._
//VerCors AST sınıflarını kullanabilmek için:
/*
Statement
Branch
Loop
Return
Eval
Scope
Block
...
 */

import vct.col.resolve.lang.C
// C fonksiyonunun adını AST’den çıkarmak için kullanılıyor.

// Tek bir CStatementSites nesnesi var. CStatementSites.collect(program) : Bu dosyanın içindeki fonksiyonları buradan çağırıyoruz:
object CStatementSites {

  // Bir candidate statement'ın programdaki görevi/konumu ne? Bunu temsil ediyor.
  sealed trait Role { // trait, ortak özellik tanımlamak için kullanılıyor.
    def label: String   // Burada her Role bir yazı etiketi vermek zorunda.
  }

  // Bir statement bir block’un içindeyse:
  final case class BlockItem(index: Int) extends Role {
    override def label: String =
      s"BLOCK_ITEM[$index]" // s"..." Scala'da string içine değişken koymak demek:
  }

  // Bu da ifin hangi kolunda olduğumuzu söylüyor.
  final case class BranchArm(index: Int) extends Role {
    override def label: String =
      if (index == 0)
        "IF_TRUE"
      else
        s"IF_ELSE[$index]"
  }

  // Candidate loop'un body’siyse: LOOP_BODY   //etiketini veriyoruz.
  case object LoopBody extends Role {
    override def label: String =
      "LOOP_BODY"
  }

  final case class Site[G](
                            functionName: String,  // hangi fonksiyon?
                            path: String,  // AST içinde nerede?
                            role: Role, // ne tür pozisyon?
                            target: Statement[G],
                          ) {
/*
def description: String =  Candidate hakkında okunabilir açıklama üretir.
ExpressionStatement
IfStatement
LoopStatement
 */
    def description: String =  // Candidate hakkında okunabilir açıklama üretir.
      describe(target)   // asıl AST statement nesnesi.  -> AddIfZero daha sonra tam olarak bunu değiştirecek.
  }

  // Bu fonksiyon bazı özel fonksiyonlara transformation uygulanmasını engelliyor.
  // Çünkü bunlar test/verification altyapısının özel fonksiyonları.
  private def blacklistedFunction(name: String): Boolean =
    name == "reach_error" ||
      name == "abort" ||
      name == "assume_abort_if_not" ||
      name.startsWith("__VERIFIER_")

  /**
   * This is deliberately a whitelist.
   *
   * We do NOT say:
   *
   *     every Statement is selectable.
   *
   * We only accept shapes that CToCol produces for ordinary
   * executable C source statements.
   *
   *
   * Sorusu:
   *
   * Bu statement candidate olabilir mi?
   *
   * Sonuç:
   *
   * true  → candidate olabilir
   * false → olamaz
   */
  private def selectable[G](
                             stat: Statement[G]
                           ): Boolean =
    stat match {  // Statement’ın türüne bakıyoruz. Nesnenin kendisiyle ilgilenmiyorum, sadece türünün Return olup olmadığına bakıyorum.

      // Supervisor requirement:
      // Return is never an AddIfOne/AddIfZero location. - return is a type here
      case _: Return[G] =>
        false

        // SemTransformers add_if1 explicitly excludes Decl.
      case _: CDeclarationStatement[G] =>  // mesela int x;
        false

        // VerCors specification/internal statements must not become
        // C source transformation locations.
      case _: NonExecutableStatement[G] =>  // VerCors annotation/specification gibi source C statement olmayan şeyler candidate değil.
        false

        // C expression statement:
        // x = 1;
        // foo();
        // i++;
      case _: Eval[G] =>
        true  // candidate.

        // C if statement.
      case _: Branch[G] =>
        true

        // A parsed C compound statement:
        //
        // { ... }
        //
        // CToCol represents it exactly as Scope(Nil, Block(...)).
      case Scope(Nil, Block(_)) =>
        true

        // A parsed C while/for statement:
        //
        // CToCol wraps Loop in Scope.
      case Scope(
        Nil,
        Loop(_, _, _, _, _)
      ) =>
        true
/*
Loop
case Scope(
  Nil,
  Loop(_, _, _, _, _)
) =>
  true

C'deki:

while (...)

veya:

for (...)

VerCors'ta Scope + Loop olarak temsil ediliyor.

Candidate.

Buradaki _:

Bu alanın değeriyle ilgilenmiyorum.
 */
        // Named C goto.
      case _: CGoto[G] =>
        true



        // These remain ordinary source statements.
      case _: Break[G] =>
        true

      case _: Continue[G] =>
        true

        /*
         * Intentionally no generic:
         *
         *   case _: Statement => true
         *
         * Anything not proven to represent a supported C source
         * statement is rejected.
         */
      case _ =>
        false
    }
/*
Scope(Nil, Block(_)) -> = “locals listesi boş olan ve gövdesi Block olan Scope”.
Çünkü VerCors’un C parser’ından gelen şu C kodu:

{
    x++;
    y++;
}

bizim burada ilgilendiğimiz parsed C AST biçiminde kabaca:

Scope
├── locals = []
└── Block
    ├── x++
    └── y++
 */
  private def describe[G]( // Sadece candidate'ı terminalde daha anlaşılır göstermek için açıklıyor.
                           stat: Statement[G]
                         ): String =
    stat match {

      case Scope(Nil, Block(stats)) => // Block içinde 3 statement varsa: Block içinde 3 statement varsa:
        s"CompoundStatement(items=${stats.size})"

      case Scope(
        Nil,
        Loop(init, cond, update, _, body)
      ) =>
        s"LoopStatement(" +
          s"init=${init.getClass.getSimpleName}, " +
          s"cond=${cond.getClass.getSimpleName}, " +
          s"update=${update.getClass.getSimpleName}, " +
          s"body=${body.getClass.getSimpleName})"

      case Branch(branches) =>
        s"IfStatement(arms=${branches.size})"

      case Eval(expr) =>
        s"ExpressionStatement(expr=${expr.getClass.getSimpleName})"

      case _: CGoto[G] =>
        "GotoStatement"

      case _: Break[G] =>
        "BreakStatement"

      case _: Continue[G] =>
        "ContinueStatement"

      case _: Return[G] =>
        "ReturnStatement"

      case _: CDeclarationStatement[G] =>
        "DeclarationStatement"

      case other =>
        other.getClass.getSimpleName
    }


/*
Şimdi asıl candidate toplama başlıyor:

def collect[G](
  program: Program[G]
): Seq[Site[G]] = {

Girdi:

bütün program AST

Çıktı:

candidate Site listesi
 */
  def collect[G](
                  program: Program[G]
                ): Seq[Site[G]] = {

    val result =
      ArrayBuffer.empty[Site[G]]
// Başlangıçta boş candidate listesi: []
// Bir statement bulduğumuzda bu fonksiyona gönderiyoruz.
    def add(
             functionName: String,
             path: String,
             role: Role,
             target: Statement[G],
           ): Unit = {

      if (selectable(target)) {
        result +=
          Site(
            functionName,
            path,
            role,
            target,
          )
      }
    }

    /*
     * Important:
     *
     * This traversal is NOT generic Node.subnodes traversal.
     *
     * It explicitly follows exactly the structural statement
     * positions corresponding to SemTransformers FindStatements.
     */

    // AST'nin statement yapısında aşağı doğru gez ve candidate noktaları bul. Sadece bizim açıkça belirlediğimiz yapıları takip ediyor.
    def descend(
                 functionName: String,
                 path: String,
                 stat: Statement[G],
               ): Unit =
      stat match {

        /*
         * C compound statement:
         *
         * {
         *   statement0;
         *   statement1;
         * }
         *
         * Each direct block item is a possible location.
         */
        case Scope(
          Nil,
          Block(statements)
        ) =>

          statements.zipWithIndex.foreach {
            case (child, index) =>

              val childPath =
                s"$path.block[$index]"  // absolute.body.block[0] mesela

              add(
                functionName,
                childPath,
                BlockItem(index),
                child,
              )

              descend(
                functionName,
                childPath,
                child,
              )
          }

          /*
           * CToCol's wrapper around while/for.
           *
           * The Scope itself represents the source loop statement.
           * The Loop node is an internal representation.
           */
        case Scope(
          Nil,
          loop @ Loop(_, _, _, _, _)  // Bunun Loop olduğunu kontrol et ve aynı nesneye loop adını ver.
        ) =>

          descend(
            functionName,
            s"$path.loop",
            loop,  // loop'un içine giriyoruz.
          )

          /*
           * IF:
           *
           * SemTransformers treats iftrue and iffalse as statement
           * locations.
           *
           * Conditions are NOT traversed as statement locations.
           */
        case Branch(branches) =>
          // Bir if gördük.
          //
          //Önce if'in doğrudan body'lerini candidate olarak kaydediyoruz.

          // First register the direct arms.
          // This mirrors FindStatements ordering.
          branches.zipWithIndex.foreach { // Önce if'in doğrudan body'lerini candidate olarak kaydediyoruz.
            case ((_, body), index) =>  // (condition, body) Ama condition ile ilgilenmiyoruz:  -> body'yi alıyoruz.

              add(  // if'in body’sini candidate yapmaya çalışıyor.
                functionName,
                s"$path.branch[$index]",
                BranchArm(index),
                body,
              )
          }

          // Then inspect structures inside the arms.
          branches.zipWithIndex.foreach {
            case ((_, body), index) =>

              descend(
                functionName,
                s"$path.branch[$index]",
                body,
              )
          }
/*
Neden iki kere?

İlk tur:

Doğrudan branch bodylerini candidate listesine ekle.

İkinci tur:

Branch bodylerinin içine gir ve içerideki candidate’ları da bul.

Bu ordering SemTransformers davranışını taklidi.
 */
          /*
           * LOOP:
           *
           * ONLY body is a source-level statement location.
           *
           * init   -> deliberately ignored
           * cond   -> deliberately ignored
           * update -> deliberately ignored
           * contract -> deliberately ignored
           */
        case Loop(
          _,
          _,
          _,
          _,
          body
        ) =>

          val bodyPath =
            s"$path.body"

          add(  // oop body’sini candidate yapıyor.
            functionName,
            bodyPath,
            LoopBody,
            body,
          )

          descend( // body'nin içine de giriyor.
            functionName,
            bodyPath,
            body,
          )

          /*
           * Do not make Label.inner itself a location merely because
           * it is a Label child.
           *
           * But structures nested below it may still contain legal
           * sites.
           */

          /*
          Label'ın kendisini candidate yapmıyoruz.

          Ama içindeki statement yapısında candidate olabilir.
           */
        case Label(_, inner, _) =>

          descend(
            functionName,
            s"$path.labelBody",
            inner,
          )

          /*
           * Eval, Return, declarations, Break, Continue, etc.
           * contain no further statement positions relevant here.
           *
           * Crucially: we do NOT recursively descend through arbitrary
           * Node.subnodes.
           */
        case _ =>  // Yukarıdaki özel durumlara (Scope, Branch, Loop, Label) girmeyen diğer tüm statement türlerinde aşağı doğru gezme. HİÇBİR ŞEY YAPMA DEMEK
      }

    def visitGlobal(
                     decl: GlobalDeclaration[G]
                   ): Unit =
      decl match {

        case unit: CTranslationUnit[G] =>
          unit.declarations.foreach(visitGlobal)  // C dosyasının içindeki bütün declaration'ları tek tek gez.

        case function: CFunctionDefinition[G] =>  // diyelim Bir C fonksiyonu bulduk. Fonksiyon adını çıkar:

          val functionName =
            C
              .getDeclaratorInfo(
                function.declarator
              )
              .name

          if (!blacklistedFunction(functionName)) {  // adı çıkarılan fonksiyon üzerindena blacklist kontrolü:

            /*
             * The function body itself is NOT a FindStatements
             * location.
             *
             * We enter it only to discover its contained
             * statement locations.
             */
            descend(
              functionName,
              s"$functionName.body",  // Fonksiyon body’sinin içine gir ve candidate ara. Fonksiyon body’sinin tamamını candidate yapmıyor. Sadece içine giriyor.
              function.body,
            )
          }
// Bu bizim daha önce crash probleminden kaçınmak için istediğimiz davranış.
        case _ =>  // Diğer globaller Struct, typedef, global variable vs.
      }

    program.declarations.foreach(visitGlobal)  // Burada gerçek tarama başlıyor: Programdaki her global declaration'ı ziyaret et.

    result.toSeq  // ArrayBufferdaki candidate'ları normal bir Seq olarak döndürüyor.
  }
}

/*Program
│
├── struct                 ❌ geç
│
├── global variable        ❌ geç
│
└── function
      │
      └── body
           │
           ├── declaration ❌
           ├── assignment  ✅ candidate
           ├── if          ✅ candidate
           │    ├── true body ✅
           │    └── false body ✅
           │
           ├── loop        ✅
           │    └── body   ✅
           │
           └── return      ❌
 */

