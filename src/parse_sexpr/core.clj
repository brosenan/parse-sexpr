(ns parse-sexpr.core)

(defn pattern-symbols [pattern]
  (cond
    (symbol? pattern) [pattern]
    (list? pattern) (pattern-symbols (last pattern))
    (vector? pattern) (cond (empty? pattern)
                            []
                            :else
                            (concat (pattern-symbols (first pattern)) (pattern-symbols (vec (rest pattern)))))
    :else []))

(defn list-symbol [sym]
  (symbol (str (name sym) "'s")))

 (defmacro with-empty-lists [pattern expr]
  (cond
    (symbol? pattern) `(let [~(list-symbol pattern) '()] ~expr)
    (vector? pattern) (if (empty? pattern)
                     expr
                     ; else
                     `(with-empty-lists ~(first pattern) (with-empty-lists ~(vec (rest pattern)) ~expr)))
    (list? pattern) `(with-empty-lists ~(last pattern) ~expr)
    :else expr))


(defmacro append-values [pattern expr]
  (cond
    (symbol? pattern) `(let [~(list-symbol pattern) (conj ~(list-symbol pattern) ~pattern)]
                         ~expr)
    (vector? pattern) (if (empty? pattern)
                        expr
                        ; else
                        `(append-values ~(first pattern)
                                        (append-values ~(vec (rest pattern)) ~expr)))
    (list? pattern) `(append-values ~(last pattern) ~expr)
    :else expr))

(defmacro parse-sexpr [pattern value expr]
  (let [err `(fn [] (throw (Exception. (str "Expected " '~pattern "; received: " ~(pr-str (eval value))))))]
    (cond
      (symbol? pattern) `(let [~pattern ~value] ~expr)
      (list? pattern) `(parse-sexpr ~(last pattern) ~value (cond ~pattern
                                                                 ~expr
                                                                 :else
                                                                 (~err)))
      (vector? pattern) `(cond (sequential? ~value)
                               ~(if (empty? pattern)
                                  expr
                                        ; else
                                  (if (= (last pattern) '...)
                                    (let [pattern-len (dec (count pattern))
                                          pattern (vec (take pattern-len pattern))
                                          vars (pattern-symbols pattern)
                                          list-vars (vec (map list-symbol vars))]
                                      `(loop [~'$args ~value
                                              ~list-vars ~(vec (for [n (range pattern-len)]
                                                                 []))]
                                         (cond (empty? ~'$args)
                                               ~expr
                                               :else
                                               (parse-sexpr ~pattern (take ~pattern-len ~'$args)
                                                            (append-values ~pattern
                                                                           (recur (drop ~pattern-len ~'$args) ~list-vars))))))
                                        ; else
                                    `(parse-sexpr ~(first pattern) (first ~value)
                                                  (parse-sexpr ~(vec (rest pattern)) (rest ~value) ~expr))))
                               :else
                               (throw (Exception. (str "Expected vector or list; received: " ~(pr-str value)))))
      :else `(if (not= ~pattern ~value) (~err)
                 ; else
                 ~expr))))





