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

(defn create-msg [overrulled]
  (let [nl (System/getProperty "line.separator")]
    (str "Failing dependency resolution because:" nl nl
         (string/join (str nl nl)
                      (for [[used desired] overrulled]
                        (str (string/join " -> " used) nl
                               "  is overrulling" nl
                               (string/join " -> " desired) nl nl
                               (help-message used desired)))))))


(defn pedantic-deps [f & args]
  (let [args (apply hash-map args)
        deps (:coordinates args)
        resolve-deps (fn [x]
                       (apply f (apply concat (assoc args :coordinates x))))
        map-to-deps (fn [coords] (into {}
                                      (map #(vector % (resolve-deps [%]))
                                           coords)))
        result (resolve-deps deps)
        overrulled (pedantic/determine-overrulled result
                                                  (map-to-deps deps))]
    (if (empty? overrulled)
      result
      (leiningen.core.main/abort (create-msg overrulled)))))

(defn hooks []
  (hooke/add-hook #'cemerick.pomegranate.aether/resolve-dependencies
                  #'pedantic-deps))
