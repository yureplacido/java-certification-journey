# 1Z0-830 Diagnostic Exam — 50 Questions

## Instructions
- **Time limit:** 120 minutes
- **Format:** Multiple choice (4 options each, exactly ONE correct)
- **Passing score:** 68% (34/50 correct)
- **Note:** This is a diagnostic exam to assess your current knowledge level

---

## Section 1: Language Basics (6 questions)

### Question 1
What is the result of compiling and running the following code?

```java
public class StringTrick {
    public static void main(String[] args) {
        String s1 = "Java";
        String s2 = "Ja" + "va";
        String s3 = new String("Java");
        System.out.println(s1 == s2);
        System.out.println(s1 == s3);
        System.out.println(s1.equals(s3));
    }
}
```

A) `true true true`
B) `true false true`
C) `false false true`
D) `false true true`

**Answer:** B

**Explanation:** String literals are interned by the JVM, so `s1` and `s2` reference the same object (compile-time concatenation of literals). The `new String()` creates a new object on the heap, so `s1 == s3` is false. However, `.equals()` compares content, returning true. This tests compile-time string concatenation optimization and runtime string interning.

**Target:** Language basics
**Tests:** Runtime behavior
**Trap:** Assuming `new String()` with the same content creates the same reference, or forgetting that `+` on string literals is resolved at compile time.

---

### Question 2
What is the result of compiling and running the following code?

```java
public class TextBlockTest {
    public static void main(String[] args) {
        String text = """
                Hello
                    World
                """;
        System.out.println(text.lines().count());
        System.out.println(text.contains("    World"));
    }
}
```

A) 2, true
B) 3, true
C) 3, false
D) 2, false

**Answer:** A

**Explanation:** Text blocks strip the common leading whitespace (16 spaces based on the closing `"""` indentation). The resulting content is `"Hello\n    World\n"`. The `lines()` method splits on line terminators and does not produce an empty trailing line, so it yields 2 lines. The `contains()` check finds the 4-space-indented `"    World"` substring.

**Target:** Language basics
**Tests:** Runtime behavior
**Trap:** Assuming the trailing newline creates a third empty line with `lines()`, or not understanding that incidental whitespace is preserved.

---

### Question 3
What is the result of compiling and running the following code?

```java
public class MathOperations {
    public static void main(String[] args) {
        int a = 5;
        double b = 2.0;
        System.out.println(a / b);
        System.out.println(Math.round(b));
        System.out.println(Math.floor(b));
    }
}
```

A) 2.0, 2.0, 2.0
B) 2.5, 2, 2.0
C) 2.5, 2.0, 2.0
D) 2.5, 2, 2

**Answer:** B

**Explanation:** `a / b` performs mixed-mode arithmetic (`int/double` promotes `a` to `double`), resulting in `2.5`. `Math.round(double)` returns a `long` (`2`), not a `double`. `Math.floor(double)` returns a `double` (`2.0`). The key distinction is the return types of `round()` (long) vs `floor()` (double).

**Target:** Language basics
**Tests:** Runtime behavior
**Trap:** Confusing `Math.round()` return type (`long`) with `Math.floor()` return type (`double`), or forgetting mixed-mode arithmetic promotion rules.

---

### Question 4
What is the result of compiling the following code?

```java
public class PrimitiveCasting {
    public static void main(String[] args) {
        byte b = 10;
        b = b + 1;
        System.out.println(b);
    }
}
```

A) 11
B) Compilation error
C) Runtime exception
D) 10

**Answer:** B

**Explanation:** The expression `b + 1` promotes `b` to `int` (binary numeric promotion because `1` is an `int` literal), and the result is `int`. Assigning an `int` to a `byte` without an explicit cast causes a compile-time error: "incompatible types: possible lossy conversion from int to byte". Note that `b += 1` would compile because compound assignment operators implicitly cast.

**Target:** Language basics
**Tests:** Compile-time error
**Trap:** Thinking `byte + int` results in a `byte`, or forgetting that compound operators like `+=` would work but plain `+` doesn't.

---

### Question 5
What is the result of compiling and running the following code?

```java
public class StringBuilderTest {
    public static void main(String[] args) {
        StringBuilder sb = new StringBuilder("Hello");
        sb.insert(3, "lo");
        sb.replace(1, 3, "a");
        System.out.println(sb);
    }
}
```

A) `Helloo`
B) `Halolo`
C) `Haalloo`
D) `Haoo`

**Answer:** B

**Explanation:** After `insert(3, "lo")`, the string becomes `"Hellolo"` (inserted at index 3: between `l` and `l`). Then `replace(1, 3, "a")` replaces characters at indices 1 and 2 (end index is exclusive) — `"el"` — with `"a"`, resulting in `"Halolo"`. StringBuilder's `replace` uses char indices (not like regex).

**Target:** Language basics
**Tests:** Runtime behavior
**Trap:** Confusing StringBuilder's `replace(start, end, str)` (half-open range) with `String.replace()` (literal replacement), or miscounting indices after insert.

---

### Question 6
What is the result of compiling the following code?

```java
public class VarTest {
    public static void main(String[] args) {
        var x = 10;
        var y = 3.14;
        var z = "Hello";
        var w = null;
        System.out.println(x + y + z + w);
    }
}
```

A) Compiles and prints `17.14Hello`
B) Compiles and prints `103.14Hello`
C) Compilation error on line `var w = null;`
D) Runtime exception

**Answer:** C

**Explanation:** `var` requires an initializer with a clear type for type inference. `null` has no type — the compiler cannot infer a type for `w`. This is a compile-time error: "Cannot infer a type for local variable 'w'". This tests understanding of `var` limitations in type inference (JLS §14.4.1 — local variable declarations and types).

**Target:** Language basics
**Tests:** Compile-time error
**Trap:** Assuming `var` can infer `Object` from `null`, or not knowing that `var` requires a definite type.

---

## Section 2: Date-Time API (4 questions)

### Question 7
What is the result of compiling and running the following code?

```java
import java.time.*;

public class DateTimeTest {
    public static void main(String[] args) {
        LocalDate date = LocalDate.of(2024, 2, 29);
        System.out.println(date.isLeapYear());
        System.out.println(date.plusYears(1));
    }
}
```

A) `true 2025-02-28`
B) `true 2025-03-01`
C) `false 2025-02-28`
D) `true 2025-02-29`

**Answer:** A

**Explanation:** 2024 is a leap year, so `isLeapYear()` returns `true`. `plusYears(1)` adjusts February 29 to February 28 because 2025 is not a leap year — `LocalDate` ensures the date remains valid by rolling back within the same month rather than rolling to the next month. This tests understanding of date adjustment rules in the `java.time` API.

