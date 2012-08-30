(ns lein-pedantic.plugin
  (:require [robert.hooke :as hooke]
            [cemerick.pomegranate.aether]
            [pedantic.core :as pedantic]
            [clojure.string :as string]))

(defn create-msg [overrulled]
  (let [nl (System/getProperty "line.separator")]
    (string/join 
     (for [[used desired] overrulled]
       (let [name (first (last used))]
         (str "Failing dependency resolution because" nl
              (string/join " -> " used) nl
              "is overrulling" nl
              (string/join " -> " desired) nl
              "Please use "
              (conj (first used) :exclusions [name])
              " to get " (last desired)
              " or use "
              (conj (first desired) :exclusions [name])
              " to get " (last used) "."))))))


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