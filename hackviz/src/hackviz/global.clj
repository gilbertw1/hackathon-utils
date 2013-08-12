(ns hackviz.global
  (:require [overtone.at-at :as at]))

(def github-token (atom ""))
(def turbinedb-url (atom "http://localhost:8080"))
(def update-delay (atom (* 1000 60)))
(def server-base (atom "http://localhost:8080"))
(def scheduler-pool (at/mk-pool))
(def repositories (atom []))

(defn update-atom [atom value]
  (if value (reset! atom value)))

(defn initialize-atoms [conf]
  (update-atom github-token (:github-token conf))
  (update-atom turbinedb-url (:turbinedb-url conf))
  (update-atom update-delay (:update-delay conf))
  (update-atom server-base (:server-base conf)))