**Target:** Date-Time API
**Tests:** Runtime behavior
**Trap:** Expecting `plusYears()` to throw an exception, roll to March 1, or keep Feb 29 for a non-leap year.

---

### Question 8
What is the result of compiling and running the following code?

```java
import java.time.*;

public class DurationTest {
    public static void main(String[] args) {
        Duration d1 = Duration.ofHours(25);
        Duration d2 = Duration.ofMinutes(1500);
        System.out.println(d1.equals(d2));
        System.out.println(d1.toHours());
        System.out.println(d2.toMinutes());
    }
}
```

A) `true 25 1500`
B) `false 25 1500`
C) `true 1 1500`
D) `false 1 1500`

**Answer:** A

**Explanation:** `Duration` normalizes time amounts internally. 25 hours equals 1500 minutes (25 × 60 = 1500), so `equals()` returns `true`. `toHours()` returns 25 (the total hours), and `toMinutes()` returns 1500 (the total minutes). This tests understanding of `Duration` equality semantics and normalization.

**Target:** Date-Time API
**Tests:** Runtime behavior
**Trap:** Thinking `Duration` stores hours and minutes separately, or forgetting that `toHours()` returns total hours, not just the hours component.

---

### Question 9
What is the result of compiling and running the following code?

```java
import java.time.*;

public class ZonedTest {
    public static void main(String[] args) {
        ZonedDateTime zdt = ZonedDateTime.of(
            2024, 7, 15, 10, 30, 0, 0, ZoneId.of("America/New_York"));
        ZonedDateTime converted = zdt.withZoneSameInstant(ZoneId.of("Asia/Tokyo"));
        System.out.println(converted.getHour());
        System.out.println(converted.getDayOfMonth());
    }
}
```

A) 10 15
B) 11 15
C) 22 15
D) 23 15

**Answer:** D

**Explanation:** In July, New York uses EDT (UTC−4) and Tokyo uses JST (UTC+9). The difference is 13 hours. 10:30 EDT + 13 hours = 23:30 JST, so `getHour()` returns 23 and `getDayOfMonth()` remains 15. This tests timezone conversion with `withZoneSameInstant()` and understanding that July uses EDT (not EST).

**Target:** Date-Time API
**Tests:** Runtime behavior
**Trap:** Forgetting that July uses EDT (UTC−4) instead of EST (UTC−5), or miscalculating the timezone offset difference.

---

### Question 10
What is the result of compiling and running the following code?

```java
import java.time.*;

public class PeriodTest {
    public static void main(String[] args) {
        LocalDate start = LocalDate.of(2024, 1, 31);
        LocalDate end = LocalDate.of(2024, 2, 29);
        Period period = Period.between(start, end);
        System.out.println(period.getMonths());
        System.out.println(period.getDays());
        System.out.println(start.plus(period));
    }
}
```

A) `1 0 2024-02-29`
B) `0 29 2024-02-29`
C) `1 -2 2024-02-29`
D) `0 30 2024-03-01`

**Answer:** B

**Explanation:** `Period.between()` calculates the exact difference: from January 31 to February 29 is 0 months and 29 days (not 1 month, because adding 1 month to January 31 would give February 28, not February 29). `start.plus(period)` adds 0 months and 29 days to January 31, yielding February 29. This tests understanding of `Period` normalization and date arithmetic.

**Target:** Date-Time API
**Tests:** Runtime behavior
**Trap:** Expecting `Period.between()` to return 1 month, or not understanding that periods are not normalized until applied.

---

## Section 3: Flow Control (5 questions)

### Question 11
What is the result of compiling and running the following code?

```java
public class SwitchExpression {
    public static void main(String[] args) {
        int x = 2;
        String result = switch (x) {
            case 1 -> "one";
            case 2 -> "two";
            case 3 -> "three";
            default -> "other";
        };
        System.out.println(result);
    }
}
```

A) `one`
B) `two`
C) `three`
D) `Compilation error`

**Answer:** B

**Explanation:** This is a switch expression (Java 14+) with arrow (`->`) syntax. The value `2` matches `case 2`, so `result` is assigned `"two"`. Arrow-syntax cases never fall through. This tests basic switch expression syntax.

**Target:** Flow control
**Tests:** Runtime behavior
**Trap:** Confusing switch expressions (which must be exhaustive and can return values) with switch statements.

---

### Question 12
What is the result of compiling and running the following code?

```java
public class SwitchPattern {
    public static void main(String[] args) {
        Object obj = "Hello";
        String result = switch (obj) {
            case Integer i -> "Integer: " + i;
            case String s -> "String: " + s;
            case null -> "Null";
            default -> "Other";
        };
        System.out.println(result);
    }
}
```

A) `String: Hello`
B) `Null`
C) `Other`
D) `Compilation error`

**Answer:** A

**Explanation:** This uses pattern matching for switch (finalized in Java 21, JEP 441). The `String s` type pattern matches the `String` object `"Hello"`, producing `"String: Hello"`. The `null` case handles null inputs. This tests Java 21's final pattern matching for switch.

**Target:** Flow control
**Tests:** Runtime behavior
**Trap:** Thinking pattern matching for switch is still a preview feature in Java 21, or confusing it with traditional switch statement rules.

---

### Question 13
What is the result of compiling and running the following code?

```java
public class LoopControl {
    public static void main(String[] args) {
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            if (i % 3 == 0) continue;
            if (i == 7) break;
            sum += i;
        }
        System.out.println(sum);
    }
}
```

A) 12
B) 15
C) 22
D) 25

**Answer:** A

**Explanation:** The loop processes: i=0 (skipped by `continue`, multiple of 3), i=1 (sum=1), i=2 (sum=3), i=3 (skipped), i=4 (sum=7), i=5 (sum=12), i=6 (skipped), i=7 (exits via `break`). The final sum is 1+2+4+5 = 12. This tests understanding of `continue` (skip current iteration) vs `break` (exit loop entirely).

**Target:** Flow control
**Tests:** Runtime behavior
**Trap:** Confusing `continue` with `break`, or miscounting which values are added.

---

### Question 14
What is the result of compiling and running the following code?

```java
public class SwitchFallThrough {
    public static void main(String[] args) {
        int x = 2;
        switch (x) {
            case 1:
                System.out.print("one ");
            case 2:
                System.out.print("two ");
            case 3:
                System.out.print("three ");
            default:
                System.out.print("default ");
        }
    }
}
```

A) `two three default`
B) `one two three default`
C) `two`
D) `Compilation error`

**Answer:** A

