#### 背景介绍

为了帮助公司的SpringBoot项目更加简单方便接入Prometheus监控和日志输出到Redis中(方便接入ELK), 专门请教公司Java大牛学习了一下Spring相关的知识将监控和日志部分抽离出来, 采用注解的方式使用.

#### 使用方法

* 上传到公司的Maven仓库中

将该项目下载到本地, 构建并发布到公司的内网Maven仓库中.或者放到自己的业务项目中. 如果是推送到了仓库中, 业务的代码需要该依赖.

* prometheus使用方式

SpringBoot 入口函数按照如下的方式配置即可:

```java
@RestController
@SpringBootApplication
@EnablePrometheusEndpoint
@EnableSpringBootMetricsCollector
@ComponentScan("me.rockywu.*") //这里加这个注解主要是能扫描到上面POM.xml添加的依赖的那个包, 如果你自己的包的命名不是`me.rockywu.*`, 可以使用@ComponentScan("me.rockywu.*,com.yourpackage.*")
public class Application {

    @PrometheusMetrics //需要加监控的接口加上这个注解即可
    @RequestMapping("/api")
    public String index() {
        return "Hello, World!";
    }

    public static void main(String[] args) {
        SpringApplication.run(ProviderApplication.class, args);
    }
}
``` 

服务正常启动后, 监控数据的path是`/prometheus`

* logback使用方式

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <property name="ROOT_LEVEL" value="WARN"/>

    <appender name="ELK-REDIS" class="com.cwbase.logback.RedisAppender">
        <host>redis-host</host>
        <port>redis-port</port>
        <key>redis-list-key</key>
        <layout class="me.rockywu.common.log.JSONEventLayout">
            <type>dev</type>
        </layout>
    </appender>

    <!--异步输出-->
    <appender name="ASYNC_ELK" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="ELK-REDIS"/>
        <queueSize>2048</queueSize>
    </appender>

    <root level="${ROOT_LEVEL}">
        <appender-ref ref="ELK-REDIS"/>
    </root>

</configuration>

```



