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

---

## End of Diagnostic Exam

**Total Questions:** 50
**Time Limit:** 120 minutes
**Passing Score:** 68% (34/50)