**Explanation:** This is a traditional switch statement (not a switch expression) using colon (`:`) syntax with fall-through behavior. When `x=2`, execution matches `case 2` and, lacking `break` statements, falls through all subsequent cases: prints `"two three default "`. This tests understanding of fall-through in traditional switch vs. arrow syntax.

**Target:** Flow control
**Tests:** Runtime behavior
**Trap:** Assuming all switch cases use arrow syntax (no fall-through) or forgetting that traditional switch requires explicit `break`.

---

### Question 15
What is the result of compiling and running the following code?

```java
public class PatternDominance {
    public static void main(String[] args) {
        Object obj = "Hello";
        String result = switch (obj) {
            case String s when s.length() > 5 -> "Long string";
            case String s -> "String: " + s;
            case Object o -> "Object: " + o;
        };
        System.out.println(result);
    }
}
```

A) `Long string`
B) `String: Hello`
C) `Object: Hello`
D) `Compilation error`

**Answer:** B

**Explanation:** The object `"Hello"` has length 5, so the guard `s.length() > 5` evaluates to `false` at runtime. The second `case String s` matches as a fallback for when the guard fails. The `Object o` case is dominated (unreachable) by the `String s` cases, but this is legal in Java 21 because the guarded pattern can fail. This tests pattern matching guards and runtime evaluation order.

**Target:** Flow control
**Tests:** Runtime behavior
**Trap:** Assuming the guard condition is evaluated at compile time, or not understanding that pattern matching cases are evaluated top-to-bottom.

---

## Section 4: OOP - Classes, Records, Nested Classes, Var (5 questions)

### Question 16
What is the result of compiling and running the following code?

```java
record Point(int x, int y) {
    Point {
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("Coordinates must be non-negative");
        }
    }
}

public class RecordTest {
    public static void main(String[] args) {
        Point p = new Point(3, 4);
        System.out.println(p.x() + ", " + p.y());
        System.out.println(p.toString());
    }
}
```

A) `3, 4` and `Point[x=3, y=4]`
B) `3, 4` and `Point@<hashcode>`
C) Compilation error
D) Runtime exception

**Answer:** A

**Explanation:** Records automatically generate `toString()`, `equals()`, `hashCode()`, and accessor methods (`x()`, `y()`). The compact constructor validates state but does not need to assign fields — the canonical constructor handles assignment after the compact body runs. The generated `toString()` returns `Point[x=3, y=4]`. This tests record basics and compact constructors.

**Target:** OOP - classes, records, nested classes, var
**Tests:** Runtime behavior
**Trap:** Forgetting that records have auto-generated `toString()`, or thinking compact constructors must assign fields.

---

### Question 17
What is the result of compiling the following code?

```java
public class NestedClassTest {
    private int outerField = 10;

    class Inner {
        void printOuter() {
            System.out.println(outerField);
        }
    }

    static class StaticInner {
        void printOuter() {
            System.out.println(outerField);
        }
    }

    public static void main(String[] args) {
        NestedClassTest outer = new NestedClassTest();
        Inner inner = outer.new Inner();
        inner.printOuter();
        StaticInner si = new StaticInner();
        si.printOuter();
    }
}
```

A) 10, 10
B) Compilation error
C) Runtime exception
D) 10, null

**Answer:** B

**Explanation:** `StaticInner` is a static nested class and cannot access instance members of the outer class (`outerField` is an instance variable). Attempting to reference `outerField` from a static context causes a compile-time error: "non-static variable outerField cannot be referenced from a static context". This tests understanding of static vs. non-static nested class access rules (JLS §8.1.3, §8.5.1).

**Target:** OOP - classes, records, nested classes, var
**Tests:** Compile-time error
**Trap:** Assuming static nested classes have the same access privileges as inner (non-static) classes.

---

### Question 18
What is the result of compiling and running the following code?

```java
import java.util.*;

public class VarTest2 {
    public static void main(String[] args) {
        var list = List.of(1, 2, 3);
        var mutable = new ArrayList<>(list);
        System.out.println(list.getClass().getSimpleName());
        System.out.println(mutable.getClass().getSimpleName());
    }
}
```

A) `List, ArrayList`
B) `ArrayList, ArrayList`
C) `ListN, ArrayList`
D) `List, List`

**Answer:** C

**Explanation:** `List.of()` returns an unmodifiable list whose runtime type is `ImmutableCollections$ListN` — `getSimpleName()` returns `"ListN"`. `new ArrayList<>()` creates a standard mutable list — `getSimpleName()` returns `"ArrayList"`. `var` is pure compile-time type inference and does not affect runtime types.

**Target:** OOP - classes, records, nested classes, var
**Tests:** Runtime behavior
**Trap:** Assuming `var` changes the runtime type, or assuming `List.of()` returns `ArrayList` or a type with simple name `"List"`.

---

### Question 19
What is the result of compiling the following code?

```java
public class RecordExtendTest {
    record Person(String name, int age) {}

    class Student extends Person {
        Student(String name, int age) {
            super(name, age);
        }
    }

    public static void main(String[] args) {}
}
```

A) Compiles successfully
B) Compilation error
C) Runtime exception
D) Warning only

**Answer:** B

**Explanation:** Records are implicitly `final` (JLS §8.10.1: "A record declaration is implicitly final"). Therefore, `Student` cannot extend `Person`. The compiler reports: "cannot inherit from final Person". This tests understanding that records are final classes.

**Target:** OOP - classes, records, nested classes, var
**Tests:** Compile-time error
**Trap:** Assuming records can be extended like regular classes, or not knowing that records are implicitly final.

---

### Question 20
What is the result of compiling and running the following code?

```java
public class LocalClassTest {
    public static void main(String[] args) {
        int localVar = 10;
        class LocalInner {
            int getValue() {
                return localVar;
            }
        }
        LocalInner inner = new LocalInner();
        System.out.println(inner.getValue());
    }
}
```

A) 10
B) Compilation error
C) Runtime exception
D) 0

**Answer:** A

**Explanation:** Local classes can access local variables from the enclosing scope, but only if they are effectively final. Since `localVar` is not modified after initialization, it is effectively final, and the code compiles and runs correctly, printing `10`. This tests understanding of local classes and the "effectively final" requirement (JLS §8.1.3, §4.12.4).

**Target:** OOP - classes, records, nested classes, var
**Tests:** Runtime behavior
**Trap:** Assuming local classes cannot access local variables, or not understanding the effectively final requirement.

---

## Section 5: OOP - Inheritance, Sealed Types, Pattern Matching, Interfaces, Enums (6 questions)

### Question 21
What is the result of compiling and running the following code?

