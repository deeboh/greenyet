(ns greenyet.view.patchwork
  (:require [clojure.string :as str]
            [greenyet.utils :as utils]
            [greenyet.view.host-component :as host-component]
            [hiccup.core :refer [h html]]))

(defn- collapse-all-green-hosts [host-entries]
  (if (every? (fn [[_ {color :color}]] (= :green color)) host-entries)
    (take 1 host-entries)
    host-entries))

(defn- hosts-for-environment [host-list environment]
  (->> host-list
       (filter (fn [[{host-environment :environment} _]]
                 (= environment host-environment)))
       (sort-by (fn [[{index :index} _]] index))
       collapse-all-green-hosts))

(defn- system-row [environments host-list]
  (map (fn [environment]
         (hosts-for-environment host-list environment))
       environments))

(defn- environment-table [environments host-list]
  (->> host-list
       (group-by (fn [[{system :system} _]] system))
       (sort (fn [[system1 _] [system2 _]]
               (compare system1 system2)))
       (map (fn [[_ system-host-list]]
              (system-row environments system-host-list)))))

(defn- environment-table-to-patchwork [environments table]
  (apply merge-with concat (map (partial zipmap environments) table)))

(defn- patchwork-as-html [patchwork params]
  (html
   [:header
    (if-not (empty? params)
      [:a.reset-selection {:href "?"}
       "Reset selection"]
      [:span.reset-selection
       "Reset selection"])
    (if-not (get params "hideGreen")
      [:a.hide-green {:href (utils/link-select params "hideGreen" "true")}
       "Unhealthy systems only"]
      [:span.hide-green
       "Unhealthy systems only"])]
   (for [[environment host-status] patchwork]
     [:div.environment-wrapper
      [:ol.patchwork {:class environment}
       (for [[host status] host-status]
         (host-component/render host status params))]])))


(defn- in-template [html template]
  (str/replace template
               "<!-- BODY -->"
               html))

(defn- environments [host-status-pairs]
  (->> host-status-pairs
       (map first)
       (map :environment)
       distinct))

(defn render [host-status-pairs page-template environment-names params]
  (let [the-environments (utils/prefer-order-of environment-names
                                                (environments host-status-pairs)
                                                str/lower-case)
        rows (environment-table the-environments host-status-pairs)

        patchwork (environment-table-to-patchwork the-environments rows)]
    (in-template (patchwork-as-html patchwork params)
                 page-template)))
