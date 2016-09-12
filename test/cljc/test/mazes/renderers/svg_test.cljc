(ns test.mazes.renderers.svg-test
  (:require [clojure.test :refer :all]
            [clojure.spec.test :as stest]
            [mazes.grid :as grid]
            [mazes.renderers.svg :refer :all :as svg]
            [com.rpl.specter :as s]
            [com.rpl.specter.macros :as sm]
            ))

;; NOTE: We're using a lot of == in these tests, because we never know when
;; something is going to return a double instead of an int.

(stest/instrument)

;; Helper: true if the two collections contain the same numbers in the same
;; order, regardless of the types of the collections or numbers. Uses ==.
(defn equal-numbers? [c1 c2]
  (every? #(apply == %) (partition 2 (interleave c1 c2))))

(deftest test-root
  (testing "without options hash"
    (let [output (svg)]
      (is (vector? output))
      (is (not (empty? output)))
      ; this simple Specter select could just be (first (filter map? output)), but
      ; selections will become more complex as the renderer SVG grows, so let's
      ; just use it consistently from the start.
      (let [attributes (sm/select-any [s/ALL map?] output)]
        (is (== (:width default-svg-attributes) (:width attributes)))
        (is (== (:height default-svg-attributes) (:height attributes)))
        (is (string? (:viewbox attributes)))
        (is (equal-numbers?
              [0 0 (:width default-svg-attributes) (:height default-svg-attributes)]
              (map read-string (clojure.string/split (:viewbox attributes) #" ")))))))
  (testing "with options hash"
    (let [output (render (grid/create-grid 2 2)
                         {:width 1500
                          :height 1000
                          :viewbox {:x 20 :y 10 :width 500 :height 400}})]
      (let [attributes (sm/select-any [s/ALL map?] output)]
        (is (== 1500 (:width attributes)))
        (is (== 1000 (:height attributes)))
        (is (string? (:viewbox attributes)))
        (is (equal-numbers?
              [20 10 500 400]
              (map read-string (clojure.string/split (:viewbox attributes) #" "))))))))

(deftest test-rect
  (let [output (rect {:x 1 :y 2 :width 100 :height 200})]
    (is (vector? output))
    (is (not (empty? output)))
    (let [attributes (sm/select-any [s/ALL map?] output)]
      (is (== 1 (:x attributes)))
      (is (== 2 (:y attributes)))
      (is (== 100 (:width attributes)))
      (is (== 200 (:height attributes)))
      (is (= default-stroke-attributes (select-keys attributes (keys default-stroke-attributes)))))))

(deftest test-line
  (let [output (line {:x1 10 :y1 20 :x2 100 :y2 200})]
    (is (vector? output))
    (is (not (empty? output)))
    (let [attributes (sm/select-any [s/ALL map?] output)]
      (is (== 10 (:x1 attributes)))
      (is (== 20 (:y1 attributes)))
      (is (== 100 (:x2 attributes)))
      (is (== 200 (:y2 attributes)))
      (is (= default-stroke-attributes (select-keys attributes (keys default-stroke-attributes)))))))

(deftest test-render-environment
  (testing "without explicit options"
    (let [columns 4
          rows 6
          {:keys [total-width total-height margin]} default-render-environment-options
          env (render-environment (grid/create-grid columns rows))]
      (is (not (nil? env)))
      (is (map? env))
      (is (== total-width
              (+ (* (:cell-width env) columns)
                 (* (:cell-h-spacing env) (- columns 1))
                 (* 2 margin)))
          "Cell widths, separated by h-spacings, must equal total-width minus
          margin on each side.")
      (is (== total-height
              (+ (* (:cell-height env) rows)
                 (* (:cell-v-spacing env) (- rows 1))
                 (* 2 margin)))
          "Cell heights, separated by v-spacings, must equal total-width minus
          margin on each side.")))
  (testing "with explicit options"
    (let [columns 8
          rows 4
          total-width 800
          total-height 600
          margin 5
          size-spacing-ratio 0.6
          env (render-environment (grid/create-grid columns rows)
                                  {:total-width total-width
                                   :total-height total-height
                                   :margin margin
                                   :size-spacing-ratio size-spacing-ratio})]
      (is (not (nil? env)))
      (is (map? env))
      (is (== total-width
              (+ (* (:cell-width env) columns)
                 (* (:cell-h-spacing env) (- columns 1))
                 (* 2 margin)))
          "Cell widths, separated by h-spacings, must equal total-width minus
          margin on each side.")
      (is (== total-height
              (+ (* (:cell-height env) rows)
                 (* (:cell-v-spacing env) (- rows 1))
                 (* 2 margin)))
          "Cell heights, separated by v-spacings, must equal total-width minus
          margin on each side."))))

(deftest test-room-geometry
  (let [rows 8
        columns 4
        grid (grid/create-grid rows columns)
        env (render-environment grid)
        top-left-cell (grid/find-cell grid 0 0)
        top-left (room-geometry env top-left-cell)
        top-right-cell (grid/find-cell grid (- rows 1) 0)
        top-right (room-geometry env top-right-cell)
        bottom-left-cell (grid/find-cell grid 0 (- columns 1))
        bottom-left (room-geometry env bottom-left-cell)
        bottom-right-cell (grid/find-cell grid (- rows 1) (- columns 1))
        bottom-right (room-geometry env bottom-right-cell) ]
    (testing "return type"
      (is (map? top-left)))


    (testing "top left"
      (is (== (:margin env) (:x top-left)))
      (is (== (:margin env) (:y top-left)))
      (is (== (:cell-width env) (:width top-left)))
      (is (== (:cell-height env) (:height top-left))))

    (testing "top right"
      (is (== (+ (:margin env)
                 (* (::grid/x top-right-cell) (:cell-width env))
                 (* (::grid/x top-right-cell) (:cell-h-spacing env)))
              (:x top-right)))
      (is (== (:margin env) (:y top-right)))
      (is (== (:cell-width env) (:width top-right)))
      (is (== (:cell-height env) (:height top-right))))

    (testing "bottom left"
      (is (== (:margin env) (:x bottom-left)))
      (is (== (+ (:margin env)
                 (* (::grid/y bottom-left-cell) (:cell-height env))
                 (* (::grid/y bottom-left-cell) (:cell-v-spacing env)))
              (:y bottom-left)))
      (is (== (:cell-width env) (:width bottom-left)))
      (is (== (:cell-height env) (:height bottom-left))))

    (testing "bottom right"
      (is (== (+ (:margin env)
                 (* (::grid/x bottom-right-cell) (:cell-width env))
                 (* (::grid/x bottom-right-cell) (:cell-h-spacing env)))
              (:x bottom-right)))
      (is (== (+ (:margin env)
                 (* (::grid/y bottom-right-cell) (:cell-height env))
                 (* (::grid/y bottom-right-cell) (:cell-v-spacing env)))
              (:y bottom-right)))
      (is (== (:cell-width env) (:width bottom-right)))
      (is (== (:cell-height env) (:height bottom-right))))))

(deftest test-render-rect
  (let [grid (grid/create-grid 1 1)
        env (render-environment grid)
        rect (render-rect env (grid/find-cell grid 0 0))]
    (is (vector? rect))
    (is (= :rect (first rect)))
    (is (map? (last rect)))
    (is (= (room-geometry env (grid/find-cell grid 0 0)) (last rect)))))

(deftest test-anchor-point
  (let [g {:x 10 :y 20 :width 100 :height 200}]
    (is (equal-numbers? [60 20] (anchor-point g ::grid/n)))
    (is (equal-numbers? [110 20] (anchor-point g ::grid/ne)))
    (is (equal-numbers? [110 120] (anchor-point g ::grid/e)))
    (is (equal-numbers? [110 220] (anchor-point g ::grid/se)))
    (is (equal-numbers? [60 220] (anchor-point g ::grid/s)))
    (is (equal-numbers? [10 220] (anchor-point g ::grid/sw)))
    (is (equal-numbers? [10 120] (anchor-point g ::grid/w)))
    (is (equal-numbers? [10 20] (anchor-point g ::grid/nw)))))

(deftest test-render-line
  (let [grid (grid/create-grid 2 2)
        grid (grid/link grid (grid/find-cell grid 0 0) ::grid/e)
        grid (grid/link grid (grid/find-cell grid 0 0) ::grid/s)
        env (render-environment grid)
        start-cell (grid/find-cell grid 0 0)
        start-room (room-geometry env start-cell)
        end-cell (grid/move grid start-cell ::grid/e)
        end-room (room-geometry env end-cell)
        line (render-line env start-cell ::grid/e)]
    (is (vector? line))
    (is (= :line (first line)))
    (is (map? (last line)))
    (let [{:keys [x1 y1 x2 y2]} (last line)]
      (is (= (anchor-point start-room ::grid/e) [x1 y1]))
      (is (= (anchor-point end-room ::grid/w) [x2 y2])))))

(deftest test-render-cell
  (let [grid (grid/create-grid 2 2)
        grid (grid/link grid (grid/find-cell grid 0 0) ::grid/e)
        grid (grid/link grid (grid/find-cell grid 0 0) ::grid/s)
        env (render-environment grid)
        start-cell (grid/find-cell grid 0 0)
        start-room (room-geometry env start-cell)
        end-cell (grid/move grid start-cell ::grid/e)
        end-room (room-geometry env end-cell)
        ; Specter selector: [s/ALL (is-svg-tag? :rect)]
        is-svg-tag? (fn [tag]
                      (fn [coll]
                        (and (vector? coll) (= tag (first coll)))))
        find-rect (fn [g] (sm/select-any [s/ALL (is-svg-tag? :rect)] g))
        find-lines (fn [g] (sm/select [s/ALL (is-svg-tag? :line)] g))]
    (testing "without existing lines"
      (let [g (render-cell env start-cell)]
        (is (vector? g))
        (is (> (count g) 0))
        (is (= :g (first g)))
        (let [rect (find-rect g)
              lines (find-lines g)]
          (is (= (room-geometry env start-cell) (last rect)))
          ; the two grid-links above should give this room two connections
          (is (= 2 (count lines))))))
    (testing "with one existing line"
      ; render as above, yielding 2 lines, but keep only 1
      (let [lines (pop (find-lines (render-cell env start-cell)))
            g (render-cell env start-cell (set lines))]
        (is (= 1 (count (find-lines g))))))))