```java
sealed interface Shape permits Circle, Rectangle, Triangle {}
record Circle(double radius) implements Shape {}
record Rectangle(double width, double height) implements Shape {}
record Triangle(double base, double height) implements Shape {}

public class SealedTest {
    static String describe(Shape shape) {
        return switch (shape) {
            case Circle c -> "Circle with radius " + c.radius();
            case Rectangle r -> "Rectangle";
            case Triangle t -> "Triangle";
        };
    }

    public static void main(String[] args) {
        System.out.println(describe(new Circle(5.0)));
    }
}
```

A) `Circle with radius 5.0`
B) `Circle`
C) Compilation error
D) Runtime exception

**Answer:** A

**Explanation:** This uses a sealed interface with exhaustive pattern matching in switch. Since `Shape` is sealed and only permits `Circle`, `Rectangle`, and `Triangle`, the compiler knows the switch is exhaustive without a `default` case. The pattern `Circle c` matches, extracting the radius via `c.radius()`. This tests Java 21's sealed types with exhaustive switch.

**Target:** OOP - inheritance, sealed types, pattern matching, interfaces, enums
**Tests:** Runtime behavior
**Trap:** Thinking a `default` case is required for sealed types, or not understanding that sealed types enable exhaustive pattern matching.

---

### Question 22
What is the result of compiling and running the following code?

```java
public class EnumTest {
    enum Day {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY,
        SATURDAY, SUNDAY;

        boolean isWeekend() {
            return this == SATURDAY || this == SUNDAY;
        }
    }

    public static void main(String[] args) {
        Day today = Day.FRIDAY;
        switch (today) {
            case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY ->
                System.out.println("Weekday");
            case SATURDAY, SUNDAY ->
                System.out.println("Weekend");
        }
    }
}
```

A) `Weekday`
B) `Weekend`
C) Compilation error
D) Runtime exception

**Answer:** A

**Explanation:** This tests enum constants in switch expressions with multiple case labels using arrow syntax (Java 14+). `Day.FRIDAY` matches the first case group, printing `"Weekday"`. Enums are particularly well-suited for switch because the compiler can verify exhaustiveness.

**Target:** OOP - inheritance, sealed types, pattern matching, interfaces, enums
**Tests:** Runtime behavior
**Trap:** Assuming enum constants cannot be grouped in switch cases, or not understanding arrow syntax with enums.

---

### Question 23
What is the result of compiling and running the following code?

```java
public class SealedPatternTest {
    sealed interface Animal permits Dog, Cat {}
    record Dog(String breed) implements Animal {}
    record Cat(String color) implements Animal {}

    static String identify(Animal animal) {
        return switch (animal) {
            case Dog d when d.breed().equals("Labrador") -> "Labrador";
            case Dog d -> "Other dog";
            case Cat c -> "Cat";
        };
    }

    public static void main(String[] args) {
        System.out.println(identify(new Dog("Labrador")));
        System.out.println(identify(new Dog("Poodle")));
        System.out.println(identify(new Cat("White")));
    }
}
```

A) `Labrador, Other dog, Cat`
B) `Labrador, Labrador, Cat`
C) Compilation error
D) Runtime exception

**Answer:** A

**Explanation:** The first case matches `Dog` with breed `"Labrador"` due to the `when` guard. The second `case Dog d` acts as a fallback for any `Dog` whose guard fails. The third `case Cat c` matches all `Cat` instances. This tests pattern matching with guards and sealed type exhaustiveness.

**Target:** OOP - inheritance, sealed types, pattern matching, interfaces, enums
**Tests:** Runtime behavior
**Trap:** Assuming guards are evaluated at compile time, or not understanding that pattern matching cases are evaluated in declaration order.

---

### Question 24
What is the result of compiling and running the following code?

```java
public class InterfaceDefaultTest {
    interface Greeter {
        default String greet(String name) {
            return "Hello, " + name;
        }
        String farewell(String name);
    }

    static class FormalGreeter implements Greeter {
        public String farewell(String name) {
            return "Goodbye, " + name;
        }
    }

    public static void main(String[] args) {
        Greeter g = new FormalGreeter();
        System.out.println(g.greet("Alice"));
        System.out.println(g.farewell("Bob"));
    }
}
```

A) `Hello, Alice` and `Goodbye, Bob`
B) Compilation error
C) Runtime exception
D) `Hello, Alice` and `Hello, Bob`

**Answer:** A

**Explanation:** `FormalGreeter` inherits the `default` method `greet()` from `Greeter` and provides an implementation for the abstract method `farewell()`. An implementing class is only required to implement abstract methods, not default methods. This tests understanding of interface default vs. abstract methods.

**Target:** OOP - inheritance, sealed types, pattern matching, interfaces, enums
**Tests:** Runtime behavior
**Trap:** Forgetting that implementing classes don't need to override default methods, or confusing default methods with abstract methods.

---

### Question 25
What is the result of compiling and running the following code?

```java
import java.util.*;

public class GenericWildcards {
    static void process(List<? extends Number> list) {
        for (Number n : list) {
            System.out.print(n + " ");
        }
    }

    public static void main(String[] args) {
        List<Integer> ints = List.of(1, 2, 3);
        List<Double> doubles = List.of(1.5, 2.5, 3.5);
        process(ints);
        process(doubles);
    }
}
```

A) `1 2 3 1.5 2.5 3.5`
B) Compilation error
C) Runtime exception
D) `1 2 3 1 2 3`

**Answer:** A

**Explanation:** `? extends Number` is an upper-bounded wildcard that accepts `List<Integer>`, `List<Double>`, or any `List` of a `Number` subtype. The method iterates and prints each number, autoboxing as needed. This tests the PECS (Producer Extends, Consumer Super) principle and wildcard generics.

**Target:** OOP - inheritance, sealed types, pattern matching, interfaces, enums
**Tests:** Runtime behavior
**Trap:** Confusing `? extends` (producer/read) with `? super` (consumer/write), or assuming generics are reified at runtime.

---

### Question 26
What is the result of compiling the following code?

```java
public class SealedHierarchyTest {
    sealed interface Vehicle permits MotorizedVehicle, Bicycle {}
    sealed interface MotorizedVehicle extends Vehicle permits Car, ElectricCar {}
    non-sealed class Bicycle implements Vehicle {}
    record Car(String model) implements MotorizedVehicle {}
    record ElectricCar(String model) implements MotorizedVehicle {}

    static String categorize(Vehicle v) {
        return switch (v) {
            case ElectricCar e -> "Electric";
            case Car c -> "Car";
            case Bicycle b -> "Bicycle";
        };
    }

    public static void main(String[] args) {
        System.out.println(categorize(new ElectricCar("Tesla")));
    }
}
```

A) `Electric`
B) `Car`
C) `Bicycle`
D) `Compilation error`

**Answer:** A

