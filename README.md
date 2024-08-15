# dht-store 

Torrent资源管理库，用于存储、查询DHT爬虫爬取的种子数据、绑定下载器后支持一键下载，适合部署在NAS上，用于打造自己的资源库。

### 效果展示：
![效果演示](_doc/demo.gif "效果演示")

### 整体架构：
![](/_doc/frame.png)

### 开发环境、用到的框架和类库：
- 开发环境：
    - IDEA
    - JDK-1.8
    - Maven-3.8.1
    - RabbitMq-3.8.34
    - Elasticsearch-7.17.6
- 用到的框架：
    - SpringBoot-2.6.13
- 用到的类库：
    - [SaToken：登录认证](https://sa-token.cc/ "SaToken：简单的登录认证")
    - [HuTool](https://github.com/looly/hutool "HuTool")


### 功能简介：
- 系统状态: 展示系统状态信息，具体内容如下：
    - 仓库信息：统计爬取的种子数量、本次启动新增的数量、以及今日新增、最近一小时新增等。
    - 爬虫信息：分别统计爬虫在[IPv4]()、[IPv6]()网络下的爬取AnnouncePeer消息速度，以及转换为种子的速度和效率。
    - 下载器信息：统计当前绑定的下载器任务数量、下载速度、磁盘空间等信息。
    - 爬虫队列信息：展示爬虫在[IPv4]()、[IPv6]()网络下爬取速率折线图。
- 资源搜索：检索已爬取的种子信息，支持单独或聚合查询种子名称、包含的文件，查询结果高亮展示，支持以下查询方式：
    - 精确查询：多个关键字查询时，查询目标需要匹配所有的关键字。
    - 模糊查询：多个关键字查询时，查询目标只需匹配其中一个即可被检索。
    - 通配符查询(?,*)：关键字支持使用?/*关键字,*号表示零个或多个字符、?表示一个单一的字符。
    - 正则查询：正则表达式查询。
- 下载器: 下载器可以添加多个，但只能启用一个下载器，下载器启用后才可以下载搜索到的种子，该模块只展示当前启用的下载器任务信息，支持暂停、开始、删除任务等功能。
- 设置: 系统配置，具体功能如下：
    - 修改密码
    - 系统数据展示：设置首页(系统状态)模块需要展示的数据，例如爬虫设置为[IPv6]()模式，此时[IPv4]()数据为空，可以使用该功能屏蔽掉IPv4相关的数据的展示。
    - 退出登录
    - 下载器设置：添加下载器，填写下载器相关信息，目前支持[qBittorrent]()、[Transmission]()，添加新的下载器需要实现com.dht.store.downloader.Downloader接口

------------

### 网络环境要求
- dht-spider: IPv4: 公网IPv4或[全锥型NAT]()，家庭宽带几乎没有公网IPv4地址，但可以通过路由器拨号、DMZ主机的方式尝试获得全锥型NAT。
- dht-spider: IPv6: 家庭宽带设置好光猫和路由器基本都能获得公网IPv6，阿里云、腾讯云服务器默认不支持IPv6。(单独使用IPv6，爬虫效率最高)。
- dht-downloader: 不需要公网IPv4或IPv6，只要能正常访问IPv4或IPv6网络即可。
- dht-store: 不需要公网IPv4或IPv6。

### 硬件资源
由于服务使用Java编写，因此比较吃内存资源。威联通TS-264 NAS上测试，CPU：N5105 内存：32GB稳定运行一周内存消耗如下：

![](/_doc/resource.jpg)

### 部署前的准备

- 安装qBittorrent或者Transmission服务(已有则不用安装)
- 填写dht-spider服务DHT_CONFIG_CLUSTER_ID(集群ID,8位随机数)
- Elasticsearch需要安装[ik分词器](https://github.com/infinilabs/analysis-ik)，[点击下载](https://github.com/infinilabs/analysis-ik/releases/tag/v7.17.6)分词器解压至/home/elasticsearch/plugins目录，重启容器即可完成安装。

  ![](/_doc/ik.png)
- `docker pull`失败可以手动下载镜像([Docker镜像下载](https://github.com/xiaomujiayou/dht-store/releases))下载后可使用`docker load -i <文件>.tar`导入docker本地镜像库

### DockerComposes部署(推荐)
- 威联通、群晖直接使用系统自带Container Station等工具进行安装。
- 服务器：复制以下代码到文件docker-compose.yml并上传至服务器，在当前文件夹执行`docker-compose up -d`

```
version: "3"

services:
  rabbit:
    image: rabbitmq:3.8-management
    hostname: rabbit
    container_name: rabbitmq
    restart: unless-stopped
    environment:
      - RABBITMQ_DEFAULT_USER=root    #MQ初始登录账号
      - RABBITMQ_DEFAULT_PASS=123456  #MQ初始登录密码
    network_mode: host
    healthcheck:
      test: ["CMD", "rabbitmqctl", "status"]
      interval: 10s
      timeout: 5s
      retries: 10      

  elasticsearch:
    image: elasticsearch:7.17.6
    restart: unless-stopped
    container_name: elasticsearch
    environment:
      - ES_JAVA_OPTS=-Xms512m -Xmx512m  #根据服务器实际情况设置
      - discovery.type=single-node
    volumes:
      - /home/elasticsearch/plugins:/usr/share/elasticsearch/plugins    #将ik分词器解压至服务器/home/elasticsearch/plugins目录后重启容器，即可安装完成。
    network_mode: host       
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:9200/_cluster/health || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 10       

  dht-spider:
    image: xiaomujiayou/dht-spider:1.0
    container_name: dht-spider
    environment:
      - JAVA_OPTS=-Xmx512m -Xms256m -Xmn64m -Xss32m       #java启动参数，内存大小可根据服务器配置适当调节
      - SPRING_RABBITMQ_HOST=127.0.0.1                    #rabbitmq地址
      - SPRING_RABBITMQ_PORT=5672                         #rabbitmq端口
      - SPRING_RABBITMQ_USERNAME=root                     #rabbitmq账号
      - SPRING_RABBITMQ_PASSWORD=123456                   #rabbitmq密码
      - DHT_CONFIG_CLUSTER_ID=                            #集群ID,8位随机数，务必填写(如果集群部署，请保证同个集群ID一致)
      - DHT_CONFIG_PORT=9527                              #爬虫端口
      - DHT_CONFIG_NETWORK=ipv4,ipv6                      #爬虫网络设置，服务器网络为IPv4公网或者全锥型NAT则可以填写"ipv4"，服务器网络为IPv6公网则可以填写"ipv6"，如果两者都有则填写"ipv4,ipv6"(单独使用IPv6，速度远远高于IPv4)
      - DHT_CONFIG_SPEED=300                              #网络限速(单位:KB)，限制爬虫上传速度，防止爬虫占完所有带宽，影响局域网其他设备或导致服务器失联。
      - DHT_CONFIG_SHARE=true                             #数据共享，共享爬取到的AnnounceMsg，当然你也会收到他人共享的数据。
    network_mode: host
    depends_on:
      rabbit:
        condition: service_healthy

  dht-downloader:
    image: xiaomujiayou/dht-downloader:1.0
    container_name: dht-downloader
    environment:
      - JAVA_OPTS=-Xmx512m -Xms256m -Xmn64m -Xss32m   #java启动参数，内存大小可根据服务器配置适当调节
      - SPRING_RABBITMQ_HOST=127.0.0.1                    #rabbitmq地址
      - SPRING_RABBITMQ_PORT=5672                         #rabbitmq端口
      - SPRING_RABBITMQ_USERNAME=root                     #rabbitmq账号
      - SPRING_RABBITMQ_PASSWORD=123456                   #rabbitmq密码
      - DOWNLOADER_NETWORK=ipv4,ipv6                      #网络设置，服务器不需要IPv4公网或IPv6公网,只要能访问IPv4或者IPv6即可对应填写(最好部署在支持IPv4、IPv6双网的服务器上，否则dht-spider爬取的IPv4或者IPv6消息不能被转换)
      - DOWNLOADER_CONNECTION=200                         #并发下载种子的数量，请根据网络实际情况修改
      - DOWNLOADER_HAVETORRENT=false                      #是否上传种子源文件到MQ，为true则会把种子文件一并上传到RabbitMQ(dht-store服务并不需要种子原文件,因此设置为false)
    network_mode: host
    depends_on:
      rabbit:
        condition: service_healthy
      
  dht-store:
    image: xiaomujiayou/dht-store:1.0
    container_name: dht-store
    environment:
      - JAVA_OPTS=-Xmx512m -Xms256m -Xmn64m -Xss32m   #java启动参数，内存大小可根据服务器配置适当调节
      - SERVER_PORT=8888                              #项目运行端口
      - SPRING_RABBITMQ_HOST=127.0.0.1                #RabbitMQ地址
      - SPRING_RABBITMQ_PORT=5672                     #RabbitMQ端口
      - SPRING_RABBITMQ_ADMINPORT=15672               #RabbitMQ管理后台端口
      - SPRING_RABBITMQ_USERNAME=root                 #RabbitMQ账号
      - SPRING_RABBITMQ_PASSWORD=123456               #RabbitMQ密码
      - SPRING_ELASTICSEARCH_URIS=127.0.0.1:9200      #Elasticsearch地址
      - AUTH_USERNAME=admin                           #Web端初始化账号
      - AUTH_PASSWORD=123456                          #Web端初始化密码
    network_mode: host
    depends_on:
      rabbit:
        condition: service_healthy
      elasticsearch:
        condition: service_healthy
      
```
### Docker部署
注：请按以下顺序启动服务
- RabbitMQ
```
docker run -d --name rabbitmq --hostname rabbit --restart unless-stopped \
  --network host \
  -e RABBITMQ_DEFAULT_USER=root \
  -e RABBITMQ_DEFAULT_PASS=123456 \
  rabbitmq:3.8-management
```
- Elasticsearch
```
docker run -d --name elasticsearch --restart unless-stopped \
  --network host \
  -e ES_JAVA_OPTS="-Xms512m -Xmx512m" \
  -e discovery.type=single-node \
  -v /home/elasticsearch/plugins:/usr/share/elasticsearch/plugins \
  elasticsearch:7.17.6
```
- DhtStore
```
docker run -d --name dht-store \
  --network host \
  -e JAVA_OPTS="-Xmx512m -Xms256m -Xmn64m -Xss32m" \
  -e SERVER_PORT=8888 \
  -e SPRING_RABBITMQ_HOST=127.0.0.1 \
  -e SPRING_RABBITMQ_PORT=5672 \
  -e SPRING_RABBITMQ_ADMINPORT=15672 \
  -e SPRING_RABBITMQ_USERNAME=root \
  -e SPRING_RABBITMQ_PASSWORD=123456 \
  -e SPRING_ELASTICSEARCH_URIS=127.0.0.1:9200 \
  -e AUTH_USERNAME=admin \
  -e AUTH_PASSWORD=123456 \
  xiaomujiayou/dht-store:1.0
```
- DhtDownloader
```
docker run -d --name dht-downloader \
  --network host \
  -e JAVA_OPTS="-Xmx512m -Xms256m -Xmn64m -Xss32m" \
  -e SPRING_RABBITMQ_HOST=127.0.0.1 \
  -e SPRING_RABBITMQ_PORT=5672 \
  -e SPRING_RABBITMQ_USERNAME=root \
  -e SPRING_RABBITMQ_PASSWORD=123456 \
  -e DOWNLOADER_NETWORK="ipv4,ipv6" \
  -e DOWNLOADER_CONNECTION=200 \
  -e DOWNLOADER_HAVETORRENT=false \
  xiaomujiayou/dht-downloader:1.0
```
- DhtSpider
```
docker run -d --name dht-spider \
  --network host \
  -e JAVA_OPTS="-Xmx512m -Xms256m -Xmn64m -Xss32m" \
  -e SPRING_RABBITMQ_HOST=127.0.0.1 \
  -e SPRING_RABBITMQ_PORT=5672 \
  -e SPRING_RABBITMQ_USERNAME=root \
  -e SPRING_RABBITMQ_PASSWORD=123456 \
  -e DHT_CONFIG_CLUSTER_ID= \
  -e DHT_CONFIG_PORT=9527 \
  -e DHT_CONFIG_NETWORK="ipv4,ipv6" \
  -e DHT_CONFIG_SPEED=300 \
  -e DHT_CONFIG_SHARE=true \
  xiaomujiayou/dht-spider:1.0
```

### Jar包运行(不推荐) [下载](https://github.com/xiaomujiayou/dht-store/releases)
- RabbitMQ，请自行安装
- Elasticsearch，请自行安装
- DhtSpider
```
java -Xmx512m -Xms256m -Xmn128m -Xss8m -javaagent:spider-1.0.jar -jar spider-1.0.jar \
  -Duser.timezone=GMT+08 \
  --spring.rabbitmq.host=127.0.0.1 \
  --spring.rabbitmq.port=5672 \
  --spring.rabbitmq.username=root \
  --spring.rabbitmq.password=123456 \
  --dht.config.cluster.id= \
  --dht.config.port=9527 \
  --dht.config.network=ipv4,ipv6 \
  --dht.config.speed=300 \
  --dht.config.share=true 
```
- DhtDownloader
```
java -Xmx512m -Xms256m -Xmn128m -Xss8m -javaagent:downloader-1.0.jar -jar downloader-1.0.jar \
  -Duser.timezone=GMT+08 \
  --spring.rabbitmq.host=127.0.0.1 \
  --spring.rabbitmq.port=5672 \
  --spring.rabbitmq.username=root \
  --spring.rabbitmq.password=123456 \
  --downloader.network=ipv4,ipv6 \
  --downloader.connection=200 \
  --downloader.havetorrent=false
```
- DhtStore
```
java -Xmx512m -Xms256m -Xmn128m -Xss8m -jar store-1.0.jar \
  -Duser.timezone=GMT+08 \
  --server.port=8888 \
  --spring.rabbitmq.host=127.0.0.1 \
  --spring.rabbitmq.port=5672 \
  --spring.rabbitmq.admin-port=15672 \
  --spring.rabbitmq.username=root \
  --spring.rabbitmq.password=123456 \
  --spring.elasticsearch.uris=127.0.0.1:9200 \
  --auth.userName=admin \
  --auth.password=123456
```
项目启动后浏览器打开`http://dht-store:8888/` 默认账号：`admin` 密码：`123456`

所有依赖打包：

- [Release下载](https://github.com/xiaomujiayou/dht-store/releases)
- [百度云下载](https://pan.baidu.com/s/1dwIfUS9CS-WwOI6Wrb1UnQ?pwd=3kre) 

### 前端更新
- 前端代码使用Vue+Vant UI实现，仓库地址：[跳转链接](https://github.com/xiaomujiayou/dht-store-web)
- 编译后的前端代码复制到`resources\static`重启项目即可
------------
### 问题反馈
![微信](https://mall-share.oss-cn-shanghai.aliyuncs.com/share/my.jpg?x-oss-process=image/resize,h_200,w_200 "微信")
