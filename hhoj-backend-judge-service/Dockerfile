# 基础镜像
FROM openjdk:8-alpine
# 设置时区为东八区
ENV TZ=Asia/Shanghai

# 关键步骤：安装 tzdata 包并配置时区
RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/$TZ /etc/localtime \
    && echo $TZ > /etc/timezone
# 指定工作目录
WORKDIR /app
  
# 将 jar 包添加到工作目录，比如 target/hhoj-backend-user-service-0.0.1-SNAPSHOT.jar
ADD target/hhoj-backend-judge-service-0.0.1-SNAPSHOT.jar .
  
# 暴露端口
EXPOSE 8105
  
# 启动命令
ENTRYPOINT ["java","-jar","/app/hhoj-backend-judge-service-0.0.1-SNAPSHOT.jar"]