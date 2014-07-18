(ns blog.rss
  (:require [me.raynes.cegdown :as md]
            [blog.post :as p]
            [blog.author :as author]
            [clojure.data.xml :as xml]))

(defn- entry [post]
  [:entry
   [:title (:title post)]
   [:updated (:date post)]
   [:author [:name (author/full-name (:author post))]]
   [:link {:href (str "http://blog.codegram.com" (:url post))}]
   [:id (str "tag:blog.codegram.com,2005:/feed/post:" (:title post))]
   [:content {:type "html"} (md/to-html (:body post) p/pegdown-options)]])

(defn atom-xml [posts]
  (xml/emit-str
   (xml/sexp-as-element
    [:feed {:xmlns "http://www.w3.org/2005/Atom"}
     [:id "tag:blog.codegram.com,2005:/feed"]
     [:updated (-> posts first :date)]
     [:title {:type "text"} "Codegram Blog"]
     [:link {:rel "self" :href "http://blog.codegram.com/feed.atom"}]
     (map entry posts)])))
