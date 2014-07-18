(ns blog.core
  (:require [optimus.assets :as assets]
            [optimus.export]
            [optimus.optimizations :as optimizations]
            [optimus.prime :as optimus]
            [optimus.link :as link]
            [optimus.strategies :refer [serve-live-assets]]
            [blog.highlight :refer [highlight-code-blocks]]
            [blog.post :as post]
            [blog.rss :as rss]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [me.raynes.cegdown :as md]
            [stasis.core :as stasis]
            [hiccup.page :refer [html5]]))

(def analytics-id "")

(defn layout-page [request page]
  (html5
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport"
             :content "width=device-width, initial-scale=1.0"}]
     [:title "Codegram Blog"]
     [:link {:rel "alternate" :title "ATOM" :type "application/atom+xml" :href "/feed.atom"}]
     [:link {:rel "stylesheet" :href (link/file-path request "/styles.css")}]]

    [:body
     [:div.container
       [:h1 [:a {:href "/"} "Codegram Blog"]]
       [:div.body page]
       [:section {:id "footer"}
        [:hr]
        [:small "Copyright 2014 Codegram Technologies"]]]
     [:script {:type "text/javascript"}
      (str "var _gaq = _gaq || [];
            _gaq.push(['_setAccount', '" analytics-id "']);
            _gaq.push(['_trackPageview']);

            (function() {
            var ga = document.createElement('script'); ga.type = 'text/javascript'; ga.async = true;
            ga.src = ('https:' == document.location.protocol ? 'https://ssl' : 'http://www') + '.google-analytics.com/ga.js';
            var s = document.getElementsByTagName('script')[0]; s.parentNode.insertBefore(ga, s);
            })();")]]))

(defn markdown-posts [posts]
  (reduce (fn [acc post]
            (conj acc { (post :url) (post/to-markdown layout-page post) })) {} posts))

(defn markdown-pages [pages]
  (zipmap (map #(-> %
                    (str/replace #"\.md$" "")
                    (str/replace #"\.markdown$" ""))
               (keys pages))
          (map #(fn [req] (layout-page req (md/to-html % [:autolinks :fenced-code-blocks :strikethrough])))
               (vals pages))))

(defn markdown-index [index posts]
  (let [render-index (fn [req]
                       (let [rendered-posts (->> posts
                                                 (map (fn [post] ((post/to-markdown (fn [_ page] page) post) req)))
                                                 (interpose "<hr/>")
                                                 (reduce str)
                                                 (str (md/to-html index [:autolinks])))]
                         (layout-page req rendered-posts)))]
    {"/index.html" render-index}))

(defn prepare-page [page req]
  (-> (if (string? page) page (page req))
      highlight-code-blocks))

(defn get-raw-pages []
  (let [post-map (stasis/slurp-directory "resources/posts" #"\.(md|markdown)$")
        posts (->> post-map
                   (map (fn [[k v]] [k v]))
                   (sort-by (fn [[k v]] k))
                   reverse
                   (map (fn [[k v]] (post/from-markdown k v))))]
    (stasis/merge-page-sources
      {:public
       (stasis/slurp-directory "resources/public" #".*\.(html|css|js)$")
       :index
       (markdown-index (slurp "resources/index.md") posts)
       :partials
       (markdown-pages (stasis/slurp-directory "resources/partials" #".*\.(md|markdown)"))
       :posts
       (markdown-posts posts)
       :rss
       { "/feed.atom" (rss/atom-xml posts) }})))

(defn prepare-pages [pages]
  (zipmap (keys pages)
          (map #(partial prepare-page %) (vals pages))))

(defn get-pages []
  (prepare-pages (get-raw-pages)))

(defn get-assets []
  (assets/load-assets "public" [#".*"]))

(def app (stasis/serve-pages get-pages))

(def app
  (optimus/wrap (stasis/serve-pages get-pages)
                get-assets
                optimizations/all
                serve-live-assets))

(def export-dir "output")

(defn export []
  (let [assets (optimizations/all (get-assets) {})]
    (stasis/empty-directory! export-dir)
    (optimus.export/save-assets assets export-dir)
    (stasis/export-pages (get-pages) export-dir {:optimus-assets assets})))
