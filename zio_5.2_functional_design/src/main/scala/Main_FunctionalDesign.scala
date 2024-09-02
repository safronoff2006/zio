
case class Email( subject: String = "",to: String = "", from: String = "", body: String = "", favorite: Boolean = false)

object Main_FunctionalDesign extends App {



  println("Функциональный дизайн. Исполняемое кодирование")
  import Execution._
  val email1 = Email("нет")
  val email2 = Email("да")
  val email3 = Email("работа")

  val filter1: Execution.EmailFilter = EmailFilter.subjectContains("работа")

   println(filter1.matches(email1))
   println(filter1.matches(email2))
   println(filter1.matches(email3))

    println("------")
   val list1 = List(Email("отдых"), Email("работа","","","", true), Email("отпуск"), Email("бестолковая работа"), Email("нет"))
   list1.filter(filter1.matches(_)).foreach(println)
  println("------")
  val filter2 = filter1 || EmailFilter.subjectContains("нет")
  list1.filter(filter2.matches(_)).foreach(println)
  println("------")
  val filter3 =  EmailFilter.subjectContains("да") || EmailFilter.subjectContains("нет")
  list1.filter(filter3.matches(_)).foreach(println)
  println("------")
  val filter4 = ! EmailFilter.subjectContains("нет")
  list1.filter(filter4.matches(_)).foreach(println)
  println("------")
  val filter5 = filter1 || EmailFilter.favorite
  list1.filter(filter5.matches(_)).foreach(println)
  println("------")
  val filter6 = ! EmailFilter.favorite
  list1.filter(filter6.matches(_)).foreach(println)
  println("--------------------------")

  println("Функциональный дизайн. Декларативное кодирование")
  println("--------------------------")

  import Declarative._
  val dfilter1 = Declarative.subjectContains("работа")
  list1.filter(email => matches(dfilter1, email)).foreach(println)
  println("------")
  list1.filter(email => matches(dfilter1 ||  Declarative.subjectContains("нет") , email)).foreach(println)
  println("------")
  list1.filter(email => matches( Declarative.favorite , email)).foreach(println)

}



object Execution {
  final case class EmailFilter(matches: Email => Boolean) { self =>
    def &&(that: EmailFilter): EmailFilter =
      EmailFilter(email => self.matches(email) && that.matches(email))

    def ||(that: EmailFilter): EmailFilter =
      EmailFilter(email => self.matches(email) || that.matches(email))

    def unary_! : EmailFilter =
      EmailFilter(email => !self.matches(email))

  }


  object EmailFilter {
    def subjectContains(phrase: String): EmailFilter =
      EmailFilter(_.subject.contains(phrase))
    def favorite: EmailFilter = EmailFilter(_.favorite)
  }

}

object Declarative {

  sealed trait EmailFilter { self =>
    def &&(that: EmailFilter): EmailFilter = And(self, that)

    def ||(that: EmailFilter): EmailFilter = Or(self, that)

    def unary_! : EmailFilter = Not(self)

  }


  final case class SubjectContains(phrase: String) extends EmailFilter
  final case object  Favorite extends EmailFilter
  final case class And(left: EmailFilter, right: EmailFilter) extends EmailFilter
  final case class Or(left: EmailFilter, right: EmailFilter) extends EmailFilter
  final case class Not(value: EmailFilter) extends EmailFilter

  def subjectContains(phrase: String): EmailFilter =
    SubjectContains(phrase)

  def favorite: EmailFilter = Favorite

  def matches(filter: EmailFilter, email: Email): Boolean =
    filter match {
      case And(l, r) => matches(l, email) && matches(r, email)
      case Or(l, r) => matches(l, email) || matches(r, email)
      case Not(v) => !matches(v, email)
      case SubjectContains(phrase) => email.subject.contains(phrase)
      case Favorite => email.favorite
    }
}