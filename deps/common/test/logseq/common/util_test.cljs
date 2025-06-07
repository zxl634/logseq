(ns logseq.common.util-test
  (:require [clojure.test :refer [deftest are testing is]]
            [logseq.common.util :as common-util]))

(deftest valid-edn-keyword?
  (are [x y]
       (= (common-util/valid-edn-keyword? x) y)

    ":foo-bar"  true
    ":foo!"     true
    ":foo,bar"  false
    "4"         false
    "foo bar"   false
    "`property" false))

(deftest extract-file-extension?
  (are [x y]
       (= (common-util/path->file-ext x) y)
    "foo.bar" "bar"
    "foo"     nil
    "foo.bar.baz" "baz"
    "../assets/audio.mp3" "mp3"
       ;; From https://www.w3.org/TR/media-frags/
    "../assets/audio.mp3?t=10,20" "mp3"
    "../assets/audio.mp3?t=10,20#t=10" "mp3"
    "/root/Documents/audio.mp3" "mp3"
    "C:\\Users\\foo\\Documents\\audio.mp3" "mp3"
    "/root/Documents/audio" nil
    "/root/Documents/audio." nil
    "special/characters/aäääöüß.7z" "7z"
    "asldk lakls .lsad" "lsad"
    "中文asldk lakls .lsad" "lsad"))

(deftest url?
  (are [x y]
       (= (common-util/url? x) y)
    "http://logseq.com" true
    "prop:: value" false
    "a:" false))

(deftest escape-regex-chars
  (testing "ensure the result is a valid regex string"
    (are [x]
        (some? (re-find (re-pattern  (common-util/escape-regex-chars x)) x))
      "[[page-name]]"
      "end-with-backslash\\"
      "\\[]{}().+*?|$^")))

(deftest safe-resize-observer
  (testing "safe-resize-observer creates ResizeObserver when available"
    (when (exists? js/ResizeObserver)
      (let [callback-called (atom false)
            callback (fn [entries] (reset! callback-called true))
            observer (common-util/safe-resize-observer callback)]
        (testing "creates a ResizeObserver instance"
          (is (instance? js/ResizeObserver observer)))
        (testing "has observe method"
          (is (fn? (.-observe observer))))
        (testing "has disconnect method"
          (is (fn? (.-disconnect observer)))))))
  
  (testing "safe-resize-observer handles missing ResizeObserver gracefully"
    (with-redefs [exists? (constantly false)]
      (let [callback (fn [entries] nil)
            observer (common-util/safe-resize-observer callback)]
        (testing "returns nil when ResizeObserver doesn't exist"
          (is (nil? observer))))))
  
  (testing "safe-resize-observer error handling"
    (when (exists? js/ResizeObserver)
      (let [error-thrown (atom nil)
            original-raf js/requestAnimationFrame
            callback (fn [entries] 
                      (throw (js/Error. "ResizeObserver loop limit exceeded")))
            observer (common-util/safe-resize-observer callback)]
        (testing "suppresses ResizeObserver errors"
          ;; Mock requestAnimationFrame to execute immediately for testing
          (with-redefs [js/requestAnimationFrame (fn [f] (f))]
            (try
              ;; Simulate calling the internal callback directly
              (let [internal-callback (.-callback observer)]
                (internal-callback #js []))
              (catch js/Error e
                (reset! error-thrown e)))
            (testing "ResizeObserver errors are suppressed"
              (is (nil? @error-thrown)))))))))