**Explanation:** `ElectricCar` implements `MotorizedVehicle`, which extends `Vehicle`. The pattern `ElectricCar e` is checked first in the switch — it matches the `ElectricCar` instance, returning `"Electric"`. This tests understanding of sealed type hierarchies, transitive permitted subtypes, and pattern dominance in exhaustive switches.

**Target:** OOP - inheritance, sealed types, pattern matching, interfaces, enums
**Tests:** Runtime behavior
**Trap:** Not understanding that `MotorizedVehicle`'s permitted subtypes are transitively covered by `Vehicle`'s sealed hierarchy, or confusing pattern dominance rules.

---

## Section 6: Exceptions (4 questions)

### Question 27
What is the result of compiling and running the following code?

```java
public class TryWithResources {
    public static void main(String[] args) {
        try (var resource = new AutoCloseable() {
            {
                System.out.println("Opening");
            }
            public void close() {
                System.out.println("Closing");
            }
        }) {
            System.out.println("Using");
        }
    }
}
```

A) `Opening Using Closing`
B) `Using Opening Closing`
C) `Opening Closing Using`
D) Compilation error

**Answer:** A

**Explanation:** In try-with-resources, the resource is initialized before the try block body executes, and `close()` is called after the try block completes (in a `finally` block implicitly). The order is: resource initialization (`"Opening"`), try body (`"Using"`), resource cleanup (`"Closing"`). This tests understanding of try-with-resources execution order (JLS §14.20.3).

**Target:** Exceptions
**Tests:** Runtime behavior
**Trap:** Assuming resources are closed before the try block executes, or not understanding that initialization happens before the try body.

---

### Question 28
What is the result of compiling and running the following code?

```java
public class SuppressedExceptions {
    public static void main(String[] args) {
        try {
            throw new RuntimeException("Original");
        } catch (RuntimeException e) {
            System.out.println("Caught: " + e.getMessage());
            throw new RuntimeException("New", e);
        } finally {
            System.out.println("Finally");
        }
    }
}
```

A) `Caught: Original` and `Finally`
B) `Caught: Original`, `Finally`, and uncaught `RuntimeException`
C) `Finally` and uncaught `RuntimeException`
D) Compilation error

**Answer:** B

**Explanation:** The catch block prints `"Caught: Original"`, then throws a new `RuntimeException("New", e)`. The `finally` block always executes, printing `"Finally"`. The new exception propagates out of `main()` as uncaught. The original exception becomes the **cause** of the new one via the `RuntimeException(String, Throwable)` constructor — the stack trace shows it as `Caused by: java.lang.RuntimeException: Original`. Note that *suppressed* exceptions are a different mechanism: they only arise with try-with-resources when a `close()` failure is attached to an exception already in flight. Option B is the most complete description of all output (though the uncaught exception is a stack trace, not `System.out` output).

**Target:** Exceptions
**Tests:** Runtime behavior
**Trap:** Thinking the `finally` block prevents the exception from propagating, or confusing an exception's *cause* with *suppressed* exceptions (suppressed only occur in try-with-resources).

---

### Question 29
What is the result of compiling and running the following code?

```java
public class MultiCatchTest {
    static void process(String s) {
        try {
            int x = Integer.parseInt(s);
            int y = 10 / x;
            System.out.println(y);
        } catch (NumberFormatException | ArithmeticException e) {
            System.out.println("Error: " + e.getClass().getSimpleName());
        }
    }

    public static void main(String[] args) {
        process("abc");
        process("0");
    }
}
```

A) `Error: NumberFormatException` and `Error: ArithmeticException`
B) `Error: NumberFormatException` and `Error: ArithmeticException` with suppressed
C) `Error: Exception` and `Error: Exception`
D) Compilation error

**Answer:** A

**Explanation:** Multi-catch (`|`) catches either exception type in a single handler. For `"abc"`, `parseInt` throws `NumberFormatException`. For `"0"`, `10 / 0` throws `ArithmeticException`. Each call is handled independently, and `getClass().getSimpleName()` prints the actual exception class name. This tests multi-catch syntax and exception types.

**Target:** Exceptions
**Tests:** Runtime behavior
**Trap:** Assuming multi-catch catches both exceptions simultaneously, or not understanding that `|` means "or" not "and".

---

### Question 30
What is the result of compiling the following code?

```java
public class ExceptionHierarchy {
    static class BaseException extends Exception {}
    static class SubException extends BaseException {}

    static void method() throws BaseException {
        throw new SubException();
    }

    public static void main(String[] args) {
        try {
            method();
        } catch (BaseException e) {
            System.out.println("Caught Base");
        } catch (SubException e) {
            System.out.println("Caught Sub");
        }
    }
}
```

A) `Caught Base`
B) `Caught Sub`
C) Compilation error
D) Runtime exception

**Answer:** C

**Explanation:** The catch block for `SubException` is unreachable because `SubException` is a subclass of `BaseException`, and the first catch block already handles it. The compiler detects this and reports: "exception SubException has already been caught". This tests understanding of exception hierarchy and catch block reachability analysis (JLS §14.21).

**Target:** Exceptions
**Tests:** Compile-time error
**Trap:** Assuming catch blocks are evaluated only at runtime in order, or not understanding that the compiler statically checks catch block reachability.

---

## Section 7: Collections (4 questions)

### Question 31
What is the result of compiling and running the following code?

```java
import java.util.*;

public class SequencedCollectionTest {
    public static void main(String[] args) {
        var list = new ArrayList<>(List.of(1, 2, 3, 4, 5));
        System.out.println(list.getFirst());
        System.out.println(list.getLast());
        list.addFirst(0);
        System.out.println(list);
    }
}
```

A) `1, 5, [0, 1, 2, 3, 4, 5]`
B) `5, 1, [0, 1, 2, 3, 4, 5]`
C) Compilation error
D) Runtime exception

**Answer:** A

**Explanation:** Java 21 introduced the `SequencedCollection` interface (JEP 431) with methods `getFirst()`, `getLast()`, `addFirst()`, and `addLast()`. `ArrayList` implements this interface, so `getFirst()` returns `1` and `getLast()` returns `5`. `addFirst(0)` inserts `0` at the beginning, producing `[0, 1, 2, 3, 4, 5]`.

**Target:** Collections
**Tests:** Runtime behavior
**Trap:** Confusing `getFirst()` with `get(0)` (functionally equivalent but a new API), or not knowing that `SequencedCollection` is new in Java 21.

---

### Question 32
What is the result of compiling and running the following code?

