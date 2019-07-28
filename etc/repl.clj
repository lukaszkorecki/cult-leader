(ns R
  (:refer-clojure :exclude [find-ns])
  (:require [clojure.test :as test]
            [clojure.repl :as repl]
            [kaocha.repl]
            clojure.pprint
            [cemerick.pomegranate]
            [clojure.tools.namespace.find :as ns.find]
            [clojure.tools.namespace.repl :as ns.repl]
            [clojure.java.io :as io])
  (:import (java.io File)))

(ns.repl/disable-reload! *ns*)

(defn help []
  (println (str ">>> in ns " *ns*))
  (println "(R/t) - run all tests
(R/find-test-ns  #\"some-regex\") - find test namespace matching regex
(R/find-ns  #\"some-regex\") - find namespace matching regex
(R/t (R/find-test-ns  #\"some-regex\")) - run tests for matching namespaces
(R/t! (R/find-test-ns  #\"some-regex\")) - run tests for matching namespaces but also refresh (dangerous!)
(R/time+ \"tag\"  (some expr)) - like time, but better
(R/refresh) - refresh all namespaces
(scrtach/refresh-all) - refresh all project + dep namespaces
(R/pp) alias for clojure.pprint/pprint
(R/list-ns) - find all namespaces in SRC
(R/start-system! 'some.user-ns) - refresh and start a system in some.user-ns
(R/stop-system! 'some.user-ns) - stop a system in some.user-ns
(R/add-dependency ['cheshire \"5.8.1\"]) - add a library to current repl process"))

(defn init! []
  (ns scratch)
  (ns.repl/disable-reload! *ns*)
  (help))

(def refresh ns.repl/refresh)
(def refresh-all ns.repl/refresh-all)
(def pp clojure.pprint/pprint)

(defn list-ns
  "Return list of symbols of namespaces found in src dir"
  ([root]
   (ns.find/find-namespaces-in-dir (File. root)))
  ([]
   (list-ns "./src/")))

(defn find-ns [re]
  (filter #(re-find re (str %)) (list-ns)))

(defn find-test-ns [re]
  (filter #(re-find re (str %)) (list-ns "./test/")))

(defn t
  ([]
   (kaocha.repl/run :unit))
  ([ns-list]
   (apply kaocha.repl/run ns-list)))

(defn t!
  ([]
   (refresh)
   (kaocha.repl/run :unit))
  ([ns-list]
   (refresh)
   (apply kaocha.repl/run ns-list)))

(def system-status (atom {}))

(defn start-system!
  "Given a namespace, usually some-service.user, do the following:
  - refresh
  - require the user ns
  - start  system, invoking somer-service.user/start
  Warning: best if the system is not running, or things will go south"
  [an-ns]
  (if (get @system-status an-ns)
    (println "!!  System possibly running")
    (do
      (println "!! Refreshing and reloading " an-ns)
      (refresh)
      (require an-ns)
      (let [f (ns-resolve an-ns 'start)]
        (f)
        (swap! system-status (fn [s] (assoc s an-ns true)))))))

(defn stop-system!
  "Given a namespace, usually some-service.user, stop the system"
  [an-ns]
  (let [f (ns-resolve an-ns 'stop)]
    (f)
    (swap! system-status (fn [s] (assoc s an-ns false)))))

(defn add-dependency
  "Adds dependencies within a running REPL
  Stolen from https://clojureverse.org/t/how-to-use-a-dependency-from-clojure-repl-without-starting-a-lein-project/1596/5
  Usage: (add-dependency ['cheshire \"5.8.1\"])"
  [dep-vec]
  (cemerick.pomegranate/add-dependencies
   :coordinates [dep-vec]
   :repositories (merge @(resolve 'cemerick.pomegranate.aether/maven-central)
                        {"clojars" "https://clojars.org/repo"})))

(init!)