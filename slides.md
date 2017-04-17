<!--

###
# 
# © Copyright 2017 Greg Symons <gsymons@gsconsulting.biz>.
#
# Made available under a Creative Commons Attribution-ShareAlike 4.0 
# International License: http://creativecommons.org/licenses/by-sa/4.0/
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations under
# the License.
#
###

-->

Property Based Testing
======================

---

What is Property Based Testing?
-------------------------------

^^^

A generative testing technique where statements (known as "properties") about 
the behavior of the system under test are asserted against many randomly 
generated inputs. Failing test cases are then minimized (or "shrunk") in order
to find a minimal input set that causes the failure.


^^^

### Examples

- QuickCheck (Haskell)          <!-- .element: class="fragment" -->
- Quvik QuickCheck (Erlang)     <!-- .element: class="fragment" -->
- clojure/test.check (Clojure)  <!-- .element: class="fragment" -->
- JSVerify (Javascript)         <!-- .element: class="fragment" -->
- ScalaCheck (Scala)            <!-- .element: class="fragment" -->
- And many more: <!-- .element: class="fragment" --> https://en.wikipedia.org/wiki/QuickCheck <!-- .element: class="fragment" -->


---

A Simple Example
----------------

```scala
//Tricks the REPL into letting us 
//define the companion for Stack
object Stacks {
  sealed trait StackLike[+T] {}
  case object Empty extends StackLike[Nothing]
  case class Stack[+T](value: T, 
      depth: Short,
      next: StackLike[T]) extends StackLike[T]

  object Stack {
    def empty[T]: StackLike[T] = Empty
  }
}
```

^^^
### Let's define some operations on Stacks...

```scala
import Stacks._

def push[T](s: StackLike[T])(v: T): StackLike[T] = ???
def pop[T](s: StackLike[T]): (StackLike[T], Option[T]) = ???
def depth[T](s: StackLike[T]): Int = ???
```

^^^
### A First Property

```scala
import org.scalacheck._
import Prop.forAll

val push_empty = forAll { (value: Int) =>
  depth(push(Empty)(value)) == 1
}
```

^^^

```scala
scala> push_empty.check
! Exception raised on property evaluation.
> ARG_0: 0
> ARG_0_ORIGINAL: -1185386442
> Exception: scala.NotImplementedError: an implementation is missing
scala.Predef$.$qmark$qmark$qmark(Predef.scala:252)
$line3.$read$$iw$$iw$$iw$$iw$.push(<console>:13)
$line20.$read$$iw$$iw$$iw$$iw$$iw$$iw$$anonfun$1.apply$mcZI$sp(<console>:20
  )
$line20.$read$$iw$$iw$$iw$$iw$$iw$$iw$$anonfun$1.apply(<console>:19)
$line20.$read$$iw$$iw$$iw$$iw$$iw$$iw$$anonfun$1.apply(<console>:19)
```

^^^
### A Possible Implementation?

```scala
def depth[T](s: StackLike[T]): Int = 1
def push[T](s: StackLike[T])(value: T): StackLike[T] = s
```




^^^
### Well, it does have the desired property

```scala
scala> push_empty.check
+ OK, passed 100 tests.
```

^^^

### Perhaps some additional properties?

```scala
def push_then_pop_on_empty_has_depth_0 = forAll { (v: Int) =>
  val s1 = push(Empty)(v)
  val (s2, _) = pop(s1)
  depth(s2) == 0
}

def push_then_pop_returns_the_value = forAll { (v: Int) =>
  val s1 = push(Empty)(v)
  val (_, popped) = pop(s1)
  popped.nonEmpty && popped.filter(_ == v).nonEmpty
}
```

^^^


