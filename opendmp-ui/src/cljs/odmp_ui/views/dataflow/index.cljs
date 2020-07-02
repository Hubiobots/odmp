;; Copyright 2020 The Open Data Management Platform contributors.

;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at

;; http://www.apache.org/licenses/LICENSE-2.0

;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns odmp-ui.views.dataflow.index
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [odmp-ui.components.common :as tcom]
            [odmp-ui.components.chips :as chips]
            [odmp-ui.util.styles :as style]
            [odmp-ui.subs :as subs]
            ["@material-ui/core/Button" :default Button]
            ["@material-ui/core/Tooltip" :default Tooltip]
            ["@material-ui/core/Table" :default Table]
            ["@material-ui/core/TableHead" :default TableHead]
            ["@material-ui/core/TableBody" :default TableBody]
            ["@material-ui/core/TableCell" :default TableCell]
            ["@material-ui/core/TableContainer" :default TableContainer]
            ["@material-ui/core/TableHead" :default TableHead]
            ["@material-ui/core/TablePagination" :default TablePagination]
            ["@material-ui/core/TableRow" :default TableRow]
            ["@material-ui/core/TableSortLabel" :default TableSortLabel]
            ["@material-ui/core/Grid" :default Grid]
            ["@material-ui/core/Toolbar" :default Toolbar]
            ["@material-ui/core/Typography" :default Typography]
            ["@material-ui/core/Paper" :default Paper]
            ["@material-ui/icons/Add" :default AddIcon]))


(defn dataflow-index-styles [^js/Mui.Theme theme]
  {:right {:float :right}
   :dataflow-item-cell {:cursor :pointer}})

(defn dataflow-row [dataflow classes]
^{:key (:id dataflow)}
  [:> TableRow  {:hover true :tabIndex -1}
   [:> TableCell {:class (:dataflow-item-cell classes)}
    [:> Tooltip {:title (:description dataflow) :placement "bottom-start"}
     [:p (:name dataflow)]]]
   [:> TableCell (chips/status-chip (:status dataflow))]
   [:> TableCell (chips/health-chip (get-in dataflow [:health :state]))]])

(defn table-header []
  [:> TableHead
   [:> TableRow
    [:> TableCell "Name"]
    [:> TableCell "Status"]
    [:> TableCell "Health"]]])

(defn toolbar [classes]
  [:> Toolbar {:disableGutters true}
   [:> Grid {:container true :spacing 2}
    [:> Grid {:item true :xs 9}]
    [:> Grid {:item true :xs 3}
     [:> Button {:color :primary :variant :contained :disableElevation true :class (:right classes)}
      [:> AddIcon] "Create"]]]])


(defn dataflow-index []
  (let [dataflows (rf/subscribe [::subs/dataflows])]
    (style/let [classes dataflow-index-styles]
      (tcom/full-content-ui {:title "Data Flows"}
        [:div 
         (toolbar classes)
         [:> Paper
          [:> TableContainer
           [:> Table
            (table-header)
            [:> TableBody
             (map #(dataflow-row % classes) @dataflows)]]]]]))))
