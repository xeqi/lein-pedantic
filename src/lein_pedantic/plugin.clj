(ns lein-pedantic.plugin
  (:require [robert.hooke :as hooke]
            [cemerick.pomegranate.aether]
            [pedantic.core :as pedantic]
            [clojure.string :as string]))

(defn help-message [used desired]
  (let [name (first (last used))]
    (cond (= (count used) 1)
          (str "Please use "
               (conj (first desired) :exclusions [name])
               " to get " (last used)
               " or remove " (first used)
               " to get " (last desired) ".")
          (= (count desired) 1)
          (str "Please use "
               (conj (first used) :exclusions [name])
               " to get " (last desired)
               " or remove " (first desired)
               " to get " (last used) ".")
          :else
          (str "Please use "
               (conj (first used) :exclusions [name])
               " to get " (last desired)
               " or use "
               (conj (first desired) :exclusions [name])
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