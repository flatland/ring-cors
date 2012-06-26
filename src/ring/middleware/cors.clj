(ns ring.middleware.cors
  "Ring middleware for Cross-Origin Resource Sharing."
  (:use [clojure.string :only (capitalize join split)]))

(defn origin
  "Returns the Origin request header."
  [request] (get (:headers request) "origin" "none specified"))

(defn allow-request?
  "Returns true if the request's origin matches the access control
  origin, otherwise false."
  [request access-control]
  (let [origin (origin request)
        allowed (:access-control-allow-origin access-control)]
    (if (and origin allowed (some #(re-matches % origin) (if (sequential? allowed) allowed [allowed])))
      true false)))

(defn header-name
  "Returns the capitalized header name as a string."
  [header] (if header (join "-" (map capitalize (split (name header) #"-")))))

(defn normalize-headers
  "Normalize the headers by converting them to capitalized strings."
  [headers] (reduce #(assoc %1 (header-name (first %2)) (last %2)) {} headers))

(defn add-access-control
  "Add the access control headers using the request's origin to the response."
  [request response access-control]
  (if-let [origin (origin request)]
    (let [access-headers (normalize-headers (assoc access-control :access-control-allow-origin origin))]
      (assoc response :cors-headers access-headers))
    response))

(defn wrap-cors
  "Middleware that adds Cross-Origin Resource Sharing headers.

Example:

  (def handler
    (-> routes
        (wrap-cors
         :access-control-allow-origin #\"http://example.com\")))
"
  [handler & access-control]
  (let [access-control (apply hash-map access-control)]
    (fn [request]      
      (when-let [resp (and (allow-request? request access-control)
                           (handler request))]
        (add-access-control request resp access-control)))))

(defn wrap-cors-headers
  "Should be placed near the top of the stack.
   If a cors request was allowed earlier on, the response will
   contain a :cors-headers key. This will be merged into the
   response headers."
  [handler]
  (fn [request]
    (when-let [resp (handler request)]
      (if (get (:headers request) "origin")
        (if-let [headers (:cors-headers resp)]
          (update-in resp [:headers] merge headers)
          {:status 403
           :headers {}
           :body "CORS Request Denied."})
        resp))))