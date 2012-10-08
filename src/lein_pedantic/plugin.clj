(ns lein-pedantic.plugin
  (:require [robert.hooke :as hooke]
            [cemerick.pomegranate.aether]
            [pedantic.core :as pedantic]
            [clojure.string :as string]))

(defn- insert-exclusion
  "Insert an exclusion into spec."
  [spec x]
  (if (contains? (apply hash-map spec) :exclusions)
    (vec (mapcat (fn [[k v]] (if (= k :exclusions)
                              [k (conj v x)]
                              [k v])) (partition 2 spec)))
    (conj spec :exclusions [x])))

(defn help-message [used desired]
  (let [name (first (last used))]
    (cond (= (count used) 1)
          (str "Please use "
               (insert-exclusion (first desired) name)
               " to get " (last used)
               " or remove " (first used)
               " to get " (last desired) ".")
          (= (count desired) 1)
          (str "Please use "
               (insert-exclusion (first used) name)
               " to get " (last desired)
               " or remove " (first desired)
               " to get " (last used) ".")
          :else
          (str "Please use "
               (insert-exclusion (first used) name)
               " to get " (last desired)
               " or use "
               (insert-exclusion (first desired) name)
               " to get " (last used) "."))))

(defn create-msg [overrulled nl]
  (string/join (str nl nl)
               (for [[used desired] overrulled]
                 (str (string/join " -> " used) nl
                      "  is overrulling" nl
                      (string/join " -> " desired) nl nl
                      (help-message used desired)))))

(defn failure-msg [overrulled]
  (let [nl (System/getProperty "line.separator")]
    (str "Failing dependency resolution because:" nl nl
         (create-msg overrulled nl))))

(defn warning-msg [overrulled]
  (let [nl (System/getProperty "line.separator")]
    (str "WARNING - dependency conflicts found but ignored:" nl nl
         (create-msg overrulled nl))))


;We're hooking a private method and pulling its arguments.
;This will probably break sometime in the future.
(defn pedantic-deps [get-dependencies dependency-key project & args]
  (let [deps (get project dependency-key)
        resolve-deps (fn [x]
                       (get-dependencies dependency-key (assoc project dependency-key x)))
        map-to-deps (fn [coords] (into {}
                                      (map #(vector % (resolve-deps [%]))
                                           coords)))
        result (apply get-dependencies dependency-key project args)
        overrulled (pedantic/determine-overrulled result
                                                  (map-to-deps deps))]
    (if (empty? overrulled)
      result
      (if (= (get project :pedantic) :warn)
        (do (println (warning-msg overrulled))
            result)
        (leiningen.core.main/abort (failure-msg overrulled))))))

(defn hooks []
  "Run anytime deps are gathered"
  (hooke/add-hook #'leiningen.core.classpath/get-dependencies
                  #'pedantic-deps))


(defn middleware [project]
  "Make the repl default to :warn"
  (if (some :dependencies (:included-profiles (meta project)))
    (update-in project [:pedantic] #(if (nil? %) :warn %))
    project))