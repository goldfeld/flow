(ns flow.core-test
  (:require [flow.core :as flow]
            [schema.core :as s]
            [cemerick.cljs.test
             :include-macros true :refer [deftest is testing]]))

(deftest parse-simple-config->flow
  (let [Block (s/one flow/Block "Block")]
    (let [f1 (flow/config->flow
              [["working.." [25 25 :min] :exec "emacs my-work-file.clj"]
               ["playing!" [5 5 :min] :exec "emacs -f tetris"]
               ["break" [:.. 15 :min]]])]
      
      (is (= (repeat 5 :min) (map second f1)))
      (is (= [25 5 25 5 15] (map first f1))))
    (let [f2 (flow/config->flow
              [["gamedev" [2 :. 0 3 :min] :exec "exhibit 3"]
               ["projects" [2 5 :min] :exec "exhibit 5"]
               ["job" [2 4 6 :min] :exec "exhibit 1"]
               ["catchup" [:.. 2 :min] :exec "exhibit 4"]
               ["break" [:.. 4 :min] :exec "exhibit 6 && hooker 33"]])]
      (is (= [2 2 2  5 4  0 6  3  2 4] (map first f2)))
      (is (s/validate [Block Block Block
                       Block Block  Block Block
                       Block  Block Block] f2)))))
