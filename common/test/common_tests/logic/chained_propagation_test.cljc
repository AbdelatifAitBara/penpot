;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC

(ns common-tests.logic.chained-propagation-test
  (:require
   [app.common.files.changes :as ch]
   [app.common.files.changes-builder :as pcb]
   [app.common.files.libraries-common-helpers :as cflh]
   [app.common.logic.libraries :as cll]
   [app.common.logic.shapes :as cls]
   [app.common.pprint :as pp]
   [app.common.types.component :as ctk]
   [app.common.types.file :as  ctf]
   [app.common.uuid :as uuid]
   [clojure.test :as t]
   [common-tests.helpers.compositions :as thc]
   [common-tests.helpers.debug :as thd]
   [common-tests.helpers.files :as thf]
   [common-tests.helpers.ids-map :as thi]))

(t/use-fixtures :each thi/test-fixture)


;; TODO Test case - Chained components changes propagation

(t/deftest test-propagation-stuff
  (let [;; Setup
        file
        (-> (thf/sample-file :file1)
            #_(thc/add-simple-component-with-copy :component-1 :component-1-main-root :component-1-main-child :component-1-copy-root)

            (thc/add-frame :frame-comp-1)
            (thf/add-sample-shape :rectangle :parent-label :frame-comp-1)
            (thf/make-component :comp-1 :frame-comp-1)

            (thc/add-frame :frame-comp-2)
            (thf/instantiate-component :comp-1 :copy-comp-1 :parent-label :frame-comp-2 :children-labels [:rect-comp-2])
            (thf/make-component :comp-2 :frame-comp-2)

            (thc/add-frame :frame-comp-3)
            (thf/instantiate-component :comp-2 :copy-comp-2  :parent-label :frame-comp-3 :children-labels [:comp-1-comp-2])
            (thf/make-component :comp-3 :frame-comp-3))

        page (thf/current-page file)
        shape (thf/get-shape file :rectangle)
        component-1-main-child (thf/get-shape file :rectangle)

        ;; Action
        ;; Changes to update the color of the main component
        changes-update-color (cls/generate-update-shapes (pcb/empty-changes nil (:id page))
                               [(:id component-1-main-child)]
                               (fn [shape]
                                 (assoc shape :fills [{:fill-color "#FABADA" :fill-opacity 1}]))
                               (:objects page)
                               {})

        file'      (thf/apply-changes file changes-update-color)
        page'      (thf/current-page file')
        shape'     (thf/get-shape file' :component-1-main-child)
        component  (thf/get-component  file' :component-1)
        file'_id   (:id file')
        libraries' {file'_id file'}

        ;; Changes to propagate the color change to copies
        changes-sync (-> (pcb/empty-changes)
                         (cll/generate-sync-file-changes
                           nil
                           :components
                           file'_id
                           (:id component)
                           file'_id
                           libraries'
                           file'_id))

        components_changed (ch/components-changed file' {:id (:id component-1-main-child)
                                                         :page-id (:id page)
                                                         :component-id (:id component)
                                                         :operations [{:type :set
                                                                       :attr
                                                                       :fills
                                                                       :val [{:fill-color "#FABADA" :fill-opacity 1}]
                                                                       :ignore-geometry false
                                                                       :ignore-touched false}]})
        _ (println "components_changed" components_changed)

        file'' (thf/apply-changes file' changes-sync)

       ;; Get
        page'' (thf/current-page file'')]

    ;; (println "---->"(-> shape
    ;;                     :name))
    ;; (println "---->"(-> shape'
    ;;                     :name))
    ;; generate-sync-shape-direct

    (thd/dump-page page [:touched :fills])
    (println "------111")
    (thd/dump-page page' [:touched :fills])
    (println "------222")
    (thd/dump-page page'' [:touched :fills])

    ;; Check
    (t/is false)))