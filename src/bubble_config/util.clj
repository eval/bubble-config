(ns bubble-config.util
  (:require [babashka.fs :as fs]
            [babashka.process :as p]))

(defn whenp [v & preds]
  (when (and v ((apply every-pred preds) v))
    v))

(defn is-tty
  [fd key]
  (-> ["test" "-t" (str fd)]
      (p/process {key :inherit :env {}})
      deref
      :exit
      (= 0)))

(def tty-out? (memoize #(is-tty 1 :out)))

(defn- plain-mode? [{:keys [plain] :as _cli-opts}]
  (or (fs/windows?) plain (not (tty-out?))))

(defn no-color? [{:keys [color] :as cli-opts}]
  (or (false? color)
      (plain-mode? cli-opts)
      (System/getenv "NO_COLOR")
      (= "dumb" (System/getenv "TERM"))))
