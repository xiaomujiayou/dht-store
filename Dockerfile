FROM openjdk:8

RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo 'Asia/Shanghai' >/etc/timezone
ADD target/*.jar target.jar
#java启动参数
ENV JAVA_OPTS="-Xmx512m -Xms256m -Xmn64m -Xss32m"
#项目运行端口
ENV SERVER_PORT=8888
#RabbitMQ地址
ENV SPRING_RABBITMQ_HOST=127.0.0.1
#RabbitMQ端口
ENV SPRING_RABBITMQ_PORT=5672
#RabbitMQ管理后台端口
ENV SPRING_RABBITMQ_ADMINPORT=15672
#RabbitMQ账号
ENV SPRING_RABBITMQ_USERNAME=root
#RabbitMQ密码
ENV SPRING_RABBITMQ_PASSWORD=123456
#Elasticsearch地址
ENV SPRING_ELASTICSEARCH_URIS=127.0.0.1:9200
#Web端初始化账号
ENV AUTH_USERNAME=admin
#Web端初始化密码
ENV AUTH_PASSWORD=123456

EXPOSE $SERVER_PORT
ENTRYPOINT exec java $JAVA_OPTS -jar /target.jar -Duser.timezone=GMT+08
#ENTRYPOINT ["sh", "-c", "java", "$JAVA_OPTS", "-jar", "/target.jar","-Duser.timezone=GMT+08"]