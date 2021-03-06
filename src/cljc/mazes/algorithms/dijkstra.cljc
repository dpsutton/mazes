(ns mazes.algorithms.dijkstra
  "An implementation of Dijkstra's algorithm for maze solving."
  (:require [mazes.grid :as g]
            [clojure.set :refer [union difference]]
            [clojure.spec :as spec]))

(spec/def ::current ::g/cell)
(spec/def ::destination ::g/cell)
(spec/def ::distance int?)
(spec/def ::unvisited (spec/coll-of ::g/cell :kind set?))
(spec/def ::distances (spec/map-of ::g/cell ::distance :conform-keys true))
(spec/def ::step-values (spec/keys :req [::g/grid ::unvisited ::distances ::current]))

(def infinite-distance #?(:clj Integer/MAX_VALUE, :cljs js/Infinity))

(defn get-initial-values
  "Given a grid and an origin, sets up initial values for a single step in the
  algorithm."
  [grid origin]
  (let [all-cells (g/all-cells grid)
        ; origin cell is 0 steps from origin, of course; all others start at infinity
        initial-distance (fn [c] (if (= c origin) 0 infinite-distance))]
    {::g/grid grid
     ::unvisited (set all-cells)
     ::distances (zipmap all-cells (map initial-distance all-cells))
     ::current origin}))
(spec/fdef get-initial-values
  :args (spec/cat :grid ::g/grid :origin ::g/cell)
  :ret ::step-values)

(defn step
  "Given a working set of data for Dijkstra's algorithm, performs a single step
  in the iterative solution. If you only need the solution, use the solve
  function."
  ;; To update distances, we make a collection of updates: if (assoc m k v)
  ;; updates a single key-value, (apply assoc m [k v, ...]) applies all the
  ;; key-value updates in the vector. The distances of the neighbors, in our
  ;; uniform grid, are always 1 greater than the distance to the current
  ;; square. But we only want to perform the update if the new distance is less
  ;; than the old; hence the (min).
  ;;
  ;; To choose a new ::current, we get the key of the map entry which had the
  ;; lowest value in the distances map.
  [{:keys [::g/grid ::unvisited ::distances ::current] :as step-values}]
  (let [distance-updates ; a seq of [k v] pairs
        (map (fn [cell]
               (let [old-distance (distances cell)
                     new-distance (inc (distances current))]
                 [cell (min old-distance new-distance)]))
             (g/linked-cells grid current))
        distances (apply assoc distances (flatten distance-updates))
        unvisited (disj unvisited current)]
    {::g/grid grid
     ::unvisited unvisited
     ::distances distances
     ::current (if (empty? unvisited)
                 nil
                 (key (apply min-key val (select-keys distances unvisited))))}))
(spec/fdef step
  :args (spec/cat :values ::step-values)
  :ret ::step-values)

(defn solve
  "Given a grid and an origin cell, uses Dijkstra's algorithm to generate a map
  of the distances of each cell from the origin cell, in the form {<cell>
  <int>, ...}. This map is guaranteed to have a distance for every reachable
  cell.

  Given a grid, an origin cell, and a destination cell, returns a distance map
  which is guaranteed to contain the shortest distance for the destination
  cell, but may not have distances for any other cells.

  In both cases, unreachable cells will not be represented in the map."
  ([grid origin]
   (solve grid origin nil))
  ([grid origin destination]
   ;; NOTE: If no cells have less than infinite distance, they must not have
   ;; been linked to any visited cell.
   (let [complete? (if (nil? destination)
                     ; true when all reachable cells have been visited
                     (fn [distances unvisited]
                       (or (empty? unvisited)
                           (every? (partial = infinite-distance)
                                   (vals (select-keys distances unvisited)))))
                     ; true when the destination cell has been visited
                     (fn [distances unvisited]
                       (not (contains? unvisited destination))))]
     (loop [{:keys [::distances ::unvisited] :as values} (get-initial-values grid origin)]
       ; (clojure.pprint/pprint values)
       (if (complete? distances unvisited)
         distances
         (recur (step values)))))))

(defn path
  "Given a grid and origin and destination cells, uses Dijkstra's algorithm to
  produce a collection of ::g/direction items showing the most efficient route
  from the start cell to the end cell."
  [grid origin destination]
  (let [distances (solve grid origin)]
    (loop [cell destination, path []]
      (if (= cell origin)
        (reverse (map g/converse-directions path))
        ; among neighbors, find direction which has the lowest distance in distances
        (let [exits (::g/exits cell)
              ; a map of cells to exits
              neighbors (zipmap (map (partial g/move grid cell) exits) exits)
              next-cell (key (apply min-key val (select-keys distances (keys neighbors))))
              direction (neighbors next-cell)]
          (recur next-cell (conj path direction)))))))
