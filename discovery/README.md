







# 트러블 슈팅 (Client 쪽)



## Service 등록이 왜케 느릴까?

인스턴스를 등록할 때, 하트비트 시간이 있기 때문에, 기본적으로 30초가 걸리게 됩니다. 인스턴스, 서버 및 클라이언트가 모두 로컬 캐시에 동일한 메타 데이터를 가져야 하기 때문입니다.(3번의 하트비트가 걸리는 시간) `eureka.instance.leaseRenewalIntervalInSeconds` 의 값을 변경함으로 써 걸리는 시간을 변경할 수 있습니다. 30초 미만으로 설정하면, 클라이언트가 다른 서비스와 연결하는 시간이 짧아지지만, 그렇지만 이렇게 설정하면 실제 배포할 때는 하트비트를 짧은 시간에 여러번 하기 때문에 디폴트인 30으로 하는 것이 좋습니다. 

- 인스턴스 : 이미 올라가 있는 서비스
- 클라이언트 : 올릴 서비스
- 서버 : 디스커버리 서버



## Zone 이라는 개념도 있네?

Zone이란게 뭘까? 



## 클라이언트 새로고침하기

기본적으로 클라이언트는 새로고침이 가능합니다. 새로고침이 일어나게 되면, 클라이언트는 유레카 서버에서 잠깐 동안 사용불가능 상태가 됩니다. 새로고침을 방지하기 위해서는 `eureka.client.refresh.enable=false` 를 사용하면 됩니다.





# 서킷 브레이커 , 히스트릭스



## 서킷브레이커 히스트릭스 disable 하기

`spring.cloud.circuitbreaker.hystrix.enabled=false` 을 해줌으로써 auto-configura을 해제할 수 있습니다. 



### 기본 설정

모든 circuit breaker의 기본 설정을 주고 싶으면 `HystrixCircuitBreakerFactory` 또는 `ReactiveHystrixCircuitBreakerFactory` bean을 만들면 됩니다. 

```java
@Bean
public Customizer<HystrixCircuitBreakerFactory> defaultConfig() {
    return factory -> factory.configureDefault(id -> HystrixCommand.Setter
            .withGroupKey(HystrixCommandGroupKey.Factory.asKey(id))
            .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
            .withExecutionTimeoutInMilliseconds(4000)));
}
```

#### Reactive 쓸때,

```java
@Bean
public Customizer<ReactiveHystrixCircuitBreakerFactory> defaultConfig() {
    return factory -> factory.configureDefault(id -> HystrixObservableCommand.Setter
            .withGroupKey(HystrixCommandGroupKey.Factory.asKey(id))
            .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                    .withExecutionTimeoutInMilliseconds(4000)));
}
```

### 특정 서킷 브레이커 설정하기

```java
@Bean
public Customizer<HystrixCircuitBreakerFactory> customizer() {
    return factory -> factory.configure(builder -> builder.commandProperties(
                    HystrixCommandProperties.Setter().withExecutionTimeoutInMilliseconds(2000)), "foo", "bar");
}
```

#### Reactive 쓸때,

```java
@Bean
public Customizer<ReactiveHystrixCircuitBreakerFactory> customizer() {
    return factory -> factory.configure(builder -> builder.commandProperties(
                    HystrixCommandProperties.Setter().withExecutionTimeoutInMilliseconds(2000)), "foo", "bar");
}
```



## Circuit Breaker : Hystrix 클라이언트

넷플릭스는, Circuit breaker 패턴을 이용하여 Hystrix을 만들었습니다. 마이크로서비스 아키텍쳐에서,다음과 같이 여러 개의 서비스 레이어를 호출 하는 것이 흔해졌습니다.

