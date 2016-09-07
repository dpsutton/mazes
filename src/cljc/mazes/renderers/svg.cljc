(ns mazes.renderers.svg
  "Renderer which generates SVG markup for maze grids. The render function
  produces Hiccup data; use the render-svg function to get an SVG string."
  (:require [mazes.grid :as grid]
            [clojure.spec :as spec]))

(def default-svg-attributes
  {:version "1.1"
   :base-profile "full"
   :xmlns "http://www.w3.org/2000/svg"
   :width 1000
   :height 800
   :viewbox {:x 0
             :y 0
             :width 1000
             :height 800}})

(def default-render-environment-options
  {:total-width (:width default-svg-attributes)
   :total-height (:height default-svg-attributes)
   :margin 20
   :size-spacing-ratio 0.75})

(def default-stroke-attributes
  {:stroke-width 2
   :stroke-linejoin "miter"
   :stroke-linecap "square"})

(defn rect [attributes]
  [:rect (merge attributes default-stroke-attributes)])

(defn line [attributes]
  [:line (merge attributes default-stroke-attributes)])

(defn svg
  ([]
   (svg default-svg-attributes))
  ([{:keys [:width :height :viewbox] :as options}]
   (let [viewbox (or viewbox
                     (merge (:viewbox default-svg-attributes)
                            {:width width :height height}))
         viewbox-values (map viewbox [:x :y :width :height])
         viewbox-string (clojure.string/join " " viewbox-values)
         ; note that we are outputting NON-namespaced keys for Hiccup output
         svg-attributes (merge default-svg-attributes
                               {:width width
                                :height height
                                :viewbox viewbox-string})]
     [:svg svg-attributes])))

(defn render-environment
  "Given a grid and an options map, returns a map of values to be used in
  rendering computations. The options map defaults to the values in
  default-render-environment-options.

  Options accepted:
    :total-width - Optional. The total width of the render area. Equal to the
      width property of the SVG tag.
    :total-height - Optional. The total height of the render area. Equal to the
      height property of the SVG tag.
    :size-spacing-ratio - Optional. A value between 0 and 1, defining the ratio
      between the size of cell and the space between them.
    :margin - Optional. The blank space around the edges of the rendered area.
      A higher margin will leave less space for cells.

  Values returned:
    :total-width - Passed through from input options.
    :total-height - Passed through from input options.
    :margin - Passed through from input options.
    :cell-width - The width of each cell to be rendered.
    :cell-height - The height of each cell to be rendered.
    :cell-h-spacing - The horizontal space between each cell.
    :cell-v-spacing - The vertical space between each cell."
  ([grid]
   (render-environment grid default-render-environment-options))
  ([grid options]
   (let [options (merge default-render-environment-options options)
         {:keys [total-width total-height size-spacing-ratio margin]} options
         columns (grid/column-count grid)
         rows (grid/row-count grid)
         cell-area-width (- total-width (* 2 margin))
         cell-area-height (- total-height (* 2 margin))
         width-per-cell (/ cell-area-width columns)
         height-per-cell (/ cell-area-height rows)
         cell-width (* width-per-cell size-spacing-ratio)
         cell-height (* height-per-cell size-spacing-ratio)]
     {:total-width total-width
      :total-height total-height
      :margin margin
      :cell-width cell-width
      :cell-height cell-height
      ; spacing exists between cells, but not on the far side of the last
      ; column/row; so divide the non-cell space by n - 1, not n.
      :cell-h-spacing (/ (- cell-area-width (* cell-width columns))
                         (- columns 1))
      :cell-v-spacing (/ (- cell-area-height (* cell-height rows))
                         (- rows 1))})))

(defn render-cell
  "Given a render-environment map and a ::grid/cell map, returns an SVG group
  which displays the cell."
  [render-env cell]
  [:g
   [:rect {:x (* (::grid/x cell)
                 (+ (:cell-width render-env)
                    (:cell-h-spacing render-env)))
           :y (* (::grid/y cell)
                 (+ (:cell-height render-env)
                    (:cell-v-spacing render-env)))
           :width (:cell-width render-env)
           :height (:cell-height render-env)}]])

(defn render
  "Given a grid, returns an SVG rendering as Hiccup data structures. Accepts an
  options map with the following keys. All numeric values are in user units.

    :width -   The width of the SVG. Default 400.
    :height -  The height of the SVG. Default 300.
    :viewbox - An {:x, :y, :width, :height} map. Defaults to the width and
               height of the SVG."
  ; TODO: fully implement these functions; right now they just output the SVG base tag.
  ([grid]
   (svg))
  ([grid options]
   (svg options)))
