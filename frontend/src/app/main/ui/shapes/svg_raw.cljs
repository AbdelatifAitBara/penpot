;; This Source Code Form is subject to the terms of the Mozilla Public
;; License, v. 2.0. If a copy of the MPL was not distributed with this
;; file, You can obtain one at http://mozilla.org/MPL/2.0/.
;;
;; Copyright (c) KALEIDOS INC

(ns app.main.ui.shapes.svg-raw
  (:require
   [app.common.data.macros :as dm]
   [app.common.geom.shapes :as gsh]
   [app.common.svg :as csvg]
   [app.main.ui.context :as muc]
   [app.main.ui.shapes.attrs :as usa]
   [app.util.object :as obj]
   [rumext.v2 :as mf]))

;; Graphic tags
(def graphic-element
  #{:svg :circle :ellipse :image :line :path :polygon :polyline :rect :symbol :text :textPath :use})

;; Context to store a re-mapping of the ids
(def svg-ids-ctx (mf/create-context nil))

(defn set-styles [attrs shape render-id]
  (let [props (-> (usa/get-style-props shape render-id)
                  (obj/unset! "transform"))

        attrs (if (map? attrs)
                (-> attrs csvg/attrs->props obj/map->obj)
                #js {})

        style (obj/merge (obj/get attrs "style")
                         (obj/get props "style"))]

    (-> attrs
        (obj/merge! props)
        (obj/set! "style" style))))

(defn translate-shape [attrs shape]
  (let [transform (dm/str (csvg/svg-transform-matrix shape)
                          " "
                          (:transform attrs ""))]
    (cond-> attrs
      (and (:svg-viewbox shape) (contains? graphic-element (-> shape :content :tag)))
      (assoc :transform transform))))

(mf/defc svg-root
  {::mf/wrap-props false}
  [props]

  (let [shape    (unchecked-get props "shape")
        children (unchecked-get props "children")
        {:keys [x y width height]} shape
        {:keys [attrs] :as content} (:content shape)

        ids-mapping (mf/use-memo #(csvg/generate-id-mapping content))
        render-id   (mf/use-ctx muc/render-id)

        attrs (-> (set-styles attrs shape render-id)
                  (obj/set! "x" x)
                  (obj/set! "y" y)
                  (obj/set! "width" width)
                  (obj/set! "height" height)
                  (obj/set! "preserveAspectRatio" "none"))]

    [:& (mf/provider svg-ids-ctx) {:value ids-mapping}
     [:g.svg-raw {:transform (gsh/transform-str shape)}
      [:> "svg" attrs children]]]))

(mf/defc svg-element
  {::mf/wrap-props false}
  [props]
  (let [shape    (unchecked-get props "shape")
        children (unchecked-get props "children")

        {:keys [content]} shape
        {:keys [attrs tag]} content

        ids-mapping (mf/use-ctx svg-ids-ctx)
        render-id   (mf/use-ctx muc/render-id)

        attrs (mf/use-memo #(csvg/replace-attrs-ids attrs ids-mapping))

        attrs (translate-shape attrs shape)
        element-id (get-in content [:attrs :id])
        attrs (cond-> (set-styles attrs shape render-id)
                (and element-id (contains? ids-mapping element-id))
                (obj/set! "id" (get ids-mapping element-id)))]
    [:> (name tag) attrs children]))

(defn svg-raw-shape [shape-wrapper]
  (mf/fnc svg-raw-shape
    {::mf/wrap-props false}
    [props]

    (let [shape  (unchecked-get props "shape")
          childs (unchecked-get props "childs")

          {:keys [content]} shape
          {:keys [tag]} content

          svg-root?  (and (map? content) (= tag :svg))
          svg-tag?   (map? content)
          svg-leaf?  (string? content)
          valid-tag? (contains? csvg/svg-tags-list tag)]

      (cond
        svg-root?
        [:& svg-root {:shape shape}
         (for [item childs]
           [:& shape-wrapper {:shape item :key (dm/str (:id item))}])]

        (and svg-tag? valid-tag?)
        [:& svg-element {:shape shape}
         (for [item childs]
           [:& shape-wrapper {:shape item :key (dm/str (:id item))}])]

        svg-leaf?
        content

        :else nil))))


