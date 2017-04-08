(ns parse-sexpr.core-test
  (:require [midje.sweet :refer :all]
            [parse-sexpr.core :refer :all]))

[[:chapter "Introduction"]]
"Clojure macros are great, but they do not always give the best error messages.
If we wish to help users understand what they did wrong, we should catch syntax errors as soon as we can.
These syntax errors include wrong parameter types provided to macros, wrong number of arguments in a certain case, etc."

[[:chapter "parse-sexpr"]]
"This macro receives three arguments:
1. A pattern to be matched,
2. The value to match against the pattern, and
3. An expression based on the bindings defined in the pattern
The function returns the evaluated expression based on the bindings received by matching the value against the pattern.
The function will throw an exception when the value does not match the patter."

"A keyword is matched to itself."
(fact
 (parse-sexpr :my-keyword :my-keyword 12) => 12)

"If the keyword does not match, an exception is thrown"
(fact
 (parse-sexpr :my-keyword :something-else 12) => (throws "Expected :my-keyword; received: :something-else"))

"If the pattern is a symbol, the value is bound to it."
(fact
 (parse-sexpr foo 3 (+ foo 2)) => 5)

"If the pattern is a list (function call), it binds the value to the last argument."
(fact
 (parse-sexpr (number? three) 3 (+ 2 three)) => 5)

"The function call is actually a guard to check the value.  If the guard fails, an exception is thrown"
(fact
 (parse-sexpr (number? three) "three" three) => (throws "Expected (number? three); received: \"three\""))

"If the pattern is a vector, the matching value is expected to be sequential (vector or list)."
(fact
 (parse-sexpr [] 3 "foo") => (throws "Expected vector or list; received: 3")
 (parse-sexpr [] [] "bar") => "bar"
 (parse-sexpr [] '() "bar") => "bar")

"Elements in a vector are matched recursively to create a sequence."
(fact
 (parse-sexpr [(number? a) [(number? b) (number? c)]] [1 [2 3]] (+ a b c)) => 6)

"Error messages remain informative inside vector patterns."
(fact
 (parse-sexpr [(number? a) [(number? b) (number? c)]] [1 [2 :three]] (+ a b c))
 => (throws "Expected (number? c); received: :three"))

"If the last element of a vector pattern is an elipsis (`...`), bindings are made into sequences."
(fact
 (parse-sexpr [a b ...] [1 2 3 4] [a's b's]) => [[1 3] [2 4]])

[[:section "Under the Hood"]]
"The function `pattern-symbols` lists all matched symtols in a pattern."
(fact
 (pattern-symbols :kw) => []
 (pattern-symbols 'foo) => '[foo]
 (pattern-symbols '(number? x)) => '[x]
 (pattern-symbols '[x y :kw z]) => '[x y z])
"The `with-empty-lists` macro evaluates an expression in an environment where for each binding variable x in the given pattern
there exists a corresponding variable x's initialized to an empty list."
(fact
 (with-empty-lists :foo 3) => 3
 (with-empty-lists x x's) => []
 (with-empty-lists [x y] [x's y's]) => [[] []]
 (with-empty-lists (number? x) x's) => [])

"The `append-values` macro takes a pattern and an expression.
It evaluates the expression where for each variable bound in the pattern the variable's value
is appended to its corresponding list."
(fact
 (let [x's [1]
       y's [2]
       x 2
       y 3]
   (append-values :foo 3) => 3
   (append-values x x's) => [1 2]
   (append-values [x y] [x's y's]) => [[1 2] [2 3]]
   (append-values (number? x) x's) => [1 2]))
