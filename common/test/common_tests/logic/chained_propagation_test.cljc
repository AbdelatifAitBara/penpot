;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC

(ns common-tests.logic.chained-propagation-test
  (:require
   [app.common.files.changes-builder :as pcb]
   [app.common.files.libraries-helpers :as cflh]
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
            #_(thf/add-sample-shape :component-1-main-child
                                    :type :rect
                                    :name "Rect1")


            ;; (thc/add-simple-component-with-copy :component-1 :component-1-main-root :component-1-main-child :component-1-copy-root)
            ;; (thf/make-component :component-2 :component-1-copy-root)
            ;; (thf/instantiate-component :component-2 :component-2-copy-root :children-labels [:component-1-copy-in-component-2])
            ;; (thf/make-component :component-3 :component-2-copy-root)

            ;; (thf/instantiate-component :component-3 :component-3-copy-root :children-labels [:component-3-copy-in-component-4])
            ;; (thf/make-component :component-4 :component-3-copy-root)

            (thc/add-simple-component :component-1 :comp-1 :rect)

            (thc/add-frame :comp-2)
            (thf/instantiate-component :component-1 :comp-1-comp-2 :parent-label :comp-2 :children-labels [:rect-1])
            (thf/make-component :component-2 :comp-2)

            ;; (thf/instantiate-component :component-container-main :component-container-instance :children-labels [:test-2])

            ;; (thc/add-frame :aa)
            ;; (thf/make-component :kk :component-container-instance)
            ;; (thf/instantiate-component :kk :qq :parent-label :aa :children-labels [:test-3])
            )

        page (thf/current-page file)
        _ (println "KK")
        shape (thf/get-shape file :rect)

        changes-update (-> (pcb/empty-changes nil (:id page))
                           (pcb/with-container page)
                           (pcb/with-objects (:objects page))
                           (pcb/update-shapes
                            [(:id shape)]
                            (fn [shape objects]
                              (assoc shape :fills [{:fill-color "#FABADA" :fill-opacity 1}]))
                            {:with-objects? true}))
        file' (thf/apply-changes file changes-update)
        page' (thf/current-page file')
        shape' (thf/get-shape file' :component-1-main-child)

        changes-sync (cflh/generate-sync-shape-direct (pcb/empty-changes)
                                                      file'
                                                      {(:id  file') file'}
                                                      page'
                                                      (:id shape)
                                                      true
                                                      true)
        _ (pp/pprint (:redo-changes changes-sync))


        file'' (thf/apply-changes file' changes-sync)
        page'' (thf/current-page file'')]

    ;; (println "---->"(-> shape
    ;;                     :name))
    ;; (println "---->"(-> shape'
    ;;                     :name))
    ;; generate-sync-shape-direct

    (thd/dump-page page [:touched :fills])
    (println "------")
    (thd/dump-page page' [:touched :fills])
    (println "------")
    (thd/dump-page page'' [:touched :fills])

    ;; Check
    (t/is false)))