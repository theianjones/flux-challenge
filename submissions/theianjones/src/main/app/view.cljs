(ns app.view
  (:require [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.data-fetch :as df]
            [app.proxy :refer [sith-output]]))

(comment
  we are adding the apprentice/master ident to the [:siths :list/siths] ui edge. Then we fetch the data for that sith. When the data comes back, we display the sith slot.)
;; (defmutation load-sith [{:keys [query-class] :sith/keys [id] :as params}]
;;   (action [{:keys [app state]}]
;;           (swap! state assoc-in [:siths :list/siths] (add-to-list @state id))
;;           (df/load! app [:sith/id id] query-class)))

(comment
  we could just put a df/load into the mutations... you could load from the Sith component. When the mutation is in a cljc file, then you cant import your mutations as well as importing your cljs component into the cljc file... clojure doesnt like you going back and forth like that.
  (df/load! app [:person/id id] PersonDetail))

(defmutation set-button [{:keys [button-key button-value]}]
  (action [{:keys [app state]}]
          (swap! state assoc button-key button-value)))

(defsc Sith [this {:sith/keys [id name homeWorld] :as props}]
  {:query [:sith/id :sith/name :sith/master :sith/apprentice  {:sith/homeWorld [:homeWorld/name :homeWorld/id]}]
   :ident :sith/id}
  (dom/div
   (dom/h3 name)
   (dom/h6 "Homeworld: "
           (dom/span (:homeWorld/name homeWorld)))))

(def ui-sith (comp/factory Sith {:keyfn :sith/id}))

(defn load-sith [query-component component where id]
  (let [load-target [:slot/by-id where :slot/sith]
        sith-ident [:sith/id id]]
    (df/load! component sith-ident query-component {:target load-target})))

(defsc SithSlot [this {:db/keys [id] :slot/keys [sith]}]
  {:query [:db/id {:slot/sith (comp/get-query Sith)}]
   :initial-state (fn [{:keys [id]}] {:db/id id :slot/sith nil})
   :ident [:slot/by-id :db/id]
   :componentDidUpdate (fn [this prev-props _]
                         (let [p (comp/props this)
                               _ (prn p)
                               app-id (-> p :slot/sith :sith/apprentice)
                               app-target (:app-target (comp/get-computed this))
                               mas-id (-> p :slot/sith :sith/master)
                               mas-target (:mas-target (comp/get-computed this))]
                           (when (and app-id app-target)
                             (load-sith Sith this app-target app-id))
                           (when (and mas-id mas-target)
                             (load-sith Sith this mas-target mas-id))))}
  (dom/li :.css-slot
          (when (some? sith)
            (ui-sith sith))))

(def ui-slot (comp/factory SithSlot {:keyfn :db/id}))

(defn slot->app-target [slot-id]
  (slot-id {:one :two
            :two :three
            :three :four
            :four :five}))

(defn slot->mas-target [slot-id]
  (slot-id {:two :one
            :three :two
            :four :three
            :five :four}))

(defn target-occupied? [slots-data slot-id]
  (reduce (fn [target-occupied? [id val]] (if (= (:db/id val) slot-id) (some? (:slot/sith val)) target-occupied?)) false slots-data))

(defn get-slot-props [slots-data slot-id]
  (let [app-target (slot->app-target slot-id)
        app-target-occupied? (not (target-occupied? slots-data app-target))
        mas-target (slot->mas-target slot-id)
        mas-target-occupied? (not (target-occupied? slots-data mas-target))]
    {:app-target (and app-target-occupied? app-target)
     :mas-target (and mas-target-occupied? mas-target)}))

  (defsc SithList [this {:slot/keys [one two three four five] :as props}]
    {:query [{:slot/one (comp/get-query SithSlot)}
             {:slot/two (comp/get-query SithSlot)}
             {:slot/three (comp/get-query SithSlot)}
             {:slot/four (comp/get-query SithSlot)}
             {:slot/five (comp/get-query SithSlot)}]
     :initial-state (fn [_] {:slot/one (comp/get-initial-state SithSlot {:id :one})
                             :slot/two (comp/get-initial-state SithSlot {:id :two})
                             :slot/three (comp/get-initial-state SithSlot {:id :three})
                             :slot/four (comp/get-initial-state SithSlot {:id :four})
                             :slot/five (comp/get-initial-state SithSlot {:id :five})})
     :ident (fn [] [:LIST :only-one])}
    (dom/ul :.css-slots
            (prn (get-slot-props props :one))
            (ui-slot (comp/computed one (get-slot-props props :one)))
            (ui-slot (comp/computed two (get-slot-props props :two)))
            (ui-slot (comp/computed three (get-slot-props props :three)))
            (ui-slot (comp/computed four (get-slot-props props :four)))
            (ui-slot (comp/computed five (get-slot-props props :five)))))

(def ui-sith-list (comp/factory SithList {:keyfn :db/id}))

(defn merge-ident [slot new-ident]
  (merge slot {:slot/sith new-ident}))

(defmutation navigate-up [_]
  (action [{:keys [app state]}]
          (let [old-slots (:slot/by-id @state)
                new-slot-one (merge-ident (:one old-slots) nil)
                new-slot-two (merge-ident (:two old-slots) nil)
                new-slot-three (merge-ident (:three old-slots) (-> old-slots :one :slot/sith))
                new-slot-four (merge-ident (:four old-slots) (-> old-slots :two :slot/sith))
                new-slot-five (merge-ident (:five old-slots) (-> old-slots :three :slot/sith))
                new-slots {:one new-slot-one :two new-slot-two :three new-slot-three :four new-slot-four :five new-slot-five}]
            (swap! state assoc :slot/by-id new-slots))))

(defmutation navigate-down [_]
  (action [{:keys [app state]}]
          (let [old-slots (:slot/by-id @state)
                new-slot-one (merge-ident (:one old-slots) (-> old-slots :three :slot/sith))
                new-slot-two (merge-ident (:two old-slots) (-> old-slots :four :slot/sith))
                new-slot-three (merge-ident (:three old-slots) (-> old-slots :five :slot/sith))
                new-slot-four (merge-ident (:four old-slots) nil)
                new-slot-five (merge-ident (:five old-slots) nil)
                new-slots {:one new-slot-one :two new-slot-two :three new-slot-three :four new-slot-four :five new-slot-five}]
            (swap! state assoc :slot/by-id new-slots))))

(defsc Root [this {:keys [root/list up-enabled down-enabled] :as props}]
  {:query [{:root/list (comp/get-query SithList)} :up-enabled :down-enabled]
   :initial-state (fn [params] {:root/list (comp/get-initial-state SithList {}) :up-enabled true :down-enabled true})}
  (dom/div :.css-root
           (dom/h1 :.css-planet-monitor "Obi-Wan currently on: "
                   (dom/span "Earth"))
           (dom/section :.css-scrollable-list
                        (ui-sith-list list)
                        (dom/div :.css-scroll-buttons
                                 (dom/button :.css-button-up {:className (if up-enabled "" "css-button-disabled")
                                                              :disabled (not update-in)
                                                              :onClick #(comp/transact! this [(navigate-up)])})
                                 (dom/button :.css-button-down {:className (if down-enabled "" "css-button-disabled")
                                                                :disabled (not down-enabled)
                                                                :onClick #(comp/transact! this [(navigate-down)])})))))

(comment
  (comp/get-initial-state Sith {:sith/id 3616 :sith/name "Darth"})
  df/marker-table can be used to keep track of loading states
  merge/remove-ident*)
