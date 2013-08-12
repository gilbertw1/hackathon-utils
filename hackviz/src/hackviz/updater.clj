(ns hackviz.updater
  (:require [hackviz.github :as gh]
            [hackviz.turbine :as turbine]
            [overtone.at-at :as at]
            [hackviz.global :as g]))

(defn new-commit-events-for-repo [{:keys [name owner team ts]}]
  (println "Retrieving Events: " name " - " owner " - " team " - " ts)
  (doall (gh/commit-events-since owner team name ts)))

(defn update-repo-atom [repo values]
  (swap! repo #(merge % values)))

(defn newest-timestamp [commit-events]
  (when (seq commit-events)
    (let [times (map :ts commit-events)]
      (apply max times))))

(defn update-repo [repo]
  (let [commit-events (new-commit-events-for-repo @repo)]
    (println "Adding " (count commit-events) " to " (:name @repo))
    (when (seq commit-events)
        (turbine/add-commit-events commit-events)
        (let [newest-ts (newest-timestamp commit-events)]
          (update-repo-atom repo {:ts newest-ts})))))

(defn schedule-continual-updates [repo]
  (at/every @g/update-delay #(update-repo repo) g/scheduler-pool :initial-delay 1000))