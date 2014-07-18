(ns blog.author
  (:require [clavatar.core :as g]))

(def ^:private authors { "txustice" { :name "Josep M. Bach (Txus)"
                                      :twitter "txustice"
                                      :github "txus"
                                      :email "josep.m.bach@gmail.com" }})

(defn full-name [id]
  (println id)
  (get-in authors [id :name]))

(defn gravatar-url [id]
  (let [email (get-in authors [id :email])]
    (g/gravatar email)))

(defn twitter-url [id]
  (let [handle (get-in authors [id :twitter])]
    (str "https://twitter.com/" handle)))

(defn github-url [id]
  (let [handle (get-in authors [id :github])]
    (str "https://github.com" handle)))
