(ns hackviz.github
  (:require [tentacles.repos :as repos]
            [clj-http.client :as client]
            [cheshire.core :as json]
            [clj-time.format :refer :all]
            [clj-time.coerce :refer :all]
            [hackviz.global :as g]))

(def iso-formatter (formatter "yyyy-MM-dd'T'HH:mm:ss'Z'"))

(defn auth [] 
  {:oauth-token @g/github-token})

(defrecord CommitEvent [time owner author team repo additions deletions])

(defn ts-to-iso [ts]
  (unparse iso-formatter (from-long ts)))

(defn iso-to-ts [iso]
  (to-long (parse iso-formatter iso)))

(defn has-keys? [c]
  (seq (keys c)))

(defn retrieve-raw-commits [owner repo ts]
  (repos/commits owner repo (merge (auth) {:since (ts-to-iso ts) :all-pages true})))

(defn get-commit-details [commit]
  (let [resp (client/get (:url commit) {:headers {"Authorization" (str "token " @g/github-token)}})
        body (:body resp)]
    (json/parse-string body true)))

(defn convert-event [commit owner team repo]
  (let [details (get-commit-details commit)
        author (-> details :author :login)
        ts (iso-to-ts (-> details :commit :author :date))
        ups (-> details :stats :additions)
        downs (-> details :stats :deletions)]
    (CommitEvent. ts owner author team repo ups downs)))

(defn commit-events-since [owner team repo ts]
  (try
    (let [raw-commits (filter has-keys? (retrieve-raw-commits owner repo ts))]
      (map #(convert-event % owner team repo) raw-commits))
    (catch RuntimeException e (println "Failed retrieve commits (" owner " - " team " - " repo " - " ts "), Exception: " e))))