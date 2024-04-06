(ns solution
  (:require [clojure.string :as str]))

(defn main []
  (let [input (-> "in.txt" slurp str/split-lines)
        a (Double/parseDouble (get input 0))
        b (Double/parseDouble (get input 1))
        c (Double/parseDouble (get input 2))]
    (format "%.1f" (/ (+ (* a 2.0) (* b 3.0) (* c 5.0)) 10.0))))

(println "MEDIA =" (main))
