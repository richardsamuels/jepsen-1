(ns jepsen.fauna
  ; TODO: rename to faunadb for consistency
  (:require
            [clojure.tools.logging :refer :all]
            [jepsen [cli :as cli]
             [core :as jepsen]
             [db :as db]
             [generator :as gen]
             [tests :as tests]]
            [jepsen.os.debian :as debian]
            [jepsen.faunadb.auto :as auto]))

(defn db
  "FaunaDB DB"
  []
  (reify db/DB
    (setup! [_ test node]
      (auto/install! test)
      (auto/configure! test node)
      (when (:clear-cache test)
        (auto/clear-cache!))
      (if (auto/cache-valid? test)
        (auto/unpack-cache!)
        ; We have to go through the whole setup process, then we'll build a
        ; cache for next time
        (do (auto/start! test node)
            (auto/init! test node)
            (auto/stop! test node)
            (auto/build-cache! test)))
      (auto/start! test node))

    (teardown! [_ test node]
      (info node "tearing down FaunaDB")
      (auto/teardown! test node))

    db/LogFiles
    (log-files [_ test node]
      ["/var/log/faunadb/core.log"
       "/var/log/faunadb/query.log"
       "/var/log/faunadb/exception.log"])))

(defn basic-test
  "Sets up the test parameters common to all tests."
  [opts]
  (merge
    tests/noop-test
    {:name    (str "fauna"
                   " " (:name opts)
                   (when (:strong-read opts)
                     " strong-read")
                   (when (:at-query opts)
                     " at-query")
                   (when (:fixed-instances opts)
                     " fixed-instances")
                   (when (:serialized-indices opts)
                     " serialized-indices")
                   " nemesis:" (:name (:nemesis opts)))
     :os      debian/os
     :db      (db)
     :client  (:client (:client opts))
     :nemesis (:nemesis (:nemesis opts))
     :generator (gen/phases
                  (->> (gen/nemesis (:during (:nemesis opts))
                                    (:during (:client opts)))
                       (gen/time-limit (:time-limit opts)))
                  (gen/log "Nemesis terminating")
                  (gen/nemesis (:final (:nemesis opts)))
                  ; Final client
                  (gen/clients (:final (:client opts))))}
    (dissoc opts :name :client :nemesis)))
