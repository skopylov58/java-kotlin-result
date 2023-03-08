# Обработка исключений в Java с использованием сопоставления с образцом (pattern matching).

Данная статья является логическим продолжением статей 
- [Обработка исключений в Java в функциональном стиле](https://habr.com/ru/post/676852/) и 
- [Обработка исключений в Java в функциональном стиле. Часть 2](https://habr.com/ru/post/687954/).

В данной статье рассмотрим способы обработки исключений Java при помощи pattern-matching, как это делается в других FP языках.

## Пример на других языках

В функциональных языках программирования существуют удобные средства для работы с исключениями. В Kotlin и Rust это класс Result, в Scala и Haskell - Try. Обработка успешного результата или ошибки может производится при помощи pattern-matching как на примерах ниже.

Scala
```scala
val result = divideWithTry(10, 0) match {
  case Success(i) => i
  case Failure(DivideByZero()) => None
}
```

Rust
```rust
    let greeting_file_result = File::open("hello.txt");
    let greeting_file = match greeting_file_result {
        Ok(file) => file,
        Err(error) => panic!("Problem opening the file: {:?}", error),
    };
```

Использование pattern-matching является естественным (idiomatic) в функциональных языках. А как дело обстоит в Java?


## Немного предыстории

Существует мнение, что сейчас в Java происходит 3-я революция. Первая революция произошла с появлением генериков в Java 5, вторая революция - с появлением лямбд и потоков а Java 8. Новые возможности кардинально изменили язык Java сделав его более современным и выразительным. Заметим, что эти изменения происходили одномоментно с выходом соответствующей версии Java. Однако после 8-й версии Java release-train изменился, новые версии стали выпускаться чаще, но новые фичи стали не такими крупными. Вот некоторые из них за последние годы, многие из них имеют по несколько pre-view, некоторые еще не финализированы до сих пор.

- JEP 305: Pattern Matching for instanceof - приятный сахар, не более того
- JEP 359: Records  - действительно полезная вещь, в том числе как замена кортежей (tuples)
- JEP 360: Sealed Classes - ценность не вполне понятна в изолированном контексте
- JEP 405: Record Patterns - попытка де-конструировать записи, зачем?
- JEP 406: Pattern Matching for switch - уже четыре preview, еще не финализировано, но картина начинает складываться

Сами по себе эти нововведения в изоляции не кажутся особо значимыми, но взятые вместе они позволяют говорить о тихо идущей 3-ей революции в Java - революции функционального подхода в программировании.

## Моделирование результата с исключением

Представим себе некоторое вычисление которое может завершиться успешно с результатом типа `T` или выбросить исключение. Знакомая ситуация?

Смоделируем результат с помощью запечатанного (sealed) интерфейса `Result<T>`.

```java
public sealed interface Result<T> permits Success, Failure {
...
}
```
Этот запечатанный интерфейс позволяет иметь только двух наследников - `Success` и `Failure`. Особенностью запечатанных классов (интерфейсов) является то, что конструкция switch может теперь знать весь набор возможных значений этого типа.

Определим наследников при помощи записей (records)

```java
public record Success<T>(T value) implements Result<T> {
}

public record Failure<T>(Exception exception) implements Result<T> {
}
```
Подобный стиль моделирования данных широко используется в функциональном программировании и носит название алгебраических типов данных (ADT).

## Получение результата

Для получения результата можно использовать производящую (factory) функцию

```java
    static <T> Result<T> runCatching(CheckedSupplier<T> suppl) {
        try {
            return new Success<>(suppl.get());
        } catch (Exception e) {
            return new Failure<>(e);
        }
    }
```

Вот так например мы можем создать URL результат из строки, здесь конструктор URL может выбросить MalformedURLException. 

```java
    @Test public void testUrl() {
        var urlResult = Result.runCatching(() -> new URL("foo/bar"));
        assertTrue(urlResult instanceof Failure);
        urlResult.onFailure(e -> assertTrue(e instanceof MalformedURLException));
    }
```

## Обработка результата

Рассмотрим различные варианты обработки результата на примере функции которая извлекает номер порта из строкового представления URL. Здесь может возникнуть ошибка при преобразовании строки в URL, или URL может не иметь явно указанного порта и тогда ```getPort()``` вернет -1.

### 1. Традиционный код

```java
    Optional<Integer> getURLPortTraditional(String urlStr) {
        try {
            URL url = new URL(urlStr);
            int port = url.getPort();
            return port == -1 ? Optional.empty() : Optional.of(port);
        } catch (MalformedURLException e) {
            return Optional.empty();
        }
    }    
```
Без комментариев.

### 2. Сопоставление с образцом класса

```java
    Optional<Integer> getURLPortWithSimplePatternMatching(String url) {
        var portResult = runCatching(() -> new URL(url)).map(URL::getPort);
        return switch (portResult) {
        case Success<Integer> s -> s.value() == -1 ? Optional.empty() : Optional.of(s.value());
        case Failure f -> Optional.empty();
        };
    }
```
Сопоставляем c образцом класса ```Success<Integer> s```, порт достаем явно с помощью метода ```s.value()```

### 3. Сопоставление с образцом записи с деконструкцией записи на компоненты

```java
    Optional<Integer> getURLPortWithRecordMatching(String url) {
        var portResult = runCatching(() -> new URL(url)).map(URL::getPort);
        return switch (portResult) {
        case Success<Integer>(Integer port) -> port == -1 ? Optional.empty() : Optional.of(port);
        case Failure f -> Optional.empty();
        };
    }
```
Сопоставляем с записью ```Success<Integer>(Integer port)```, компилятор определяет для нас переменную ```port``` и неявно инициализирует ее значением из записи. Происходит так называемая де-конструкция записи (record deconstruction) на компоненты.

### 4. Сопоставление с образцом записи с выводом типа

```java
    Optional<Integer> getURLPortWithRecordMatchingInfere(String url) {
        var portResult = runCatching(() -> new URL(url)).map(URL::getPort);
        return switch (portResult) {
        case Success<Integer>(var port) -> port == -1 ? Optional.empty() : Optional.of(port);
        case Failure f -> Optional.empty();
        };
    }
```
Как и в случае 3, только не нужно указывать тип компоненты, можно написать ```var port```, компилятор сам выведет нужный тип.


### 5. Как будет в окончательной версии Java 20+

```java
    Optional<Integer> getURLPortWithRecordMatchingInfere(String url) {
        var portResult = runCatching(() -> new URL(url)).map(URL::getPort);
        return switch (portResult) {
        case Success(var port) -> port == -1 ? Optional.empty() : Optional.of(port);
        case Failure f -> Optional.empty();
        };
    }
```
Самая компактный код, сопоставляем с записью ```Success(var port)```, типы указывать не надо вообще, компилятор это выведет сам. Красота.

### 6. Без сопоставления с образцом

```java
    Optional<Integer> getURLPortWithMonad(String url) {
        return runCatching(() -> new URL(url)).map(URL::getPort)
            .filter(port -> port != -1)
            .fold(port -> Optional.of(port), exception -> Optional.empty());
    }
```
Задача не сложная, можно обойтись и без pattern-matching-а.

## Комбинирование результатов.

Иногда нужно из нескольких результатов получить итоговый результат. Для примера возьмем два результата ```Result<Integer>``` и найдем сумму если оба результата успешные.

### 7. Наивное комбинирование с распаковкой Result

```java
    Result<Integer> sumResultsNaive(Result<Integer> i1, Result<Integer> i2) {
        if (i1.isSuccess() && i2.isSuccess()) {
            Integer x1 = i1.getOrNull();
            Integer x2 = i2.getOrNull();
            return Success.of(sum(x1, x2));
        } 
        return i1.isFailure() ? i1 : i2;
    }
```
Так напишет программист не знакомый с приемами FP, но это типичный вполне рабочий код.

### 8. Комбинирование в стиле FP при помощи вложенных (nested) ```flatMap```

```java
    Result<Integer> sumResultsFlatMap(Result<Integer> i1, Result<Integer> i2) {
        return i1.flatMap(x1 -> i2.map(x2 -> sum(x1, x2)));
    }
```
Этот идиоматичный прием FP следует изучить, запомнить и применять в каждом удобном случае. Компактно и красиво, однако следует иметь в виду что таким образом можно комбинировать только монады одного типа.

### 9. Комбинирование с применением pattern matching.

Если нужно скомбинировать монады различных типов, например ```Result<Integer>``` и ```Option<Integer>```, то у нас есть либо наивный вариант #7, либо привести типы к одному виду и использовать #8, либо использование pattern matching.

```java
    Result<Integer> sumResultsFromDifferentMonads(Option<Integer> i1, Result<Integer> i2) {
        record TwoInts(Option<Integer> i1, Result<Integer> i2) {};
        return switch (new TwoInts(i1,i2)) {
        case TwoInts(Some<Integer>(var x1), Success<Integer>(var x2)) -> Success.of(sum(x1, x2));
        default -> i2.isFailure() ? i2 : Failure.of(new NoSuchElementException()); 
        };
    }
```

Объявляем локальную (!) запись ```TwoInts```, создаем экземпляр той записи и затем сравниваем её с различными вариантами этой записи  с декомпозицией на компоненты.

На Scala этот код будет выглядеть так. 
```scala
    def sumScala(i1: Option[Int], i2: Try[Int]) : Option[Int] = {
      (i1, i2) match {
		case (Some(x1), Success(x2)) => Some(x1 + x2)
        case (_, Failure(f)) => i2
        case (None, _) => None()
        }
    }
```
На Scala покрасивей будет конечно чем в Java, но идея та же. Вместо записи ```TwoInts``` используем безымянный кортеж ```(i1, i2)```, используем символ ```"_"``` когда конкретное значение компоненты нас не интересует, то есть любое значение. Java пока не умеет игнорировать компоненты, возможно появится в будущем.

Мне не удалось написать последнюю функцию на Kotlin. Уважаемые читатели, кто знает другие FP языки. Напишите пожалуйста в комментариях как будет выглядеть последняя функция на вашем языке (Kotlin, Haskell, Rust, др.) Будет интересно сравнить с Java.

## Проблемы

JEP 406 еще не финализирована, находится в 4-ой стадии preview в Java 20, которая на момент написания статьи (февраль 2023) еще официально не вышла. Автор экспериментировал с последней доступной версией Java 19 и последней версией Eclipse Version: 2023-03 M2 (4.27.0 M2).

- Eclipse не может скомпилировать пример 4, не может вывести тип переменной port
- JDK 19 не может скомпилировать пример 3 и 4, говорит что  switch не покрывает все возможные варианты. 

Последняя проблема похожа на ошибку компилятора, аналогичный случай описан на https://stackoverflow.com/questions/73787918/java-19-compiler-issues-when-trying-record-patterns-in-switch-expressions, лечится добавлением default кейса. 

Фичи еще находятся в preview, надеюсь в финальной версии Java 20 это будет исправлено.

## Имплементация Result

Я решил взять Kotlin Result как исходный образец API и портировал его в Java. С результатом можно ознакомиться в репозитории на [GitHub](https://github.com/skopylov58/java-kotlin-result). Почему Kotlin? Во первых, у Kotlin очень хорошая стандартная библиотека, во вторых - не хотелось изобретать новые API.

## Выводы

В последние годы в Java идет тихая функциональная революция

- С использованием запечатанных (sealed) классов и записей (records) стало возможным эффективное моделирование данных в функциональном стиле как алгебраических типов данных (ADT).
- Сопоставление записей с образцом (records pattern matching) с одновременной деконструкцией записи на компоненты является сильной стороной функциональных языков и теперь это возможно в Java.

Конечно Java не станет чистым функциональным языком, однако эти новые возможности значительно обогатят экосистему Java.

Хотя для меня странным выглядит то, что несмотря на большой крен в сторону FP, в стандартной библиотеке Java до сих пор нет таких функциональных примитивов как Result, Either, Option и других (```java.util.Optional``` не является запечатанным классом и не поддается декомпозиции при сопоставлении с образцом). У кого нибудь есть объяснение этому факту?

С уважением   
Сергей Копылов   