![](https://raw.githubusercontent.com/spring-cloud/spring-cloud-netflix/master/docs/src/main/asciidoc/images/Hystrix.png)



### 탐색 1. 마이크로서비스 그래프

로우 레벨 서비스에서 서비스 오류가 발생하면 사용자까지 연속의 오류가 발생할 수 있습니다. 서비스 호출이 `circuitBreaker.requestVolumeThreshold` 설정된 시간을 초과하거나(기본 : 20호출), `metrics.rollingStats.timeInMilliseconds` 에 설정된 시간동안(기본 : 10초)    `circuitBreaker.errorThresholdPercentage` (기본 : 50% 이상) 에 설정한 에러율이 이상되면 circuit이 발생하며, 더 이상 호출되지 않습니다.



![](https://raw.githubusercontent.com/spring-cloud/spring-cloud-netflix/master/docs/src/main/asciidoc/images/HystrixFallback.png)



### 탐색 2. 연속의 오류를 방지하는 히스트릭스 폴백

서킷 브레이커를 사용하면, 망가진 서비스 회복할 시간을 가질 수 있게 됩니다. fallback은 히스트릭스를 보호합니다. The fallback can be another Hystrix protected call, static data, or a sensible empty value. Fallbacks may be chained so that the first fallback makes some other business call, which in turn falls back to static data.



## 히스트릭스를 포함하는 방법

프로젝트에 히스트릭스를 포함하고 싶다면, `org.springframework.cloud` 의 `spring-cloud-starter-netflix-hystrix` 라이브러리를 넣어주면 됩니다.

다음은 히스트릭스 서킷 브레이커를 사용한 유레카 서버입니다.

```java
@SpringBootApplication
@EnableCircuitBreaker
public class Application {

    public static void main(String[] args) {
        new SpringApplicationBuilder(Application.class).web(true).run(args);
    }

}

@Component
public class StoreIntegration {

    @HystrixCommand(fallbackMethod = "defaultStores")
    public Object getStores(Map<String, Object> parameters) {
        //do stuff that might fail
    }

    public Object defaultStores(Map<String, Object> parameters) {
        return /* something useful */;
    }
}
```



`@HystrixCommand` 는 스프링 클라우드가 히스트릭스 서킷 브레이커로 연결된 프록시로 자동으로 감싸줍니다. 서킷 브레이커는 서킷 브레이커를 언제 활성화 할지 결정하며, 실패 시 어떻게 해야할지 결정합니다.

`@HystrixCommand`를 설정하기 위해서는 @HystrixProperty 어노테이션의 commandProperties 속성을 이용합니다. [HystrixProperty](https://github.com/Netflix/Hystrix/tree/master/hystrix-contrib/hystrix-javanica#configuration)



### Security Context 전파하기 , Spring Scopes 이용하기

스레드 로컬의 컨텍스트를 @HystrixCommand로 전파하고 싶다면 , 기본 설정으로는 전파가 되지 않습니다. 왜냐하면 스레드 풀안에서 명령이 실행되기 때문입니다. 설정 또는 어노테이션을 사용하여, 히스트릭스에게 호출했던 스레드를 그대로 사용하라고 설정해 줄 수 있습니다. 

```java
@HystrixCommand(fallbackMethod = "stubMyService",
    commandProperties = {
      @HystrixProperty(name="execution.isolation.strategy", value="SEMAPHORE")
    }
)
...
```



이러한 동작을 @SessionScope 또는 @RequestScope를 이용하여 똑같이 적용할 수 있습니다.  scoped context를 찾지 못하는 런타임 예외를 만나게되면, 똑같은 스레드를 사용해야만 합니다.(If you encounter a runtime exception that says it cannot find the scoped context, you need to use the same thread.)

또한 `hystrix.shareSecurityContext` 속성을 true로 설정하면 히스트릭스 concurrency strategy 플러그인이 `SecurityContext` 를 메인 스레드에서 히스트릭스 커맨드가 사용할 스레드로 전달해줍니다. 히스트릭스는  여러개의 히스트릭스 concurrency strategy를 등록하는걸 허용하지 않으므로, `HystrixConcurrencyStrategy` 를 스프링 bean으로 등록하여 사용하면 됩니다. 

### Health Indicator

`/health` 엔드포인트를 이용하여 다음과 같이 애플리케이션의 서킷 브레이커 상태를 알 수 있습니다.

```json
{
    "hystrix": {
        "openCircuitBreakers": [
            "StoreIntegration::getStoresByLocationLink"
        ],
        "status": "CIRCUIT_OPEN"
    },
    "status": "UP"
}
```



### 히스트릭스 Metrics Stream

metrics stream을 사용하기 위해서는 `spring-boot-starter-actuator` 과 `management.endpoints.web.exposure.include: hystrix.stream` 디펜던시가 필요합니다. `/actuator/hystrix.stream` 엔드포인트로 관리할 수 있습니다.



## 서킷 브레이커 : 히스트릭스 대시보드

히스트릭스의 주요 장점 중인 하나는 각각 히스트릭스 커맨드에 대한 메트릭스를 한군데로 모아준다는 점 입니다.

히스트릭스 대시보드는 효과적인 방법으로 각각의 서킷 브레이커 상태를 대시보드에 보여줍니다. 

![](https://raw.githubusercontent.com/spring-cloud/spring-cloud-netflix/master/docs/src/main/asciidoc/images/Hystrix.png)



## 히스트릭스 타임아웃과 리본 클라이언트

리본 클라이언트로 감싼 히스트릭스 커맨드를 사용할 때, 설정된 리본 타임아웃 또는 재시도 횟수를 히스트릭스에서 타임아웃을 길게 가져가고 싶을 때가 있습니다. 예를 들어, 리본 커넥션 타임아웃이 1초이고 재시도 횟수를 3번이라고 가정했을 때, 히스트릭스 타임아웃은 3초 이상으로 설정해줄 수 있습니다.

### 

### 히스트릭스 대시보드를 어떻게 포함하나요?

프로젝트에 히스트릭스 대시보드를 포함하고 싶으면, `org.springframework.cloud:spring-cloud-starter-netflix-hystrix-dashboard` 를 포함해주면 됩니다. 

히스트릭스 대시보드를 실행하고 싶다면 Spring Boot 메인 클래스에 `@EnableHystrixDashboard` 를 설정해주면 됩니다. 그 다음 `/hystrix` 와 `/hystrix.stream` 엔드포인트에 접속하면 됩니다. 



>  When connecting to a `/hystrix.stream` endpoint that uses HTTPS, the certificate used by the server must be trusted by the JVM. If the certificate is not trusted, you must import the certificate into the JVM in order for the Hystrix Dashboard to make a successful connection to the stream endpoint.



>  In order to use the `/proxy.stream` endpoint you must configure a list of hosts to allow connections to. To set the list of allowed hosts use `hystrix.dashboard.proxyStreamAllowList`. You can use an Ant-style pattern in the host name to match against a wider range of host names.



### 터빈 (써보면서 이해하면서 공부해야할듯.)

독립적인 히스트릭스의 인스턴스 데이터를 보면, 시스템의 전반적인 건강측정에서는 그다지 유용하지 않습니다. 터빈은 `/hystrix.stream` 엔드포인트와 `/turbine.stream` 을 결합하여 관련있는 데이터를 히스트릭스 대시보드에 모아줍니다. 독립적인 인스턴스들은 유레카를 통해 위치해 있습니다. 터빈을 사용하기 위해서는 스프링 부트 메인 클래스에 `@EnableTurbin` 어노테이션을 붙여주면 됩니다.(spring-cloud-starter-netflix-turbine 의존성 필요.) 

> 기본적으로 터빈은 유레카안에 등록된 인스턴스의 hostName과 port를 이용해 `/hystrix.stream` 엔드포인트를 찾으며 정보들을 /hystrix.stream에 이어 붙입니다.
>
>  By default, Turbine looks for the `/hystrix.stream` endpoint on a registered instance by looking up its `hostName` and `port` entries in Eureka and then appending `/hystrix.stream` to it. If the instance’s metadata contains `management.port`, it is used instead of the `port` value for the `/hystrix.stream` endpoint. By default, the metadata entry called `management.port` is equal to the `management.port` configuration property. It can be overridden though with following configuration:



```yaml
eureka:
  instance:
    metadata-map:
      management.port: ${management.port:8081}
```

`turbine.appconfig` 설정 키는 터빈이 인스턴스를 찾기 위해 사용하는 유레카 서비스ID의 리스트 입니다.터빈 stream은 다음과 비슷한  URL로 히스트릭스 대시보드에서 사용됩니다.

[my.turbine.server:8080/turbine.stream?cluster=CLUSTERNAME](https://my.turbine.server:8080/turbine.stream?cluster=CLUSTERNAME)

name이 `default` 라면 cluster 파라미터는 생략해도 됩니다. cluster의 파라미터는 반드시 `turbine.aggregator.clusterConfig` 설정과 일치해야 합니다. 유레카에서 리턴되는 값은 upperCase 입니다. 다음의 예제는 유레카에 `customers` 라는 애플리케이션이 등록되어있으면 정상작동 합니다.

```yaml
turbine:
  aggregator:
    clusterConfig: CUSTOMERS
  appConfig: customers
```

만약 터빈이 사용하는 클러스터 네임을 커스터마이징 하고 싶다면 `TurbinClustersProvider` 빈을 제공해주면 됩니다.

The `clusterName` can be customized by a SPEL expression in `turbine.clusterNameExpression` with root as an instance of `InstanceInfo`. 기본 값은 유레카의 cluster key가 되는 appName이 기본 값입니다. 

(여긴 직접 내가 써봐야 이해가 될것 같다.)



## 클라이언트 사이드 로드 밸런서 : 리본

리본은 HTTP , TCP 클라이언트의 동작을 컨트롤 할 수 있는 클라이언트 사이드 로드 밸런서 입니다. Feign은 이미 리본을 사용하고 있기 때문에 @FeignClient 어노테이션을 사용하면 바로 적용됩니다.

리본의 메인 컨셉은 네임드 클라이언트 입니다. 각각의 로드밸런서는 요청시 원격 서버에 연결하기 위해 함께 작동하는 구성 요소 앙상블의 일부이며, 앙상블에는 애플리케이션 개발자가 지정한 애플리케이션 이름이 있습니다

A central concept in Ribbon is that of the named client. Each load balancer is part of an ensemble of components that work together to contact a remote server on demand, and the ensemble has a name that you give it as an application developer (for example, by using the `@FeignClient` annotation). On demand, Spring Cloud creates a new ensemble as an `ApplicationContext` for each named client by using `RibbonClientConfiguration`. This contains (amongst other things) an `ILoadBalancer`, a `RestClient`, and a `ServerListFilter`.



### 리본을 포함하는 방법

프로젝트에 리본을 사용하려면 `org.springframework.cloud:spring-cloud-starter-netflix-ribbon` 을 포함하면 됩니다.



## 커스터마이징 : 리본 클라이언트

리본 설정은 외부 프로퍼티인 `<client>.ribbon.*` 을 설정함으로써 커스터마이징이 가능합니다. 

스프링 클라우드는 또한 다음과 같이 추가적인 설정으로 클라이언트의 모든 조작을 허용해줍니다.

```java
@Configuration
@RibbonClient(name = "custom", configuration = CustomConfiguration.class)
public class TestConfiguration {
}
```



> `CustomConfiguration` 클래스는 반드시 `@Configuration` 어노테이션을 포함해야 하지만 메인 Application context의 @ComponentScan으로 올리면 안됩니다. 모든 @RiboonClient가 공유하기 때문 입니다. 그러므로 반드시, Application context에 올라가지 않도록 주의를 기울어야 합니다.
>
> 겹치지 않는 별도의 패키지에 넣거나 @ComponentScan에서 명시적으로 스캔할 패키지를 지정해주면 됩니다.

다음은 기본적으로 스프링 클라우드 넷플릭스가 리본을 위해 기본적으로 제공하는 빈의 목록 들입니다.



|         Bean Type          |         Bean Name         |            Class Name            |
| :------------------------: | :-----------------------: | :------------------------------: |
|      `IClientConfig`       |   `ribbonClientConfig`    |    `DefaultClientConfigImpl`     |
|          `IRule`           |       `ribbonRule`        |       `ZoneAvoidanceRule`        |
|          `IPing`           |       `ribbonPing`        |           `DummyPing`            |
|    `ServerList<Server>`    |    `ribbonServerList`     |  `ConfigurationBasedServerList`  |
| `ServerListFilter<Server>` | `ribbonServerListFilter`  | `ZonePreferenceServerListFilter` |
|      `ILoadBalancer`       |   `ribbonLoadBalancer`    |     `ZoneAwareLoadBalancer`      |
|    `ServerListUpdater`     | `ribbonServerListUpdater` |    `PollingServerListUpdater`    |

다음과 같이 위에 있는 Bean type중 몇 개를 만들고 @RibbonClient 설정안에서 설정을 해주면 다음과 같이 오버라이드가 가능해집니다.

```java
@Configuration(proxyBeanMethods = false)
protected static class FooConfiguration {

    @Bean
    public ZonePreferenceServerListFilter serverListFilter() {
        ZonePreferenceServerListFilter filter = new ZonePreferenceServerListFilter();
        filter.setZone("myTestZone");
        return filter;
    }

    @Bean
    public IPing ribbonPing() {
        return new PingUrl();
    }

}
```



### 모든 리본 클라이언트에게 기본 설정 주기

`@RibbonClients` 어노테이션을 사용하면 다음과 같이 모든 리본 클라이언트에게 기본설정을 등록해줄 수 있습니다. 

```java
@RibbonClients(defaultConfiguration = DefaultRibbonConfig.class)
public class RibbonClientDefaultConfigurationTestsConfig {

    public static class BazServiceList extends ConfigurationBasedServerList {

        public BazServiceList(IClientConfig config) {
            super.initWithNiwsConfig(config);
        }

    }

}

@Configuration(proxyBeanMethods = false)
class DefaultRibbonConfig {

    @Bean
    public IRule ribbonRule() {
        return new BestAvailableRule();
    }

    @Bean
    public IPing ribbonPing() {
        return new PingUrl();
    }

    @Bean
    public ServerList<Server> ribbonServerList(IClientConfig config) {
        return new RibbonClientDefaultConfigurationTestsConfig.BazServiceList(config);
    }

    @Bean
    public ServerListSubsetFilter serverListFilter() {
        ServerListSubsetFilter filter = new ServerListSubsetFilter();
        return filter;
    }

}
```

### 

### 프로퍼티 셋팅으로 리본 클라이언트 커스터마이징 하기

스프링 클라우드 넷플릭스 1.2.0 부터 프로퍼티를 이용해 리본 클라이언트을 커스터마이징 할 수 있게 됩니다. 

- `<clientName>.ribbon.NFLoadBalancerClassName`: Should implement `ILoadBalancer`
- `<clientName>.ribbon.NFLoadBalancerRuleClassName`: Should implement `IRule`
- `<clientName>.ribbon.NFLoadBalancerPingClassName`: Should implement `IPing`
- `<clientName>.ribbon.NIWSServerListClassName`: Should implement `ServerList`
- `<clientName>.ribbon.NIWSServerListFilterClassName`: Should implement `ServerListFilter`



이 값들은 위에서 보여드린 @RibbonClient(configuration=MyRibbonConfig.class)의 설정보다 우선시 합니다. 

`user` 라는 서비스의 `IRule`을 설정하고 싶으면 다음과 같이 프로퍼티를 설정합니다.

**application.yml**

```yaml
users:
  ribbon:
    NIWSServerListClassName: com.netflix.loadbalancer.ConfigurationBasedServerList
    NFLoadBalancerRuleClassName: com.netflix.loadbalancer.WeightedResponseTimeRule
```

 [Ribbon documentation](https://github.com/Netflix/ribbon/wiki/Working-with-load-balancers) 



### 유레카와 함께 리본 사용

유레카를 리본과 함께 사용하는 경우에는, `ribbonServerList` 는 `DiscoveryEnabledNIWSServerList`의 확장(extension)으로 오버라이드 됩니다. 이 클래스는 유레카의 서버 목록을 채웁니다. 또한 `IPnig` 인터페이스 또한   `NIWSDiscoveryPing` 클래스로 대체되는데, 이 클래스는 서버가 작동중인지 확인하기 위해 유레카에게 위임 합니다. 기본적으로 설치되는 `ServerList` 는 `DomainExtractingServerList` 입니다.

When Eureka is used in conjunction with Ribbon (that is, both are on the classpath), the `ribbonServerList` is overridden with an extension of `DiscoveryEnabledNIWSServerList`, which populates the list of servers from Eureka. It also replaces the `IPing` interface with `NIWSDiscoveryPing`, which delegates to Eureka to determine if a server is up. The `ServerList` that is installed by default is a `DomainExtractingServerList`. Its purpose is to make metadata available to the load balancer without using AWS AMI metadata (which is what Netflix relies on). By default, the server list is constructed with “zone” information, as provided in the instance metadata (so, on the remote clients, set `eureka.instance.metadataMap.zone`). If that is missing and if the `approximateZoneFromHostname` flag is set, it can use the domain name from the server hostname as a proxy for the zone. Once the zone information is available, it can be used in a `ServerListFilter`. By default, it is used to locate a server in the same zone as the client, because the default is a `ZonePreferenceServerListFilter`. By default, the zone of the client is determined in the same way as the remote instances (that is, through `eureka.instance.metadataMap.zone`).



### 유레카 없이 리본을 사용하는 방법

유레카는 클라이언트에서 URL을 하드코딩할 필요 없이 원격 서버 검색을 추상화 해 놓은 편리한 라이브러리 입니다. 그러나 유레카를 쓰지 않기로 해도, Ribbon과 Feign을 그대로 사용할 수 있습니다. @RinbbonClient를 선언한 "stores" 서비스가 있고, 유레카는 사용하지 않는다고 가정해 봅시다. 리본 클라이언트는 다음과 같이 서버들의 리스트를 가져 올 수 있는 주소를 입력해줘야 합니다. 

**application.yml**

```yaml
stores:
  ribbon:
    listOfServers: example.com,google.com
```



### 리본에서 유레카 비활성화 하기

`ribbon.eureka.enabled` 속성을 `false` 로 설정하면 명시적으로 리본에서 유레카를 비활성화 할 수 있습니다.

**application.yml**

```yaml
ribbon:
  eureka:
   enabled: false
```



### 리본 API를 직접 사용 

직접적으로 `LoadBalancerClient` 를 사용할 수 있습니다.

```java
public class MyClass {
    @Autowired
    private LoadBalancerClient loadBalancer;

    public void doStuff() {
        ServiceInstance instance = loadBalancer.choose("stores");
        URI storesUri = URI.create(String.format("https://%s:%s", instance.getHost(), instance.getPort()));
        // ... do something with the URI
    }
}
```



### 리본 설정 캐시하기

각각의 리본 네임드 클라이언트는 스프링 클라우트가 관리하는 자식 application Context와 일치합니다. 이 application context는 처음 네임드 클라이언트에게 요청을 할 때, 지연 로딩 됩니다. 지연 로딩 동작은 리본 클라이언트의 이름을 설정해줌으로써  즉시로딩으로 변경할 수 있습니다.

**application.yml**

```yaml
ribbon:
  eager-load:
    enabled: true
    clients: client1, client2, client3
```



### 히스트릭스 스레드 풀 설정하기

`zuul.riboonIsolationStrategy` 의 값을 `THREAD` 로 설정했다면, 히스트릭스는 모든 라우터에 대해 스레드 격리 전략을 사용합니다 이 경우 `HystrixTreadPoolKey` 값이 `RibbonCommand` 로 기본값 설정 됩니다. 이 말은 모든 라우트에 관한 HystrixCommand가 같은 Hystrix 스레드 풀에서 실행된다는 뜻입니다. 이 동작은 다음과 같은 설정으로 변경할 수 있습니다. 

**application.yml**

```yaml
zuul:
  threadPool:
    useSeparateThreadPools: true
```

방금 예제의 결과는 HystrixCommand가 각 라우트 마다 스레드 풀에서 실행 됩니다.

이 경우 , 기본 `HystrixThreadPoolKey` 값은 각 라우트의 서비스 ID와 같습니다. prefix를 추가하고 싶으면 `zuul.threadPool.threadPoolkeyPrefix` 를 설정파일에 추가하면 됩니다. 

**application.yml**

```yaml
zuul:
  threadPool:
    useSeparateThreadPools: true
    threadPoolKeyPrefix: zuulgw
```



### 리본의 IRule에 키를 제공하는 방법

### [7.11. How to Provide a Key to Ribbon’s `IRule`](https://docs.spring.io/spring-cloud-netflix/docs/2.2.5.RELEASE/reference/html/#how-to-provdie-a-key-to-ribbon)

If you need to provide your own `IRule` implementation to handle a special routing requirement like a “canary” test, pass some information to the `choose` method of `IRule`.

com.netflix.loadbalancer.IRule.java

```
public interface IRule{
    public Server choose(Object key);
         :
```

You can provide some information that is used by your `IRule` implementation to choose a target server, as shown in the following example:

```
RequestContext.getCurrentContext()
              .set(FilterConstants.LOAD_BALANCER_KEY, "canary-test");
```

If you put any object into the `RequestContext` with a key of `FilterConstants.LOAD_BALANCER_KEY`, it is passed to the `choose` method of the `IRule` implementation. The code shown in the preceding example must be executed before `RibbonRoutingFilter` is executed. Zuul’s pre filter is the best place to do that. You can access HTTP headers and query parameters through the `RequestContext` in pre filter, so it can be used to determine the `LOAD_BALANCER_KEY` that is passed to Ribbon. If you do not put any value with `LOAD_BALANCER_KEY` in `RequestContext`, null is passed as a parameter of the `choose` method.



## [ 8. External Configuration: Archaius](https://docs.spring.io/spring-cloud-netflix/docs/2.2.5.RELEASE/reference/html/#external-configuration-archaius)

[Archaius](https://github.com/Netflix/archaius) is the Netflix client-side configuration library. It is the library used by all of the Netflix OSS components for configuration. Archaius is an extension of the [Apache Commons Configuration](https://commons.apache.org/proper/commons-configuration) project. It allows updates to configuration by either polling a source for changes or by letting a source push changes to the client. Archaius uses Dynamic<Type>Property classes as handles to properties, as shown in the following example:

Archaius Example

```java
class ArchaiusTest {
    DynamicStringProperty myprop = DynamicPropertyFactory
            .getInstance()
            .getStringProperty("my.prop");

    void doSomething() {
        OtherClass.someMethod(myprop.get());
    }
}
```

Archaius has its own set of configuration files and loading priorities. Spring applications should generally not use Archaius directly, but the need to configure the Netflix tools natively remains. Spring Cloud has a Spring Environment Bridge so that Archaius can read properties from the Spring Environment. This bridge allows Spring Boot projects to use the normal configuration toolchain while letting them configure the Netflix tools as documented (for the most part).

## 라우터와 필터 : Zuul

마이크로서비스 아키텍처에서 필수 입니다. 예를 들어 `/` 경로는 웹 애플리케이션으로 매핑되고, /api/users는 user 서비스로 맵핑하고, /api/shop은 shop 서비스로 매핑할 수 있습니다. Zuul은 JVM기반 라우터이며, 서버 사이드 로드 밸런서 입니다.

넷플릭스에서는 다음과 같이 Zuul을 사용합니다.

- Authentication
- Insights
- Stress Testing
- Canary Testing
- Dynamic Routing
- Service Migration
- Load Shedding
- Security
- Static Response handling
- Active/Active traffic management

Zuul’s rule engine lets rules and filters be written in essentially any JVM language, with built-in support for Java and Groovy.

> The configuration property `zuul.max.host.connections` has been replaced by two new properties, `zuul.host.maxTotalConnections` and `zuul.host.maxPerRouteConnections`, which default to 200 and 20 respectively.

>  The default Hystrix isolation pattern (`ExecutionIsolationStrategy`) for all routes is `SEMAPHORE`. `zuul.ribbonIsolationStrategy` can be changed to `THREAD` if that isolation pattern is preferred.

### Zuul 포함하기

프로젝트에 zuul을 포함하고 싶다면, `org.springframework.cloud:spring-cloud-starter-netflix-zuul` 을 추가하면 됩니다.



### Zuul 리버스 프록시 내장

스프링 클라우드는 UI 애플리케이션(프론트엔드 ex) React, Vue) 이 하나 이상의 백엔드 서비스에 대해 프록시 호출을 원하는 일반적인 사례에서, 개발을 쉽게하기 위해 임베디드 Zuul 프록시를 만들었습니다. 이런 기능은 사용자 인터페이스가 필요한 백엔드 서비스를 프록시하는데 유용하므로 모든 백엔드에 대해 CORS 및 인증 문제를 독립적으로 관리 할 필요가 없습니다.

활성화 하기 위해, @EnableZuulProxyt 어노테이션을 스프링 부트 메인 클래스에 붙여주면 됩니다. 이렇게 함으로써 요청들이 적절한 서비스로 포워드 됩니다. 컨벤션에 따라, 사용자 ID가 있는 서비스는 /users에 있는 프록시에서 요청을 수신합니다. 프록시는 리본을 사용하여 디스커버리를 통해 인스턴스가 어디에 있는지 확인합니다. 모든 요청은 hystrix command 안에서 실행되며, 요청 실패시 Hystrix metrics에 보여지게 됩니다. 서킷 브레이커가 발동되면 프록시는 서비스와 교류하지 않습니다.

> Zull starter 라이브러리는 discovery client를 포함하지 않습니다. 서비스 ID를 기반으로 라우트를 하고 싶다면 유레카 클라이언트 라이브러리를 추가하는 것이 좋습니다.

서비스가 자동으로 추가되는 것을 건너뛰고 싶다면, `zuul.ignored-services` 에 서비스 ID 패턴의 목록을 추가하면 됩니다. 이 값의 패턴과 일치하면 해당 서비스를 무시하고, 명시적으로 routes에 적어준 값들은 포함됩니다.

**application.yml**

```yaml
 zuul:
  ignoredServices: '*'
  routes:
    users: /myusers/**
```

users 서비스는 빼고 다른 서비스는 모두 무시 됩니다. 

프록시 경로를 늘리거나 변경하려면 다음과 같이 외부 구성을 추가 할 수 있습니다.

**application.yml**

```yaml
 zuul:
  routes:
    users: /myusers/**
```

위에 설정은, `/mysuers` 요청들을 users 서비스도 포워딩 됩니다.ex) `/myusers/101` 은 `/101` 로 포워딩 됩니다.

또한 독립적으로 서비스Id와 url을 맵핑 시켜줄수 있습니다.

**application.yml**

```yaml
 zuul:
  routes:
    users:
      path: /myusers/**
      serviceId: users_service
```

위에 예제는 `/myusers` 로 들어오는 요청은 `users_service` 서비스로 포워딩 됩니다. 라우트는 안티스타일 패턴으로 `path` 를 반드시 지정해줘야 하며,  `/myusers/*` 은 오직 1레벨 까지만 일치하는지 검사하고, `/myusers/**` 은 모든 계층을 검사합니다.

백엔드의 위치는 다음과 같이  `serviceId` 나 `url`로 지정해 줄 수 있습니다.

**application.yml**

```yaml
 zuul:
  routes:
    users:
      path: /myusers/**
      url: https://example.com/users_service
```

위에 예제는 HystrixCommand로 실행하지도 않으며, 리본을 이용한 로드밸런싱도 되지 않습니다. 이런 기능들을 사용하고 싶으면 다음과 같이 사용해야 합니다. 

**application.yml**

```yaml
zuul:
  routes:
    echo:
      path: /myusers/**
      serviceId: myusers-service
      stripPrefix: true

hystrix:
  command:
    myusers-service:
      execution:
        isolation:
          thread:
            timeoutInMilliseconds: ...

myusers-service:
  ribbon:
    NIWSServerListClassName: com.netflix.loadbalancer.ConfigurationBasedServerList
    listOfServers: https://example1.com,http://example2.com
    ConnectTimeout: 1000
    ReadTimeout: 3000
    MaxTotalHttpConnections: 500
    MaxConnectionsPerHost: 100
```

다른 방법으로는 다음과 같이 serviceId에 대한 서비스라우트와 리본클라이언트 설정을 해주는 것입니다.(이렇게 할땐, 유레카에서 리본을 사용하지 않아야함)

**application.yml**

```yaml
zuul:
  routes:
    users:
      path: /myusers/**
      serviceId: users

ribbon:
  eureka:
    enabled: false

users:
  ribbon:
    listOfServers: example.com,google.com
```

`regexmapper` serviceId와 라우트 간의 컨벤션을 제공해줄 수 있습니다.It uses regular-expression named groups to extract variables from `serviceId` and inject them into a route pattern, as shown in the following example:

**ApplicationConfiguration.java**

```java
@Bean
public PatternServiceRouteMapper serviceRouteMapper() {
    return new PatternServiceRouteMapper(
        "(?<name>^.+)-(?<version>v.+$)",
        "${version}/${name}");
}
```

위에 예제는 myusers-v1의 serviceId를 `/v1/myusers/**` 로 맵핑하라는 예제 입니다. 모든 정규식이 허용되지만, 모든 네임드 그룹은 servicePattern과 routePattern에 모두 있어야합니다.(?) `servicePattern`이 `serviceId` 와 맞지 않으면, 기본 동작으로 대체됩니다. 이전 예제에서 myusers의 serviceId를 /myusers/**(version은 없음) 라우트로 맵핑 했습니다.  이 기능은 기본적으로 비활성화 되어 있으며, 오직 디스커버리안에 있는 서비스에만 적용이 가능합니다. 

만약 prefix를 `/api` 같은걸 모든 맵핑에 지정하고 싶으면 `zuul.prefix` 값을 설정하면 됩니다. 프록시 prefix는 request가 포워딩 될때 제거됩니다.(zuul.stripPrefix = false로 끌 수 있음.) 또한 개별로 서비스 별 prefix 제거를 끌 수 있습니다.

```yaml
 zuul:
  routes:
    users:
      path: /myusers/**
      stripPrefix: false
```

> `zuul.stripPrefix` 는 `zuul.prefix` 에 설정된 prefix에만 적용됩니다. 지정된 라우트 경로 내에 정의된 prefix에는 영향을 주지 않습니다.



앞선 예제는 `/myusers/101` 로 들어오는 요청은 users 서비스의 `/myusers/101` 로포워딩 됩니다.

`zuul.rotes` 들의 값들은 실제로 ZuulProperties 객체에 바인딩 됩니다. 이 객체를 들여다보면, `retryable` 이라는 flag를 갖고 있습니다. 이 flag를 `ture`로 설정하면, 요청 실패 시 리본 클라이언트가 자동적으로 재시도 합니다. You can also set that flag to true when you need to modify the parameters of the retry operations that use the Ribbon client configuration.

기본적으로 `X-Forwarded-Host` 헤더는 포워딩 될 요청에 추가됩니다. 끄고 싶다면, `zuul.addProxyHeaders = false`로 설정하면 됩니다.   기본적으로 prefix는 벗겨지며 `X-Forwarded-Prefix` 헤더에 담깁니다.

기본 라우트를 `/` 로 설정하면 `@EnableZuulProxy` 가 있는 애플리케이션은 독립적인 서버로 작동합니다. 예를 들어 `zuul.route.home : / ` 으로 설정하면 모든 트래픽이 "home" 서비스로 라우트 됩니다.

좀 더 상세한 설정을 필요로 한다면, 특정 패턴을 무시하도록 명시해줄 수 있습니다.

**application.yml**

```yaml
 zuul:
  ignoredPatterns: /**/admin/**
  routes:
    users: /myusers/**
```

위에 예제는 `/myusers/101` 같은 요청을 `users` 서비스의 `/101` 로 보내지만, 중간에 `/admin/`  이 있다면 무시합니다.

> 순서를 보장할 수 있게 작성하고 싶다면, properties 파일 대신 YAML 파일을 사용해야 합니다.

**application.yml**

```yaml
 zuul:
  routes:
    users:
      path: /myusers/**
    legacy:
      path: /**
```

If you were to use a properties file, the `legacy` path might end up in front of the `users` path, rendering the `users` path unreachable.

### Zuul Http 클라이언트

The default HTTP client used by Zuul is now backed by the Apache HTTP Client instead of the deprecated Ribbon `RestClient`. `RestClient` 나 `okhttp3.OkHttpClient` 를 사용하고 싶다면, `ribbon.restclient.enabled=true` 나 `ribbon.okhttp.enabled=true` 로 설정하면 됩니다. Apache HTTP client나 OK HTTP client를 커스터 마이징 하고 싶다면, `CloseableHttpClient` 나 `OkHttpClient` 빈을 제공하면 됩니다.

### 쿠키와 민감한 헤더 정보

같은 시스템 내에 있는 서비스들 끼리는 헤더를 공유할 수 있습니다. 그러나 외부 서버로 민감한 헤더 정보를 보내고 싶지 않을 때가 있습니다. 이를 설정하기 위해 라우트 설정에서 보내고 싶지 않는 헤더의 리스르를 작성할 수 있습니다. 쿠키는 브라우저에서 의미를 갖고 있고, 항상 민감한 것으로 취읍되기 때문에 특별합니다. 프록시의 소비자가 브라우저인 경우 다운 스트림 서비스에 대한 쿠키는 모두 함께 뒤죽박죽이 되기 때문에 사용자에게 문제를 일으킵니다. ( 모든 다운 스트림 서비스는 같은 곳에서 온 것처럼 보이기 때문에)

서비스의 디자인에 주위를 기울였다면,(예를 들어 하나의 다운스트림 서비스에만 쿠키를 적용한 경우) 백엔드에서 호출자까지 서비스가 흐르도록 할 수 있습니다. 또한 프록시가 쿠키를 설정하고 모든 백엔드 서비스가 같은 시스템의 부분이라면, 자연스럽게 쿠키가 공유가 가능합니다.(예를 들어, Spring Session을 사용하여 쿠키를? 묶어 상태를 공유할 수 있습니다.) 또 다른점은, 다운스트림 서비스에서 가져오거나 설정하는 쿠키는 호출하는 사람에게 유용하지 않을 수 있습니다. 그러므로 Set-Cookie 및 Cookie를 도메인의 일부가 아닌 경로에 대해 민감한 헤더로 만드는 것을 추천합니다. 라우트가 도메인의 일부일지여도, 쿠키와 프록시 사이의 흐름을 허락해준다는 뜻이 어떤 의미인지를 신중히 생각해봐야 합니다. 

민감한 헤더는 라우트 마다 콤마로 구별하여 설정해줄 수 있습니다.

**application.yml**

```yaml
 zuul:
  routes:
    users:
      path: /myusers/**
      sensitiveHeaders: Cookie,Set-Cookie,Authorization
      url: https://downstream
```

>  `sensitiveHeaders` 의 값은 기본 값이기 때문에 따로 설정해주지 않아도 적용 됩니다. This is new in Spring Cloud Netflix 1.1 (in 1.0, the user had no control over headers, and all cookies flowed in both directions).

`sensitiveHeaders` 는 블랙리스트이며, 기본값이 존재합니다, 결과적으로 Zuul이 모든 헤더(`ignored` 에설정된 값 빼고)를 보내고 싶다면, 명시적으로 빈 리스트로 설정해야 합니다.  쿠키나 , 인증 헤더를 백엔드로 보내고 싶다면 필수적으로 해야합니다.

**application.yml**

```yaml
 zuul:
  routes:
    users:
      path: /myusers/**
      sensitiveHeaders:
      url: https://downstream
```



### 헤더 무시하기

추가적으로 다운스트림 서비스와 상호작용하는 동안 `zuul.ignoredHeaders` 에 있는 값을 버리게 할 수 있습니다. (요청 응답 둘다) 기본적으로 Spring Security가 클래스패스에 없으면 `zuul.ignoredHeaders` 값들은 비어 있지만, 그렇지 않으면 Spring Security에 명시된 "security"로 알려진 헤더로 초기화 됩니다. 이런 경우 다운스트림 서비스가 이런 헤더들을 추가할 수 있습니다. 그러나 프록시로부터 오는 값들을 원할 때가 있습니다. Spring Security가 클래스패스에 있을 때, 이런 헤더들을 버리고 싶지 않으면, `zuul.ignoreSecurityHeaders` 값을 `false` 로 설정하면 됩니다. 이렇게 함으로써 Spring Security에서 HTTP 보안 응답 헤더를 비활성화 하고 다운 스트림 서비스에서 제공하는 값을 원하는 경우 유용할 수 있습니다.

### 엔드포인트 관리하기

기본적으로 `@EnableZuulProxy` 를 Sring Boot Actuator와 함께 사용한다면, 다음의 2개의 엔드포인트가 활성화가 됩니다.

- Routes
- Filters

#### 라우트 엔드포인트

GET 방식의 `/routes` 엔드포인트는 라우트에 맵핑된 리스트의 목록을 반환합니다.

**GET /routes**

```json
{
  /stores/**: "http://localhost:8081"
}
```

추가적으로 라우트에 대한 상세정보는 `?format=details` 쿼리를 추가적으로 붙이면 볼 수 있습니다.

**GET /routes/details**

```json
{
  "/stores/**": {
    "id": "stores",
    "fullPath": "/stores/**",
    "location": "http://localhost:8081",
    "path": "/**",
    "prefix": "/stores",
    "retryable": false,
    "customSensitiveHeaders": false,
    "prefixStripped": true
  }
}
```

POST 방식의 `/routes` 를 호출하면 라우트를 새로고침 합니다.  (for example, when there have been changes in the service catalog). You can disable this endpoint by setting `endpoints.routes.enabled` to `false`.

> the routes should respond automatically to changes in the service catalog, but the `POST` to `/routes` is a way to force the change to happen immediately.

### 필터 엔드포인트

GET 방식의 `/filters` 엔드포인트는 type에 따라 Zuul filter의 목록을 map으로 반환 합니다.



## Strangulation Patterns and Local Forwards(교살 패턴과 로컬 포워드)

애플리케이션을 마이그레이션하거나, 오래된 API 엔드포인트 때문에 힘이 들때, 천천히 다른 구현으로 바꿀때 사용하는 패턴중의 하나가 교살패턴 입니다. Zuul 프록시는 







## 참고자료

https://docs.spring.io/spring-cloud-netflix/docs/2.2.5.RELEASE/reference/html

