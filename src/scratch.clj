(ns scratch
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [contajners.core :as c]))

(def images-docker (c/client {:engine   :docker
                              :category :images
                              :version  "v1.44"
                              :conn     {:uri "unix:///var/run/docker.sock"}}))

(def containers-docker (c/client {:engine :docker
                                  :category :containers
                                  :version  "v1.44"
                                  :conn     {:uri "unix:///var/run/docker.sock"}}))

(c/categories :docker "v1.44")
(c/ops images-docker)
(c/ops containers-docker)

(defn wait-read-all-stream [type stream]
  (with-open [reader (io/reader stream)]
    (loop [curent-reader reader
           output []]
      (let [line (.readLine curent-reader)]
        (if-not line
          output
          (recur curent-reader
                 (conj output (if (= type :json)
                                (json/read-str line :key-fn keyword)
                                line))))))))

(defn image-pulled? [image-name]
  (let [images (c/invoke images-docker {:op :ImageList})]
    (contains? (->> images
                    (mapcat :RepoTags)
                    (into #{}))
               image-name)))

(defn download-and-wait-image [image-name]
  (wait-read-all-stream :json
                        (c/invoke images-docker
                                  {:op     :ImageCreate
                                   :params {:fromImage image-name}
                                   :as     :stream
                                   :throw-exceptions true
                                   :throw-entire-message true})))

(let [image-name "clojure:temurin-11-tools-deps"
      image-results (download-and-wait-image image-name)
      image-pulled (image-pulled? image-name)
      container-name "clojure"
      ; Create clojure container
      container-create-result (c/invoke containers-docker
                                        {:op   :ContainerCreate
                                         :params {:name container-name}
                                         :data {:Image image-name
                                                :WorkingDir "/usr/src/app"
                                                :Cmd  ["clojure" "-M" "solution.clj"]}})
      ; Add files to created container
      ; tar --no-xattr --no-mac-metadata -czvf src.tar.gz -C example .
      add-files-result (wait-read-all-stream :text
                                             (c/invoke containers-docker
                                                       {:op     :PutContainerArchive
                                                        :params {:id container-name
                                                                 :path "/usr/src/app"}
                                                        :data (-> "src.tar.gz"
                                                                  io/file
                                                                  io/input-stream)
                                                        :as     :stream
                                                        :throw-exceptions true
                                                        :throw-entire-message true}))

      ; Container start
      container-start-result (c/invoke containers-docker
                                       {:op     :ContainerStart
                                        :params {:id container-name}})
      ; Wait container stop
      container-wait-result (c/invoke containers-docker {:op     :ContainerWait
                                                         :params {:id container-name}})
      ; Get container logs
      container-logs (c/invoke containers-docker
                               {:op     :ContainerLogs
                                :params {:id container-name
                                         :stdout true
                                         :stderr true}})
            ; Delete clojure stopped containers
      container-prune-result (c/invoke containers-docker
                                       {:op     :ContainerPrune
                                        :params {:name container-name}})]
  {:image-results image-results
   :image-pulled image-pulled
   :container-create-result container-create-result
   :add-files-result add-files-result
   :container-start-result container-start-result
   :container-wait-result container-wait-result
   :container-logs container-logs
   :container-prune-result container-prune-result})

(comment
  (def image-name "clojure:temurin-11-tools-deps")
  (def container-name "clojure")
  (c/invoke containers-docker {:op :ContainerPrune}))
