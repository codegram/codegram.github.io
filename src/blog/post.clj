(ns blog.post
  (:require [clj-yaml.core :as yaml]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [me.raynes.cegdown :as md]
            [clojure.string :as str]))

(defn- parse-date [filename]
  (->> filename
       (re-seq #"\d{2,4}")
       (take 3)
       (map #(Integer/parseInt %))
       (apply t/date-time)))

(defn from-markdown [filename blob]
  (let [date (parse-date filename)
        year (->> date t/year (format "%04d"))
        month (->> date t/month (format "%02d"))
        url-title (-> filename
                      (str/replace #"/\d{4}-\d{2}-\d{2}-" "")
                      (str/replace #"\.md$" "")
                      (str/replace #"\.markdown" ""))
        [_ metadata body] (str/split blob #"---", 3)]
    (-> (yaml/parse-string metadata)
        (select-keys [:title])
        (assoc :date date)
        (assoc :url (str "/" year "/" month "/" url-title))
        (assoc :body (str/trim body)))))

(def pegdown-options ;; https://github.com/sirthias/pegdown
  [:autolinks :fenced-code-blocks :strikethrough])

(defn format-date [date]
  (let [formatter (-> (f/formatter "MMM dd, yyyy")
                      (f/with-locale (java.util.Locale. "ENGLISH")))]
    (f/unparse formatter date)))

(defn to-markdown [layout post]
  (let [human-date (->> post :date format-date)
        rendered (md/to-html (post :body) pegdown-options)
        body (->> rendered
                  (str "<header><a href=\"" (post :url) "\">" (post :title) "</a></header>")
                  (str "<span class=\"date\">" human-date "</span>\n\n"))
        wrapped-body (str "<div class=\"post\">" body "</div>")]
    (fn [req] (layout req wrapped-body))))

; (def blob (slurp "resources/posts/2010-10-27-stay-fit-with-code-katas.md"))
; (def filename "2010-10-27-stay-fit-with-code-katas.md")
;
; (def p (from-markdown filename blob))
;
; (def x (to-markdown p core/layout-page))
; (def y (x {}))
; y
