(ns bubble-config.core-test
  (:require
   [bubble-config.core :as sut]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing are]]
   [aero.core :as aero]))

(defn config-as-string [s]
  (char-array s))

(defn bbl-config [m]
  (update m :config config-as-string))

(deftest available-envs-test
  (testing "simple"
    (is (= '()
           (#'sut/available-envs (bbl-config {:config "{}"})))
        "Yields empty collection when nothing found")
    (is (= '(:a :b)
           (#'sut/available-envs
            (bbl-config {:config "#env{:a 1 :b 2}"})))
        "Yields envs in order of appearance in config")))

(deftest config-test
  (testing "simple"
    (is (= {}
           (sut/config (bbl-config {:config "{}"}))))
    (is (= {:a 1}
           (sut/config (bbl-config {:config "{:a 1}"}))))
    (is (= {:a 1}
           (sut/config (bbl-config {:config "{:bubble-config/root {:a 1}}"})))
        "Yields value of :bubble-config/root if present"))

  (testing "#env"
    (is (= {:dev? true}
           (sut/config (bbl-config {:config
                                    "#env{:dev {:dev? true}
                                          :test {:dev? false}}"})))
        "Uses the first env from the config by default")
    (is (= {:dev? false}
           (sut/config (bbl-config {:config
                                    "#env{:dev {:dev? true}
                                          :test {:dev? false}}"
                                    :env :test})))
        "Uses the env provided")))

(comment



  #_:end)
