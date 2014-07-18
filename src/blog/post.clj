(ns blog.post
  (:require [clj-yaml.core :as yaml]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [me.raynes.cegdown :as md]
            [blog.author :as author]
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
        month (->> date t/month str)
        url-title (-> filename
                      (str/replace #"/\d{4}-\d{2}-\d{2}-" "")
                      (str/replace #"\.md$" "")
                      (str/replace #"\.markdown" ""))
        [_ metadata body] (str/split blob #"---", 3)]
    (-> (yaml/parse-string metadata)
        (select-keys [:title :author])
        (assoc :date date)
        (assoc :url (str "/" year "/" month "/" url-title))
        (assoc :body (str/trim body)))))

(def pegdown-options ;; https://github.com/sirthias/pegdown
  [:autolinks :fenced-code-blocks :strikethrough])

(defn format-date [date]
  (let [formatter (-> (f/formatter "MMM dd, yyyy")
                      (f/with-locale (java.util.Locale. "ENGLISH")))]
    (f/unparse formatter date)))

(defn author-link [id]
  (let [full-name (author/full-name id)
        twitter-url (author/twitter-url id)]
    (str "<a href=\"" twitter-url "\">" full-name "</a>")))

(defn to-markdown [layout post]
  (let [human-date (->> post :date format-date)
        rendered (md/to-html (post :body) pegdown-options)
        body (->> rendered
                  (str "<header><a href=\"" (post :url) "\">" (post :title) "</a></header>")
                  (str "<span class=\"meta\">by " (author-link (post :author)) " on <span class=\"date\">" human-date "</span></span>\n\n"))
        wrapped-body (str "<div class=\"post\">" body "</div>")]
    (fn [req] (layout req wrapped-body))))
