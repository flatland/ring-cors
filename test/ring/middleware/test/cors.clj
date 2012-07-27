(ns ring.middleware.test.cors
  (:use ring.middleware.cors
        clojure.test))

(comment
  (deftest test-allow-request?
    (testing "with empty vector"
      (is (not (allow-request? {:headers {"origin" "http://eample.com"}} {:access-control-allow-origin []}))))
    (testing "with one regular expressions"
      (are [origin expected]
           (is (= expected (allow-request? {:headers {"origin" origin}} {:access-control-allow-origin #"http://(.*\.)?burningswell.com"})))
           nil false
           "" false
           "http://example.com" false
           "http://burningswell.com" true))
    (testing "with multiple regular expressions"
      (are [origin expected]
           (is (= expected (allow-request? {:headers {"origin" origin}} {:access-control-allow-origin [#"http://(.*\.)?burningswell.com" #"http://example.com"]})))
           nil false
           "" false
           "http://example.com" true
           "http://burningswell.com" true
           "http://api.burningswell.com" true
           "http://dev.burningswell.com" true)))

  (deftest test-header-name
    (are [header expected]
         (is (= expected (header-name header)))
         nil nil
         :access-control-allow-origin "Access-Control-Allow-Origin"
         "Access-Control-Allow-Origin" "Access-Control-Allow-Origin"))

  (deftest test-normalize-headers
    (are [headers expected]
         (is (= expected (normalize-headers headers)))
         nil {}
         {:access-control-allow-origin "*"} {"Access-Control-Allow-Origin" "*"}
         {"Access-Control-Allow-Origin" "*"} {"Access-Control-Allow-Origin" "*"}))

  (deftest test-add-access-control
    (let [origin "http://example.com"]
      (is (= {} (add-access-control {} {} {})))
      (is (= {:headers {"Access-Control-Allow-Origin" "http://example.com"}}
             (add-access-control {:headers {"origin" origin}} {} {:access-control-allow-origin #".*\.example.com"}))))))

(defn handler [request]
  ((wrap-cors (fn [_] {})
              :access-control-allow-origin #"http://example.com"
              :access-control-allow-methods [:get :put :post])
   request))

(deftest test-preflight
  (testing "success"
    (is (= {:status 200,
            :headers {"Access-Control-Allow-Origin" "http://example.com", "Access-Control-Allow-Methods" "PUT, GET, POST"},
            :body "preflight complete"}
           (handler {:request-method :options :uri "/" :headers {"origin" "http://example.com"
                                                                 "access-control-request-method" "POST"}}))))
  (testing "failure"
    (is (nil? (handler {:request-method :options :uri "/" :headers {"origin" "http://example.com"
                                                                    "access-control-request-method" "DELETE"}})))))

(deftest test-cors
  (testing "success" (is (= {:headers {"Access-Control-Allow-Methods" "PUT, GET, POST",
                                       "Access-Control-Allow-Origin" "http://example.com"}}
                            (handler {:request-method :post :uri "/" :headers {"origin" "http://example.com"}}))))
  (testing "failure"
    (is (nil? (handler {:request-method :get :uri "/" :headers {"origin" "http://foo.com"}})))))

(deftest test-no-cors
  (is (= {} (handler {:request-method :get :uri "/" :headers {"foo" "bar"}}))))