```java
import java.util.*;

public class MapTest {
    public static void main(String[] args) {
        var map = new LinkedHashMap<String, Integer>();
        map.put("Banana", 2);
        map.put("Apple", 3);
        map.put("Cherry", 1);
        map.put("Date", 4);
        map.put("Apple", 5);
        System.out.println(map);
        System.out.println(map.get("Apple"));
    }
}
```

A) `{Banana=2, Apple=5, Cherry=1, Date=4}` and `5`
B) `{Banana=2, Apple=3, Cherry=1, Date=4, Apple=5}` and `5`
C) `{Apple=5, Banana=2, Cherry=1, Date=4}` and `5`
D) `{Banana=2, Apple=3, Cherry=1, Date=4}` and `3`

**Answer:** A

**Explanation:** `LinkedHashMap` maintains insertion order. The second `put("Apple", 5)` overwrites the previous value (`3`) but keeps `"Apple"` in its original position. The map contains 4 entries (not 5) because keys are unique. `get("Apple")` returns the updated value `5`.

**Target:** Collections
**Tests:** Runtime behavior
**Trap:** Thinking `put` with a duplicate key creates a new entry, or not understanding that `LinkedHashMap` maintains insertion order.

---

### Question 33
What is the result of compiling and running the following code?

```java
import java.util.*;

public class StreamSortTest {
    public static void main(String[] args) {
        var list = new ArrayList<>(List.of(3, 1, 4, 1, 5, 9, 2, 6));
        list.sort(Comparator.naturalOrder());
        System.out.println(list);
        list.sort(Comparator.reverseOrder());
        System.out.println(list);
    }
}
```

A) `[1, 1, 2, 3, 4, 5, 6, 9]` and `[9, 6, 5, 4, 3, 2, 1, 1]`
B) `[9, 6, 5, 4, 3, 2, 1, 1]` and `[1, 1, 2, 3, 4, 5, 6, 9]`
C) Compilation error
D) Runtime exception

**Answer:** A

**Explanation:** `Comparator.naturalOrder()` sorts in natural (ascending) order: `[1, 1, 2, 3, 4, 5, 6, 9]`. `Comparator.reverseOrder()` sorts in reverse (descending) order: `[9, 6, 5, 4, 3, 2, 1, 1]`. `List.sort()` modifies the list in place and returns `void`. This tests understanding of `Comparator` factory methods.

**Target:** Collections
**Tests:** Runtime behavior
**Trap:** Confusing `Comparator.naturalOrder()` with `Comparator.reverseOrder()`, or not knowing that `List.sort()` modifies the list in place.

---

### Question 34
What is the result of compiling and running the following code?

```java
import java.util.*;

public class ImmutableCollectionTest {
    public static void main(String[] args) {
        var list = List.of(1, 2, 3);
        list.add(4);
        System.out.println(list);
    }
}
```

A) `[1, 2, 3, 4]`
B) `[1, 2, 3]`
C) Compilation error
D) `UnsupportedOperationException` at runtime

**Answer:** D

**Explanation:** `List.of()` returns an unmodifiable list. The code compiles because `add()` is declared in the `List` interface, but the implementation does not support mutation. Calling `add()` throws `UnsupportedOperationException` at runtime. This tests understanding of immutable vs. unmodifiable collections.

**Target:** Collections
**Tests:** Runtime behavior
**Trap:** Thinking `List.of()` returns a mutable list, or assuming the compiler catches unmodifiable collection operations.

---

## Section 8: Functional (4 questions)

### Question 35
What is the result of compiling and running the following code?

```java
import java.util.function.*;

public class LambdaTest {
    public static void main(String[] args) {
        Predicate<String> isLong = s -> s.length() > 5;
        Predicate<String> startsWithJ = s -> s.startsWith("J");
        Predicate<String> combined = isLong.and(startsWithJ);
        System.out.println(combined.test("JavaScript"));
        System.out.println(combined.test("Java"));
    }
}
```

A) `true false`
B) `false true`
C) `true true`
D) `false false`

**Answer:** A

**Explanation:** `combined` is the conjunction of both predicates using `and()`. `"JavaScript"` has length 10 (>5) and starts with `"J"` → both are true → `true`. `"Java"` has length 4 (≤5), so `isLong` is false → `false`. This tests predicate composition and logical AND behavior.

**Target:** Functional
**Tests:** Runtime behavior
**Trap:** Confusing `and()` with `or()`, or not understanding that `and()` requires both predicates to return true.

---

### Question 36
What is the result of compiling and running the following code?

```java
import java.util.function.*;

public class FunctionChainTest {
    public static void main(String[] args) {
        Function<String, Integer> toLength = String::length;
        Function<Integer, String> repeat = n -> "*".repeat(n);
        Function<String, String> process = toLength.andThen(repeat);
        System.out.println(process.apply("Hello"));
    }
}
```

A) `*****`
B) `Hello`
C) Compilation error
D) Runtime exception

**Answer:** A

**Explanation:** `toLength` converts `"Hello"` to `5` (its length). `repeat` repeats `"*"` 5 times: `"*****"`. `andThen` chains the functions left-to-right: first apply `toLength`, then apply `repeat` to the result. This tests function composition with `andThen` and method references.

**Target:** Functional
**Tests:** Runtime behavior
**Trap:** Confusing `andThen` (left-to-right) with `compose` (right-to-left), or not understanding method reference syntax.

---

### Question 37
What is the result of compiling and running the following code?

```java
import java.util.function.*;

public class FunctionalInterfaceTest {
    public static void main(String[] args) {
        Function<String, String> upper = String::toUpperCase;
        Function<String, String> trim = String::trim;
        Function<String, String> combined = upper.andThen(trim);
        System.out.println(combined.apply("  hello  "));
    }
}
```

A) `HELLO`
B) `  HELLO  `
C) Compilation error
D) `hello`

**Answer:** A

**Explanation:** `upper` transforms `"  hello  "` to `"  HELLO  "` (preserving spaces). `trim` removes leading/trailing spaces, yielding `"HELLO"`. `andThen` applies `trim` after `upper`. This tests `Function` chaining with `andThen` and method references.

**Target:** Functional
**Tests:** Runtime behavior
**Trap:** Confusing the order of function application (`andThen` vs `compose`), or not understanding that `toUpperCase` preserves whitespace.

---

### Question 38
What is the result of compiling and running the following code?

```java
import java.util.function.*;

public class BiFunctionTest {
    public static void main(String[] args) {
        BiFunction<String, Integer, String> repeat = (s, n) -> s.repeat(n);
        String result = repeat.apply("Java", 3);
        System.out.println(result);
    }
}
```

A) `JavaJavaJava`
B) `Java3`
C) Compilation error
D) Runtime exception

**Answer:** A

