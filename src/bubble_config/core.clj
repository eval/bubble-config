(ns bubble-config.core
  (:refer-clojure :exclude [print])
  (:require [aero.alpha.core]
            [aero.core :as aero]
            [babashka.cli :as cli]
            [bubble-config.util :as util :refer [whenp]]
            [clojure.java.io :as io]
            [puget.printer :refer [pprint]]))

(def defaults {:config  "config.edn"
               :env-var "BBL_ENV"})

(def ^:private ^:dynamic *envs-seen* nil)

(defmethod aero.alpha.core/eval-tagged-literal 'env
  [tagged-literal opts env ks]
  (when *envs-seen*
    (swap! *envs-seen* conj (keys (:form tagged-literal))))
  (aero.alpha.core/expand-case (:env opts) tagged-literal opts env ks))


(defn- bbl-config
  ([] (bbl-config nil))
  ([opts]
   (into defaults
         (remove (comp nil? val) opts))))

(defn- read-config
  ([file]
   (read-config file nil))
  ([file opts]
   (binding [*envs-seen* (atom '())]
     (let [result (aero/read-config file opts)
           envs   (apply list (first @*envs-seen*))]
       {:bubble-config/available-envs envs
        :bubble-config/result         result}))))

(defn- available-envs
  [{:keys [config] :as _bbl-config}]
  (:bubble-config/available-envs (read-config config {})))

(defn- default-env [bbl-config]
  (first (available-envs bbl-config)))

(defn- env-from-var [{:keys [env-var] :as _bbl-config}]
  (keyword (System/getenv env-var)))

(defn- current-env
  ([bbl-config] (current-env bbl-config nil))
  ([bbl-config requested-env]
   (or (keyword requested-env)
       (env-from-var bbl-config)
       (default-env bbl-config))))

(defn- assert-valid-env! [requested envs]
  (when-let [envs (not-empty (set envs))]
    (assert (envs requested)
            (str "Unknown env " (pr-str requested) "."
                 " Should be one of " (pr-str envs) "."))))

(defn config
  ;; TODO accept help as well?
  {:org.babashka/cli {#_#_:restrict [:config :env :bbl/config :bbl/env]
                      :spec         {:env        {:alias  :e
                                                  :coerce :keyword}
                                     :config     {:alias :c}
                                     :bbl/config {:alias :bbl/c}
                                     :bbl/env    {:alias  :bbl/e
                                                  :coerce :keyword}}}}
  ([] (config nil))
  ([{env         :env
     config      :config
     nsed-env    :bbl/env
     nsed-config :bbl/config :as cfg}]
   #_(prn :args args :profile profile)
   (let [config          (or nsed-config config)
         config          (if (string? config)
                           (or (io/resource config) config)
                           config)
         {:keys [config]
          :as   bbl-cfg} (bbl-config (assoc cfg :config config))
         env             (current-env bbl-cfg (or nsed-env env))
         {:bubble-config/keys [available-envs
                               result]}
         (read-config config {:env env})]
     (assert-valid-env! env available-envs)
     (with-meta (or (:bubble-config/root result) result)
       {:bubble-config/available-envs available-envs
        :bubble-config/config-file    config
        :bubble-config/current-env    (or env (current-env bbl-cfg))}))))

(defn sample
  [& _]
  (println (slurp (io/resource "sample.edn"))))

(defn- print-help [spec]
  (println
   (str "Print config" \newline
        \newline
        "Usage: cli [OPTIONS] \n\nOPTIONS\n"
        (cli/format-opts spec)
        \newline \newline
        "ENVIRONMENT VARIABLES" \newline
        "  BBL_ENV   Environment (env-flag takes precedence)."
        \newline)))

(def ^:private print-spec
  {:restrict [:env :help :config :color :plain]
   :spec     {:help   {:alias :h
                       :desc "This help message."}
              :env    {:alias :e :coerce :keyword
                       :desc  "Environment."}
              :config {:alias :c
                       :default (:config defaults)
                       :desc  "Config file."}}})

(defn print
  "Print config"
  {:org.babashka/cli print-spec}
  [{e :env c :config help :help :as cli-opts}]
  #_(prn :cli-opts cli-opts)
  (if help
    (print-help (assoc print-spec :order [:env :config :help]))
    (binding [*print-meta* true]
      (pprint (config {:config c :env e})
              {:print-color (not (util/no-color? cli-opts))}))))

(comment

  #_:end)
