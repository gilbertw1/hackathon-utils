(ns hackviz.turbine
  (:require [clj-http.client :as client]
            [clojure.string :as string]
            [cheshire.core :as json]
            [hackviz.github :as gh]
            [hackviz.global :as g]
            [clj-time.coerce :refer :all]))

(defn coll-url [db coll]
  (str @g/turbinedb-url "/db/" db "/" coll))

(defn query [q db coll]
  (let [q-json (json/generate-string q)
        url (coll-url db coll)
        res (client/get url {:query-params {"q" q-json}})
        body (:body res)]
    (json/parse-string body true)))

(defn insert-event [commit-event db coll]
  (let [event {:timestamp (:time commit-event), :data commit-event}
        e-json (json/generate-string event)
        url (coll-url db coll)]
    (client/post url {:body e-json})))

(defn add-commit-events [events]
  (doseq [e events] (insert-event e "hackathon" "commits")))

(defn query-commits [q]
  (query q "hackathon" "commits"))

(defn create-matches [criteria]
  (map (fn [[k v]] {k {:eq v}}) criteria))

(defn get-first [key results]
  (-> results first :data first :data first key))

(defn create-query [matches groups reduces]
  {:match (create-matches matches) :group groups :reduce reduces})

(defn newest-commit-ts [owner repo]
  (let [matches (create-matches {:owner owner :repo repo})
        q {:match matches :reduce [{:newest {:max "time"}}]}
        results (query-commits q)
        ts-dbl (-> results first :data first :data first :newest)]
    (long (or ts-dbl 0))))

(defn valid-reducer? [[k v]]
  (and
    (contains? #{"time" "owner" "author" "team" "repo" "additions" "deletions"} k)
    (contains? #{"min" "max" "avg" "count" "sum"} v)))

(defn convert-reducer [[k v]]
  {(str k "-" v) {v k}})

(defn get-reducers [params]
  (if (:metrics params)
    (as-> params m
          (:metrics m)
          (string/split m #",")
          (map #(string/split % #":") m)
          (filter valid-reducer? m)
          (map convert-reducer m))))

(defn convert-group [group]
  (cond (contains? #{"minute" "hour" "day" "month" "year"} group) {"duration" group}
        (contains? #{"owner" "author" "team" "repo"} group) {"segment" group}
        :else nil))

(defn get-groups [params]
  (if (:groups params)
    (as-> params g
          (:groups g)
          (string/split g #",")
          (map convert-group g)
          (remove nil? g))))

(defn valid-filter? [[k v]]
  (contains? #{:repo :owner :author :team} k))

(defn get-filters [params]
  (filter valid-filter? params))

(defn create-query-from-params [params]
  (let [filters (get-filters params)
        groups (get-groups params)
        reducers (get-reducers params)]
    (prn (create-query filters groups reducers))
    (create-query filters groups reducers)))