```scala
scala> push_empty.check
+ OK, passed 100 tests.

scala> push_then_pop_on_empty_has_depth_0.check
! Exception raised on property evaluation.
> ARG_0: 0
> ARG_0_ORIGINAL: 1
> Exception: scala.NotImplementedError: an implementation is missing
scala.Predef$.$qmark$qmark$qmark(Predef.scala:252)
$line7.$read$$iw$$iw$$iw$$iw$.pop(<console>:12)
$line26.$read$$iw$$iw$$iw$$iw$$iw$$iw$$anonfun$push_then_pop_on_empty_has_d
  epth_0$1.apply$mcZI$sp(<console>:21)
$line26.$read$$iw$$iw$$iw$$iw$$iw$$iw$$anonfun$push_then_pop_on_empty_has_d
  epth_0$1.apply(<console>:19)
$line26.$read$$iw$$iw$$iw$$iw$$iw$$iw$$anonfun$push_then_pop_on_empty_has_d
  epth_0$1.apply(<console>:19)

scala> push_then_pop_returns_the_value.check
! Exception raised on property evaluation.
> ARG_0: 0
> ARG_0_ORIGINAL: -2147483648
> Exception: scala.NotImplementedError: an implementation is missing
scala.Predef$.$qmark$qmark$qmark(Predef.scala:252)
$line7.$read$$iw$$iw$$iw$$iw$.pop(<console>:12)
$line27.$read$$iw$$iw$$iw$$iw$$iw$$iw$$anonfun$push_then_pop_returns_the_va
  lue$1.apply$mcZI$sp(<console>:21)
$line27.$read$$iw$$iw$$iw$$iw$$iw$$iw$$anonfun$push_then_pop_returns_the_va
  lue$1.apply(<console>:19)
$line27.$read$$iw$$iw$$iw$$iw$$iw$$iw$$anonfun$push_then_pop_returns_the_va
  lue$1.apply(<console>:19)
```

### And now we have to implement pop

^^^

```scala
def pop[T](s: StackLike[T]): (StackLike[T], Option[T]) = {
  s match {
    case Empty => (Empty, None)
    case Stack(value, _, next) => (next, Some(value))
  }
}
```

^^^
### Ooops... 

Curse those lazy implementations of `push` and `depth`!




```scala
scala> push_empty.check
+ OK, passed 100 tests.

scala> push_then_pop_on_empty_has_depth_0.check
! Falsified after 0 passed tests.
> ARG_0: 0
> ARG_0_ORIGINAL: -479500829

scala> push_then_pop_returns_the_value.check
! Falsified after 0 passed tests.
> ARG_0: 0
> ARG_0_ORIGINAL: 2063615216
```

^^^
```scala

def push[T](s: StackLike[T])(v: T): StackLike[T] = {
  s match {
    case Empty => Stack(v, 1.toShort, Empty)
    case Stack(_, depth, _) => Stack(v, (1.toShort + depth).toShort, s)
  }
}

def depth[T](s: StackLike[T]): Short = {
  s match {
    case Empty => 0.toShort
    case Stack(_, depth, _) => depth
  }
}
```

^^^
### And now our Stack seems to work. 

Or does it?




```scala
scala> push_empty.check
+ OK, passed 100 tests.

scala> push_then_pop_on_empty_has_depth_0.check
+ OK, passed 100 tests.

scala> push_then_pop_returns_the_value.check
+ OK, passed 100 tests.
```

---

Custom Generators
-----------------

^^^
### Let's look at that Stack more closely...

<pre><code data-noescape>
case class Stack[+T](value: T, 
                     <mark>depth: Short,</mark>
                     next: StackLike[T]) extends StackLike[T]
</code></pre>

^^^

```scala
val largeInts = Gen.choose(Short.MaxValue - 10, Short.MaxValue + 10)

val stack_supports_large_lists = forAll (largeInts) { sz =>
  val r = 0 to sz
  val s = r.foldLeft(Stack.empty[Int])(push(_)(_))
  depth(s) == r.size
}
```

^^^
### Yep, that's an overflow!

```scala
scala> stack_supports_large_lists.check
! Falsified after 0 passed tests.
> ARG_0: 32774
```