**Explanation:** `BiFunction<String, Integer, String>` takes two arguments: a `String` and an `Integer`, returning a `String`. The lambda `(s, n) -> s.repeat(n)` calls `String.repeat(int)` (Java 11+) to repeat `"Java"` 3 times, producing `"JavaJavaJava"`. This tests `BiFunction` usage and knowledge of the `repeat()` method.

**Target:** Functional
**Tests:** Runtime behavior
**Trap:** Confusing `Function` (one arg) with `BiFunction` (two args), or not knowing that `repeat()` is a Java 11+ method.

---

## Section 9: Streams (4 questions)

### Question 39
What is the result of compiling and running the following code?

```java
import java.util.*;
import java.util.stream.*;

public class StreamCollectTest {
    public static void main(String[] args) {
        var result = Stream.of(1, 2, 3, 4, 5)
            .filter(n -> n % 2 != 0)
            .map(n -> n * n)
            .collect(Collectors.toList());
        System.out.println(result);
    }
}
```

A) `[1, 9, 25]`
B) `[1, 4, 9, 16, 25]`
C) `[2, 4]`
D) Compilation error

**Answer:** A

**Explanation:** `filter(n -> n % 2 != 0)` keeps odd numbers: `[1, 3, 5]`. `map(n -> n * n)` squares each: `[1, 9, 25]`. `Collectors.toList()` gathers results into a list. This tests basic stream pipeline operations: filter, map, and collect.

**Target:** Streams
**Tests:** Runtime behavior
**Trap:** Confusing `filter` with `removeIf`, or not understanding that `map` transforms each element.

---

### Question 40
What is the result of compiling and running the following code?

```java
import java.util.stream.*;

public class StreamReduceTest {
    public static void main(String[] args) {
        var sum = IntStream.rangeClosed(1, 10)
            .reduce(0, Integer::sum);
        var product = IntStream.rangeClosed(1, 5)
            .reduce(1, (a, b) -> a * b);
        System.out.println(sum + " " + product);
    }
}
```

A) `55 120`
B) `45 120`
C) `55 15`
D) `45 15`

**Answer:** A

**Explanation:** `IntStream.rangeClosed(1, 10)` generates 1 to 10 inclusive. `reduce(0, Integer::sum)` computes the sum: 0+1+2+...+10 = 55. `IntStream.rangeClosed(1, 5)` generates 1 to 5. `reduce(1, (a, b) -> a * b)` computes the product: 1×2×3×4×5 = 120. This tests stream reduction with identity values.

**Target:** Streams
**Tests:** Runtime behavior
**Trap:** Confusing `rangeClosed` (inclusive) with `range` (exclusive upper bound), or using the wrong identity value.

---

### Question 41
What is the result of compiling and running the following code?

```java
import java.util.*;
import java.util.stream.*;

public class StreamGroupingTest {
    public static void main(String[] args) {
        var words = List.of("apple", "banana", "cherry", "avocado", "blueberry");
        var grouped = words.stream()
            .collect(Collectors.groupingBy(w -> w.charAt(0), TreeMap::new, Collectors.toList()));
        System.out.println(grouped);
    }
}
```

A) `{a=[apple, avocado], b=[banana, blueberry], c=[cherry]}`
B) `{a=2, b=2, c=1}`
C) `[apple, avocado, banana, blueberry, cherry]`
D) Compilation error

**Answer:** A

**Explanation:** The three-argument `Collectors.groupingBy(classifier, mapFactory, downstream)` groups words by their first character into a `Map<Character, List<String>>`, using the supplied `TreeMap` factory so keys are sorted (`a`, `b`, `c`). Words starting with `'a'` are grouped together, `'b'` together, and `'c'` together. The default `groupingBy(classifier)` overload returns a `HashMap`, whose iteration order is unspecified — that is why an explicit map factory is needed for a deterministic key order.

**Target:** Streams
**Tests:** Runtime behavior
**Trap:** Confusing `groupingBy` with `counting` or `toSet`, or assuming the `HashMap` returned by the default overload has a defined iteration order (it does not — use the three-argument overload with a `TreeMap` factory for sorted keys).

---

### Question 42
What is the result of compiling and running the following code?

```java
import java.util.stream.*;

public class ParallelStreamTest {
    public static void main(String[] args) {
        var sum = IntStream.rangeClosed(1, 100)
            .parallel()
            .reduce(0, Integer::sum);
        System.out.println(sum);
    }
}
```

A) `5050`
B) `0`
C) Compilation error
D) Non-deterministic result

**Answer:** A

**Explanation:** `reduce(0, Integer::sum)` with an identity value (`0`) uses an associative and commutative operation (`Integer::sum`), so the result is deterministic regardless of parallel execution order. The sum of 1 to 100 is always 5050. This tests understanding of parallel streams and reduction semantics.

**Target:** Streams
**Tests:** Runtime behavior
**Trap:** Assuming parallel streams always produce non-deterministic results, or not understanding that `reduce` with identity is safe for parallel execution.

---

## Section 10: Modules (JPMS) (3 questions)

### Question 43
Consider the following module declaration:

```java
// module-info.java
module com.example.app {
    requires java.sql;
    exports com.example.app.api;
}
```

Which statement is true?

A) The module requires `java.sql` and exports `com.example.app.api` to other modules
B) The module provides `java.sql` and requires `com.example.app.api`
C) Compilation error — `requires` and `exports` cannot coexist in one module
D) The module imports `java.sql` and exports `com.example.app.api` for reflection

**Answer:** A

**Explanation:** In JPMS, `requires` specifies a dependency (the module needs `java.sql` to compile and run), and `exports` makes a package public to other modules for compile-time access. Both directives can coexist in a single module declaration. This tests understanding of basic JPMS module declaration syntax (JLS §7.7, JEP 261).

**Target:** Modules (JPMS)
**Tests:** Compile-time knowledge
**Trap:** Confusing `requires` with `exports`, or using non-JPMS terminology (`import`/`export` in wrong direction).

---

### Question 44
What is the effect of the `transitive` modifier in the following module declaration?

```java
// module-info.java
module com.example.app {
    requires transitive java.sql;
}
```

A) Modules that depend on `com.example.app` automatically gain access to `java.sql`
B) `com.example.app` automatically gains access to all modules that depend on `java.sql`
C) All modules in the application are forced to require `java.sql`
D) `java.sql` is prevented from being used outside `com.example.app`

**Answer:** A

**Explanation:** `requires transitive` means that any module that reads `com.example.app` will also automatically read `java.sql`. This is essential when `com.example.app`'s API exposes types from `java.sql` — dependent modules need `java.sql` on their module path without explicit declaration. This tests transitive dependencies in JPMS.

