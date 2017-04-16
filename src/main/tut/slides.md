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

```tut:silent
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

```tut:silent
import Stacks._

def push[T](s: StackLike[T])(v: T): StackLike[T] = ???
def pop[T](s: StackLike[T]): (StackLike[T], Option[T]) = ???
def depth[T](s: StackLike[T]): Int = ???
```

^^^
### A First Property

```tut:silent
import org.scalacheck._
import Prop.forAll

val push_empty = forAll { (value: Int) =>
  depth(push(Empty)(value)) == 1
}
```

^^^

```tut
push_empty.check
```

^^^
### A Possible Implementation?

```tut:silent
def depth[T](s: StackLike[T]): Int = 1
def push[T](s: StackLike[T])(value: T): StackLike[T] = s
```

```tut:invisible
val push_empty = forAll { (value: Int) =>
  depth(push(Empty)(value)) == 1
}
```

^^^
### Well, it does have the desired property

```tut
push_empty.check
```

^^^

### Perhaps some additional properties?

```tut:silent
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


```tut
push_empty.check
push_then_pop_on_empty_has_depth_0.check
push_then_pop_returns_the_value.check
```

### And now we have to implement pop

^^^

```tut:silent
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

```tut:invisible
val push_empty = forAll { (value: Int) =>
  depth(push(Empty)(value)) == 1
}

def push_then_pop_on_empty_has_depth_0 = forAll { (v: Int) =>
  val s1 = push(Empty)(v)
  val (s2, _) = pop(s1)
  depth(s2) == 0
}

def push_then_pop_returns_the_value = forAll { (v: Int) =>
  val s1 = push(Empty)(v)
  val (_, popped) = pop(s1)
  popped.filter(_ == v).nonEmpty
}
```

```tut
push_empty.check
push_then_pop_on_empty_has_depth_0.check
push_then_pop_returns_the_value.check
```

^^^
```tut:silent

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

```tut:invisible
val push_empty = forAll { (value: Int) =>
  depth(push(Empty)(value)) == 1
}

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

```tut
push_empty.check
push_then_pop_on_empty_has_depth_0.check
push_then_pop_returns_the_value.check

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

```tut:silent
val largeInts = Gen.choose(Short.MaxValue - 10, Short.MaxValue + 10)

val stack_supports_large_lists = forAll (largeInts) { sz =>
  val r = 0 to sz
  val s = r.foldLeft(Stack.empty[Int])(push(_)(_))
  depth(s) == r.size
}
```

^^^
### Yep, that's an overflow!

```tut
stack_supports_large_lists.check
```

^^^
### Let's fix that
```tut:silent
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
```tut:invisible
val largeInts = Gen.choose(Short.MaxValue - 10, Short.MaxValue + 10)

val stack_supports_large_lists = forAll (largeInts) { sz =>
  val r = 0 to sz
  val s = r.foldLeft(Stack.empty[Int])(push(_)(_))
  depth(s) == r.size
}
```

```tut
stack_supports_large_lists.check
```

---

What Else Can We Generate?
--------------------------

^^^
### People

```tut:invisible
import scala.io.Source

val census90MaleFirstNames = "https://www2.census.gov/topics/genealogy/1990surnames/dist.male.first"
val census90FemaleFirstNames = "https://www2.census.gov/topics/genealogy/1990surnames/dist.female.first"
val census90surnames = "https://www2.census.gov/topics/genealogy/1990surnames/dist.all.last"

val lineparser = """(.*)\s+([0-9.]+)\s+([0-9.]+)\s+(\d+)""".r
val firstNamesMale = Source.fromURL(census90MaleFirstNames).getLines.toStream.map {
                       case lineparser(name, _, _, _) => name.trim
                       case _ => ""
                     }

val firstNamesFemale = Source.fromURL(census90FemaleFirstNames).getLines.toStream.map {
                         case lineparser(name, _, _, _) => name.trim
                         case _ => ""
                       }
val firstNames = (firstNamesMale ++ firstNamesFemale)
val surnames = Source.fromURL(census90surnames).getLines.toStream.map {
                 case lineparser(name, _, _, _) => name.trim
                 case _ => ""
               }
                    
```

```tut:silent
case class Person(firstName: String, lastName: String, age: Int)

val YoungPeople = for {
  firstName <- Gen.oneOf(firstNames)
  lastName  <- Gen.oneOf(surnames)
  age       <- Gen.choose(18, 34)
} yield Person(firstName, lastName, age)
```

^^^


```tut
(0 to 10).foreach(_ => println(YoungPeople.sample))
```

^^^
#### And we can make them available to properties:

```tut:silent
implicit val ArbYoungPerson = Arbitrary(YoungPeople)

val allPeopleAreYoungPeople = forAll { p: Person =>
  p.age >= 18 && p.age < 34
}
```

^^^
```tut
allPeopleAreYoungPeople.check
```

---

What Other Interesting Things Can We Do?
----------------------------------------

^^^

### Characterizing the test data

```tut:silent
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


```tut
allPeopleAreYoungPeople.check
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