^^^
### Let's fix that
```scala
object Stacks {
  case class Stack[+T](value: T,
    depth: Int,
    next: StackLike[T]) extends StackLike[T]

  object Stack {
    def empty[T]: StackLike[T] = Empty
  }
}

import Stacks._

def push[T](s: StackLike[T])(v: T): StackLike[T] = {
  s match {
    case Empty => Stack(v, 1, Empty)
    case Stack(_, depth, _) => Stack(v, depth + 1, s)
  }
}

def depth[T](s: StackLike[T]): Int = {
  s match {
    case Empty => 0
    case Stack(_, depth, _) => depth
  }
}
```

^^^



```scala
scala> stack_supports_large_lists.check
+ OK, passed 100 tests.
```

---

What Else Can We Generate?
--------------------------

^^^
### People




```scala
case class Person(firstName: String, lastName: String, age: Int)

val YoungPeople = for {
  firstName <- Gen.oneOf(firstNames)
  lastName  <- Gen.oneOf(surnames)
  age       <- Gen.choose(18, 34)
} yield Person(firstName, lastName, age)
```

^^^


```scala
scala> (0 to 10).foreach(_ => println(YoungPeople.sample))
Some(Person(LEWIS,FUGUA,30))
Some(Person(RASHAD,PETRULIS,18))
Some(Person(TONYA,GRIESE,32))
Some(Person(DELENA,KEWANWYTEWA,29))
Some(Person(PATRICK,CODELUPPI,30))
Some(Person(CLAUDE,TERHAR,20))
Some(Person(ELENA,WANG,32))
Some(Person(RETHA,CERAVOLO,23))
Some(Person(JEN,TURI,29))
Some(Person(JALEESA,REITMAN,19))
Some(Person(BOBBI,STRATMAN,29))
```

^^^
#### And we can make them available to properties:

```scala
implicit val ArbYoungPerson = Arbitrary(YoungPeople)

val allPeopleAreYoungPeople = forAll { p: Person =>
  p.age >= 18 && p.age < 34
}
```

^^^
```scala
scala> allPeopleAreYoungPeople.check
! Falsified after 57 passed tests.
> ARG_0: Person(MARCHELLE,PYWELL,34)
```

---

What Other Interesting Things Can We Do?
----------------------------------------

^^^

### Characterizing the test data

```scala
import Prop.collect

val firstNamesByGender = firstNamesMale.foldLeft(
  Map.empty[String, String]) { (m, n) =>
    m.updated(n, "male")
  } ++ firstNamesFemale.foldLeft(
  Map.empty[String, String]) { (m, n) =>
    m.updated(n, "female")
  }

val allPeopleAreYoungPeople = forAll { p: Person =>
  collect(firstNamesByGender(p.firstName)) {
    p.age >= 18 && p.age <= 34
  }
}
```

^^^


```scala
scala> allPeopleAreYoungPeople.check
+ OK, passed 100 tests.
> Collected test data: 
81% female
19% male
```

^^^
### Testing Third Party Code

- AUTOSAR
- JodaTime
- Aeson

^^^
### Testing *Systems*

- Dropbox
- Selenium
- Jepsen
- Generating VMs using Nix and Scalacheck


---

References
----------
* Gilmore-Innis, Kelsey. "I Dream of Gen'ning: ScalaCheck Beyond The Basics".
  Scala By The Bay 2014. https://youtu.be/lgyGFG6hBa0. Retrieved April 15, 2017.
^^^
* Hughes, John. "Testing the Hard Stuff and Staying Sane". Clojure/West 2014. 
  https://www.youtube.com/watch?v=zi0rHwfiX1Q. Retrieved April 16, 2017.
^^^
* Nilsson, Rickard. "Testing Stateful Systems with ScalaCheck". Scala Days Berlin 2014. 
  https://www.youtube.com/watch?v=Yg660RrAt2I. Retrieved April 16, 2017.
^^^
* O'Farrell, Charles. "Practical Property-Based Testing". YOW! Lambda Jam 2015.
  https://yow.eventer.com/yow-lambda-jam-2015-1305/practical-property-based-testing-by-charles-o-farrell-1884.
  Retrieved April 16, 2017.

---

Q&amp;A
-------