**Target:** Modules (JPMS)
**Tests:** Compile-time knowledge
**Trap:** Confusing the direction of the transitive dependency, or not understanding when `requires transitive` is needed.

---

### Question 45
What does the `opens` directive do in the following module declaration?

```java
// module-info.java
module com.example.app {
    requires java.sql;
    opens com.example.app.config;
}
```

A) Makes `com.example.app.config` accessible for deep reflection at runtime
B) Makes `com.example.app.config` public to all modules for compile-time access
C) Exports `com.example.app.config` for both compile-time and runtime access
D) Prevents any reflection on `com.example.app.config`

**Answer:** A

**Explanation:** `opens` makes a package open for deep reflection at runtime (e.g., for frameworks like Spring that access private members via reflection). Unlike `exports`, which allows compile-time access, `opens` is specifically for runtime reflection. A package can be `opens` without being `exports`, and vice versa. This tests `opens` vs `exports` in JPMS.

**Target:** Modules (JPMS)
**Tests:** Runtime knowledge
**Trap:** Confusing `opens` with `exports`, or not understanding that `opens` is specifically for runtime deep reflection.

---

## Section 11: Concurrency (3 questions)

### Question 46
What is the result of compiling and running the following code?

```java
import java.util.concurrent.*;
import java.util.stream.*;

public class VirtualThreadTest {
    public static void main(String[] args) throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            IntStream.range(0, 5).forEach(i ->
                executor.submit(() -> {
                    System.out.println("Task " + i + " on "
                        + Thread.currentThread().getClass().getSimpleName());
                })
            );
        }
    }
}
```

A) Prints 5 tasks on virtual threads (thread name contains "VirtualThread")
B) Prints 5 tasks on platform threads
C) Compilation error
D) Runtime exception

**Answer:** A

**Explanation:** `Executors.newVirtualThreadPerTaskExecutor()` (Java 21, JEP 444) creates a virtual thread for each submitted task. `Thread.currentThread().getClass().getSimpleName()` returns `"VirtualThread"`. The try-with-resources ensures the executor is shut down before exiting. This tests Java 21's virtual threads API.

**Target:** Concurrency
**Tests:** Runtime behavior
**Trap:** Confusing virtual threads with platform threads, or not knowing that `newVirtualThreadPerTaskExecutor()` is new in Java 21.

---

### Question 47
What is the result of compiling and running the following code?

```java
import java.util.concurrent.*;

public class ExecutorServiceTest {
    public static void main(String[] args) throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        Future<String> future1 = executor.submit(() -> "Result1");
        Future<String> future2 = executor.submit(() -> "Result2");
        System.out.println(future1.get());
        System.out.println(future2.get());
        executor.shutdown();
    }
}
```

A) `Result1` then `Result2`
B) `Result2` then `Result1`
C) Compilation error
D) Runtime exception

**Answer:** A

**Explanation:** `submit()` returns a `Future` that holds the task's result. `get()` blocks until the result is available. `future1.get()` returns `"Result1"` and `future2.get()` returns `"Result2"` — the order of `get()` calls determines the output order. `executor.shutdown()` prevents new tasks from being submitted. This tests understanding of `ExecutorService` and `Future`.

**Target:** Concurrency
**Tests:** Runtime behavior
**Trap:** Assuming the order of task execution determines output, or not understanding that `get()` blocks and returns the specific future's result.

---

### Question 48
What is the result of compiling and running the following code?

```java
import java.util.concurrent.atomic.*;

public class AtomicTest {
    public static void main(String[] args) {
        AtomicInteger counter = new AtomicInteger(0);
        counter.incrementAndGet();
        counter.addAndGet(5);
        System.out.println(counter.get());
    }
}
```

A) `5`
B) `6`
C) `1`
D) Compilation error

**Answer:** B

**Explanation:** `incrementAndGet()` atomically increments from 0 to 1 and returns 1. `addAndGet(5)` atomically adds 5 to 1, resulting in 6, and returns 6. `get()` returns the current value: 6. This tests understanding of `AtomicInteger` methods — specifically the difference between `incrementAndGet` (returns after) and `getAndIncrement` (returns before).

**Target:** Concurrency
**Tests:** Runtime behavior
**Trap:** Confusing `incrementAndGet()` (returns new value) with `getAndIncrement()` (returns old value), or not understanding that `addAndGet()` returns the updated value.

---

## Section 12: Annotations (2 questions)

### Question 49
What is the result of compiling the following code?

```java
public class OverrideTest {
    static class Parent {
        public void process(String s) {}
    }

    static class Child extends Parent {
        @Override
        public void process(int i) {}
    }

    public static void main(String[] args) {}
}
```

A) Compiles successfully
B) Compilation error
C) Runtime exception
D) Warning only

**Answer:** B

**Explanation:** `@Override` indicates that a method is intended to override a superclass method. `Child.process(int)` does not override `Parent.process(String)` because the parameter types differ — overriding requires an identical method signature. The compiler reports: "method does not override or implement a method from a supertype". This tests `@Override` semantics (JLS §9.6.4.4).

**Target:** Annotations
**Tests:** Compile-time error
**Trap:** Assuming `@Override` is optional or doesn't affect compilation, or not understanding that override requires an identical method signature.

---

### Question 50
What is the result of compiling and running the following code?

```java
public class SuppressWarningsTest {
    @SuppressWarnings("unchecked")
    static <T> T[] merge(T[] a, T[] b) {
        T[] result = (T[]) new Object[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    public static void main(String[] args) {
        String[] a = {"Hello", "World"};
        String[] b = {"Java", "21"};
        String[] merged = merge(a, b);
        System.out.println(merged.length);
    }
}
```

A) `4`
B) Compilation error
C) Runtime exception (`ClassCastException`)
D) Warning only

**Answer:** C

**Explanation:** `@SuppressWarnings("unchecked")` suppresses the compiler warning for the unchecked cast `(T[]) new Object[...]`, but it does NOT change runtime behavior. Due to type erasure, the array created inside `merge` is actually `Object[]`, not `String[]`. The cast `(T[])` performs no runtime check inside the generic method, so the method hands back an `Object[]`; the compiler-inserted cast at the call site (`String[] merged = merge(a, b)`) then fails on the reified array check: `ClassCastException: [Ljava.lang.Object; cannot be cast to [Ljava.lang.String;`. This tests type erasure and the limits of `@SuppressWarnings`.

**Target:** Annotations
**Tests:** Runtime behavior
**Trap:** Assuming `@SuppressWarnings` eliminates runtime type checks, or not understanding that generic type information is erased at runtime.

---

## End of Diagnostic Exam

**Total Questions:** 50
**Time Limit:** 120 minutes
**Passing Score:** 68% (34/50)
