# Docker API Through Clojure POC
Experiments calling docker api through clojure with [contajners](https://github.com/lispyclouds/contajners)

## Using `curl`
```bash
# Pull new image
curl --unix-socket /var/run/docker.sock -X POST "http://localhost/v1.44/images/create?fromImage=alpine:latest"
# Create container based on pulled image
curl --unix-socket /var/run/docker.sock -H "Content-Type: application/json" -d '{"Image": "alpine", "Cmd": ["echo", "hello world"]}' -X POST http://localhost/v1.44/containers/create
# Wait to container to run
curl --unix-socket /var/run/docker.sock -X POST http://localhost/v1.44/containers/0d6348f653e13895091ea9dccf530a7c775189f242b7ec62b5480e7533c9727a/wait
# Get stdout logs
curl --unix-socket /var/run/docker.sock "http://localhost/v1.44/containers/0d6348f653e13895091ea9dccf530a7c775189f242b7ec62b5480e7533c9727a/logs?stdout=1" --output -
```

## Using Clojure
```clojure 
(def images-docker (c/client {:engine   :docker
                              :category :images
                              :version  "v1.44"
                              :conn     {:uri "unix:///var/run/docker.sock"}}))

(def containers-docker (c/client {:engine :docker
                                  :category :containers
                                  :version  "v1.44"
                                  :conn     {:uri "unix:///var/run/docker.sock"}}))

(c/ops images-docker)
(c/ops containers-docker)

; Pull new image
(c/invoke images-docker
          {:op     :ImageCreate
           :params {:fromImage "alpine:latest"}})

; Create container based on pulled image
(c/invoke containers-docker {:op     :ContainerCreate
                             ; :params {:name "testy"}
                             :data   {:Image "alpine:latest"
                                      :Cmd   ["echo" "hello world"]}})
(c/invoke containers-docker {:op     :ContainerStart
                             :params {:id "9752317e629ec41c4c56fa6ed2b10f1beff5dd3d834bb476dbfca70c1862f091"}})

; Wait to container to run
(c/invoke containers-docker {:op     :ContainerWait
                             :params {:id "9752317e629ec41c4c56fa6ed2b10f1beff5dd3d834bb476dbfca70c1862f091"}})

; Get stdout logs
(c/invoke containers-docker {:op     :ContainerLogs
                             :params {:id "9752317e629ec41c4c56fa6ed2b10f1beff5dd3d834bb476dbfca70c1862f091"
                                      :stdout true}})

; Delete all stopped containers
(c/invoke containers-docker {:op     :ContainerPrune})
```
