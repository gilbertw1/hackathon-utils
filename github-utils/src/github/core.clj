(ns github.core
  (:require [clojure.java.io :as io]
            [cheshire.core :as json]
            [clj-http.client :as client]
            [tentacles.repos :as repos]
            [tentacles.orgs :as orgs]
            [clojure.string :as str])
  (:use [clojure.core.match :only [match]])
  (:gen-class))

(def resource-path (-> "config.json" io/resource))
(def repo-opts {:public false :has-issues false :has-wikis false :has-downloads false})
(def team-opts {:permission "push"})
(def auth (atom {}))

(defn read-conf [conf-file]
  (json/parse-string (slurp conf-file) true))

(defn get-conf-file [conf-file]
  (if (nil? conf-file) resource-path conf-file))

(defn set-auth! [conf]
  (swap! auth #(conj % [:oauth-token (:github-token conf)])))

(defn team-name [team]
  (str/lower-case (str/replace team #"\s" "-")))

(defn repo-name [team]
  (str (team-name team) "-repo"))

(defn get-org-repos [org]
  (repos/org-repos org (assoc @auth :all-pages true)))

(defn get-org-teams [org]
  (orgs/teams org (assoc @auth :all-pages true)))

(defn create-repo [team org]
  (let [name (repo-name team)]
    (println "Creating repo: " name)
    (repos/create-org-repo org name (merge repo-opts @auth {:description (str name " team repository")}))))

(defn get-names [entity]
  (map :name entity))

(defn name-set [entities]
  (-> entities get-names set))

(defn find-missing-teams [teams entities munger]
  (filter (complement (name-set entities)) (map munger (name-set teams))))

(defn create-repos [{:keys [org teams]}]
  (let [org-repos (get-org-repos org)
        missing-teams (find-missing-teams teams org-repos repo-name)]
    (doseq [team missing-teams]
      (create-repo team org))))

(defn create-team [team org]
  (let [name (team-name team)
        repo (repo-name team)]
    (println "Creating team: " name " with access to " repo)
    (orgs/create-team org name (merge team-opts @auth {:repo-names [(str org "/" repo)]}))))

(defn create-teams [{:keys [org teams]}]
  (let [org-teams (get-org-teams org)
        teams-to-create (find-missing-teams teams org-teams team-name)]
    (doseq [team teams-to-create]
      (create-team team org))))

(defn is-member? [user team-id]
  (orgs/team-member? team-id user @auth))

(defn get-team-id [team org-teams]
  (->> org-teams
       (filter #(= (:name %) (team-name team)))
       (first)
       (:id)))

(defn add-user-team [user team-id]
  (println "Adding user (" user ") to team with id (" team-id ")")
  (orgs/add-team-member team-id user @auth))

(defn add-users-team [{:keys [name members]} org-teams]
  (let [team-id (get-team-id name org-teams)
        not-members (filter #(not (is-member? % team-id)) members)]
    (doseq [user not-members]
      (add-user-team user team-id))))

(defn add-users-teams [{:keys [org teams]}]
  (let [org-teams (get-org-teams org)]
    (doseq [team teams] (add-users-team team org-teams))))

(defn disable-team [team]
  (let [team-name (:name team)
        team-id (:id team)]
    (when-not (or (#{"Owners"} team-name) (nil? team-name))
      (println "Disabling team: " team-name)
      (orgs/edit-team team-id (merge @auth {:permission "pull"})))))

(defn disable-teams [{:keys [org]}]
  (let [org-teams (get-org-teams org)]
    (doseq [team org-teams] (disable-team team))))

(defn -main [& [cmd conf-file]]
  (let [conf (read-conf (get-conf-file conf-file))]
    (set-auth! conf)
    (match [cmd]
           ["add-repos"] (do
                         (create-repos conf))
           ["add-teams"] (do
                         (create-repos conf)
                         (create-teams conf))
           ["add-users"] (do
                         (create-repos conf)
                         (create-teams conf)
                         (add-users-teams conf))
           ["disable-teams"] (do
                             (disable-teams conf))
           :else (println "Invalid Target (valid targets - add-repos, add-teams, add-users, disable-teams)"))))