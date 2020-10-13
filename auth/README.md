## 인증서버



### 나만의 인증서버가 필요한 이유

- 로그인,로그아웃,패스워드 복구의 동작들을 내가 관리하고 서비스를 분리하고 싶을 때,
- 다른 분리된 서비스들에게 Oauth2.0 프로토콜을 사용하고 싶을 때



### 의존성

`spring-security-oauth2` 를 추가하면, Oauth2.0과 `spring-security-oauth2-autoconfigure` 를 사용할 수 있습니다. 

JWT를 사용하고 싶으면 `spring-security-jwt` 를 추가하면 됩니다.



### Oauth2 최소 설정

1. 의존성 추가
2. `@EnableAuthorizationServer` 어노테이션 추가
3. 적어도 한개의 Client Id와 secret 추가

#### 인증서버 활성화 하기 

Spring Boot의 `@Enable` 어노테이션ㅇ과 비슷하게 메인 메소드에 `@EnableAuthorizationServer` 어노테이션을 추가하면 됩니다.

```java
@EnableAuthorizationServer
@SpringBootApplication
public class SimpleAuthorizationServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SimpleAuthorizationServerApplication, args);
    }
}
```

이 어노테이션을 추가하면 토큰 서명, 유지시간, 권한 같은 기본값 설정 파일이 추가 됩니다.

#### Client와 Secret 설정

스펙에 따르면, 많은 Oauth 2.0 엔드포인트는 클라이언트 인증을 요구합니다. 다른사람이 인증서버와 커뮤니케이트 할 수 있도록 적어도 한개의 client를 명시해줘야 합니다.

```yaml
security:
  oauth2:
    client:
      client-id: first-client
      client-secret: noonewilleverguess
```

#### 토큰 발급받기

Oauth 2.0은 수명이 짧은 토큰과 수명이 긴 토큰을 교환하기 위한 방법을 지정하는 프레임워크 입니다.

기본적으로 `@EnableAuthorizationServer` 은 client 인증방식으로 클라이언트가 접근할 수 있께 해줍니다.

```
curl first-client:noonewilleverguess@localhost:8080/oauth/token -dgrant_type=client_credentials -dscope=any
```

애플리케이션은 다음과 같은 응답을 리턴해줍니다.

```json
{
    "access_token" : "f05a1ea7-4c80-4583-a123-dc7a99415588",
    "token_type" : "bearer",
    "expires_in" : 43173,
    "scope" : "any"
}
```

이 토큰은 리소스 서버에 접근하기 위한 인증으로 사용됩니다.



### Oauth2 Auto Configuration 끄기

기본적으로 Oauth2 부트 프로젝트는 [`AuthorizationServerConfigurer`](https://projects.spring.io/spring-security-oauth/docs/oauth2.html#authorization-server-configuration) 을 다음과 같은 기본설정과 함께 인스턴스를 만듭니다.

- `NoOpPasswordEncoder` 을 등록한다.(overriding [the Spring Security default](https://docs.spring.io/spring-security/site/docs/current/reference/htmlsingle/#core-services-password-encoding))
- 다음과 같은 인증이 기본적으로 설정됩니다. : authorization_code , password, client_credentials, implicit , refresh_token

그렇지 않으면 다음의 bean들을 가집니다. 

- `AuthenticationManager` : For looking up end users (not clients)
- `TokenStore` : 토큰 생성
- `AccessTokenConverter` : JWT와 같이 토큰을 다른 포맷으로 변환해줄 때 사용

[spring-seucirty-oauth-primitive](https://projects.spring.io/spring-security-oauth/docs/oauth2.html)

만약 `AuthorizationServerConfigurer` 빈이 있다면 아무것도 수행되지 않습니다.(?)

예를 들어, 하나 이상의 클라이언트를 설정하거나, 인증 타입을 변경하거나, no-op-password 인코더보다 더 좋은걸로 변경하고자 할 때, `AuthorizationServerConfigurer` 을 커스텀한 bean을 등록해줘야 합니다.

```java
@Configuration
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

    @Autowired DataSource dataSource;

    protected void configure(ClientDetailsServiceConfigurer clients) {
        clients
            .jdbc(this.dataSource)
            .passwordEncoder(PasswordEncoderFactories.createDelegatingPasswordEncoder());
    }
}
```

The preceding configuration causes OAuth2 Boot to no longer retrieve the client from environment properties and now falls back to the Spring Security password encoder default.

### How to Make Authorization Code Grant Flow Work

기본설정으로 인증코드 방식이 가능하지만, 조금 더 설정해줘야 합니다.

기본 설정 이외에도 추가적인 설정이 필요하기 때문입니다.

- End users
- An end-user login flow, and
- A redirect URI registered with the client

#### End user 추가하기

Spring Security로 보안된 전형적인 스프링 부트 애플리케이션에서는 user는 `UserDetailsService` 에 정의 됩니다. 인증서버도 다른점이 없습니다.

```java
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Bean
    @Override
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager(
            User.withDefaultPasswordEncoder()
                .username("enduser")
                .password("password")
                .roles("USER")
                .build());
    }
}
```

전형적인 Spring Security 웹 애플리케이션에는 user는 `WebSecurityConfigurerAdapter` 에 정의합니다.

#### 엔드 유저 로그인 Flow 추가하기

`WebSeucirtConfigurerAdapter` 클래스만 있어도, 엔드유저가 form 로그인에 관한 설정을 하는데 충분합니다. 그러나 이 클래스는 OAuth2 말고도 다른 웹 관련 설정을 할 수도 있습니다.

로그인 페이지를 커스터마이징하거나, 로그인 폼 이외에 더 제공하고 싶거나, 패스워드 찾기 같은 추가적인 동작을 추가하고 싶다면, `WebSeucirtConfigurerAdapter` 에서 하면 됩니다.

#### Redirect URI 추가하기

Oauth2 Boot는 프로퍼티로 Redriect URI를 설정할 수 없습니다. `client-id` 와 `client-secret` 과 함께 작성해야 합니다.

Redirect URI를 추가하기 위해서는 `InMemoryClientDetailsService` 또는 `JdbcClientDetailsService` 를 이용하여 client를 명시해줘야 합니다. 

이렇게 하기 위해서는 다음과 같이 자신만의 `AuthorizationServerConfigurer` 를 만들어야한다는 걸 뜻합니다.

```java
@Configuration
public class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    protected void configure(ClientDetailsServiceConfigurer clients) {
        clients
            .inMemory()
                .withClient("first-client")
                .secret(passwordEncoder().encode("noonewilleverguess"))
                .scopes("resource:read")
                .authorizedGrantTypes("authorization_code")
                .redirectUris("http://localhost:8081/oauth/login/client-app");
    }
}
```

#### 인증 코드 Flow 테스트하기

OAuth 테스트는 전체 흐름을 확인하기 위해 둘 이상의 서버가 필요하기 때문에 까다로울 수 있습니다. However, the first steps are straight-forward:

1. Browse to http://localhost:8080/oauth/authorize?grant_type=authorization_code&response_type=code&client_id=first-client&state=1234
2. The application, if the user is not logged in, redirects to the login page, at http://localhost:8080/login
3. Once the user logs in, the application generates a code and redirects to the registered redirect URI — in this case, http://localhost:8081/oauth/login/client-app

The flow could continue at this point by standing up any resource server that is configured for opaque tokens and is pointed at this authorization server instance.



### 비밀번호 인증방식 흐름

기본 설정으로 Password 인증방식은 사용가능하지만, 인증코드 방식처럼 users가 없기 때문에 추가해줘야 합니다.

기본 설정으로는 username을 user로 하고 passowrd는 랜덤으로 하면 토큰을 발급 해줍니다.

```
curl first-client:noonewilleverguess@localhost:8080/oauth/token -dgrant_type=password -dscope=any -dusername=user -dpassword=the-password-from-the-logs
```

유저를 추가하는 방법은 다음과 같습니다.

```java
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Bean
    @Override
    public UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager(
            User.withDefaultPasswordEncoder()
                .username("enduser")
                .password("password")
                .roles("USER")
                .build());
    }
}
```

This is all we need to do. We do not need to override `AuthorizationServerConfigurer`, because the client ID and secret are specified as environment properties.

So, the following should now work:

```bash
curl first-client:noonewilleverguess@localhost:8080/oauth/token -dgrant_type=password -dscope=any -dusername=enduser -dpassword=password
```

### 언제 어떻게 인증서버에 AuthenticationManager를 줘야할까?

이 질문은 흔한질문이며, `AuthorizationServerEndpointsConfigurer` 가 `AuthentifcationManager` 를 지정할때 직관적이지 않습니다. 

이 질문에 정답은 Resource Owner Password Flow를 이용할 때 합니다.                                     

It helps to remember a few fundamentals:

- `AuthenticationManager` 는 유저 인증에 대한 추상화 입니다. 일반적으로 인증 하기 위해  `UserDetailService` 있어야 합니다.
- 엔드 유저는 `WebSecurityConfigurerAdapter` 안에 명시되어야 합니다.
- Oauth2에서 기본적으로 자동으로 아무 `AuthenticationManager`  빈으로 씁니다.

그러나 모든 인증방식에는 `AuthenticationManager` 가 필요하지 않습니다. 왜냐하면 어떤 인증방식은 user 가 필요없기 때문이죠. 예를 들어 클라이언트 인증 방식은 유저의 권한이 아니라 클라이언트 권한에 따라 토큰을 발급해줍니다. 그리고 Refresh 토큰 방식 또한 리프래쉬 할 토큰의 권한에 따라 토큰을 발급 해주기 때문입니다.

또한 모든 인증방식에 Oauth 2.0API 자체에 AuthenticationMAnager가 있어야 하는 것은 아닙니다. 예를 들어 인증코드 및 Implicit 방식은 토큰이(Oauth 2.0API) 요청 될때가 아니라 로그인 할 때(애플리케이션 흐름) 사용자를 확인합니다.

오직 Resource Owner Password 방식만 유저 인증방식을 사용합니다. 이 말은 클라이언트가 Resource Owner Passowrd flow를 사용할 때 인증서버에 AuthenticationManager를 사용한다는 뜻 입니ek.

다음은 Resource Owner Password 방식의 예제입니다.

```java
.authorizedGrantTypes("password", ...)
```

In the preceding flow, your Authorization Server needs an instance of `AuthenticationManager`.

다음의 몇가지 방법이 있습니다.

- 그냥 기본 설정을 사용하고(`AuthorizationServerConfigurer` 를 만들지 않음) UserDetailsService 만들기
- 기본 설정을 사용하고, `AuthenticationManager` 만들기
- `AuthorizationServerConfigurer` 를 오버라이딩 하고(기본설정 제거) `AuthenticationConfiguration` 주입하기
- `AuthorizationServerConfigurer` 을 오버라이드 하고 수동으로 `AuthenticationManager` 연결해주기

####  1안. UserDetailsService 만들기

엔드유저는 `WebSecurityConfigurerAdapter`  안에 `UserDetailsService` 에 지정됩니다. 만약 Oauth2 Boot의 디폴트 설정(``AuthorizationServerConfigurer`` 구현하지 않음)을 사용하고 싶다면, UserDetailsService를 다음과 같이 빈으로 만들어주면 됩니다.

```java
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired DataSource dataSource;

    @Bean
    @Override
    public UserDetailsService userDetailsService() {
        return new JdbcUserDetailsManager(this.dataSource);
    }
}
```

#### 2안. AuthenticationManager 만들기

AuthenticationManager 를 좀 더 상세하게 사용할 필요가 있는 경우 `WebSecurityConfigurerAdapter` 클래스 안에 작성하여 빈으로 노출시키면 됩니다.

```java
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Bean(BeansId.AUTHENTICATION_MANAGER)
    @Override
    public AuthenticationManager authenticationManagerBean() {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(customAuthenticationProvider());
    }
}
```



#### 3안. Authentication에 의존

설정된 `AuthenticationManager` 는 `AuthenticationConfiguration` 에서 사용 가능합니다. 이 말은 즉슨, `AuthorizationServerConfigurer`가 필요한 경우 (이 경우 @Autowired 사용해야함 ) 아래 예제 처럼`AuthenticationConfiguration`에 의존하여 `AuthenticationManager` 빈을 가져올 수 있습니다.

```java
@Component
public class CustomAuthorizationServerConfigurer extends
    AuthorizationServerConfigurerAdapter {

    AuthenticationManager authenticationManager;

    public CustomAuthorizationServerConfigurer(AuthenticationConfiguration authenticationConfiguration) {
        this.authenticationManager = authenticationConfiguration.getAuthenticationManager();
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) {
        // .. your client configuration that allows the password grant
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) {
        endpoints.authenticationManager(authenticationManager);
    }
}
```

```java
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Bean
    @Override
    public UserDetailsService userDetailsService() {
        return new MyCustomUserDetailsService();
    }
}
```

#### 4안. Manually Wiring An `AuthenticationManager`

In the most sophisticated case, where the `AuthenticationManager` needs special configuration and you have your own `AuthenticationServerConfigurer`, then you need to both create your own `AuthorizationServerConfigurerAdapter` and your own `WebSecurityConfigurerAdapter`:

```java
@Component
public class CustomAuthorizationServerConfigurer extends
    AuthorizationServerConfigurerAdapter {

    AuthenticationManager authenticationManager;

    public CustomAuthorizationServerConfigurer(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Override
    public void configure(ClientDetailsServiceConfigurer clients) {
        // .. your client configuration that allows the password grant
    }

    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) {
        endpoints.authenticationManager(authenticationManager);
    }
}
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Bean(BeansId.AUTHENTICATION_MANAGER)
    @Override
    public AuthenticationManager authenticationManagerBean() {
        return super.authenticationManagerBean();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        auth.authenticationProvider(customAuthenticationProvider());
    }
}
```





## 리소스 서버

Spring Security Oauth2 Boot는 두개의 포맷인 JWT와 Opaque를 이용한 Bearer Token 인증 방식을 사용해 리소스를 간단하게 보호할 수 있습니다.



### 의존성

`spring-security-oauth2` 를 추가하면, Oauth2.0과 `spring-security-oauth2-autoconfigure` 를 사용할 수 있습니다. 

JWT를 사용하고 싶으면 `spring-security-jwt` 를 추가하면 됩니다.



###  Oauth2 Boot 최소 설정

1. 의존성을 추가한다
2. `@EnableResourceServer` 어노테이션을 추가한다
3. 토큰 검증 할 로직을 명시해준다 

#### 리소스 서버 활성화 하기

스프링 메인 클래스에 `@EnableResourceServer` 어노테이션을 다음과 같이 추가하면 됩니다.

```java
@EnableResourceServer
@SpringBootApplication
public class SimpleAuthorizationServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(SimpleAuthorizationServerApplication, args);
    }
}
```

이 어노테이션을 추가하면 `OAuth2AuthenticationProcessingFilter` 가 추가됩니다. 그래서 어떻게 적절히 토큰을 처리하고 검증할지에 대한 설정이 필요합니다.

#### 토큰 검증 로직 명시

Bearer 토큰은 일반적으로 JWT-encoded 와 opaque 두개의 형식이 있습니다. You will need to configure the resource server with one or the other strategy.

**JWT**

JWT를 사용하려면 인증서버에서 호스팅되는 JWK  set uri을 지정하면 됩니다.

```yaml
security:
  oauth2:
    resource:
      jwk:
        key-set-uri: https://idp.example.com/.well-known/jwks.json
```

JWK Set uri 대신에 key를 지정해 줄 수 있습니다.

이 설정은 인증서버가 실행되고 있어야만 합니다.



**Opaque**

opaque를 사용하려면 어떻게 토큰을 decode 할지 인증서버에 엔드포인트를 지정해줘야 합니다.

```yaml
security:
  oauth2:
    resource:
      token-info-uri: https://idp.example.com/oauth2/introspect
```

>  It’s likely this endpoint requires some kind of authorization separate from the token itself, for example, client authentication.



#### 리소스에 접근하기

리소스 서버가 올바르게 토큰을 처리하고 있는지 확인하기 위해서는 다음과 같이 간단한 컨트롤러 엔드포인트를 만들면 됩니다.

```java
@RestController
public class SimpleController {
	@GetMapping("/whoami")
	public String whoami(@AuthenticationPrincipal(expression="name") String name) {
		return name;
    }
}
```

그 다음 인증서버에서 토큰을 받아, 리소스 서버에 접근하면 됩니다.

```bash
curl -H "Authorization: $TOKEN" http://localhost:8080/whoami
```

And you should see the value of the `user_name` attribute in the token.



### Single Key를 이용한 JWT

JWK Set 엔드포인트 대신, 인증을 위한 로컬 키를 사용할 수도 있습니다. 이 방법은 key가 고정적이기 때문에 JWK 보다 보안에 취약할 수 있지만, 특정 상황에서는 필요할 수 도 있습니다.

다음과 같이 키를 설정해주면 됩니다.

```yaml
security:
  oauth2:
    resource:
      jwt:
        key-value: |
          -----BEGIN PUBLIC KEY-----
          MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKC...
          -----END PUBLIC KEY-----
```

>  The pipe in yaml indicates a multi-line property value.

key-value 대신 key-store , key-store-password , key-alias , key-password 프로퍼티로 제공해줄 수 있습니다.

아니면 key-uri 엔드포인트를 애용하여 인증서버에서 key를 가져올 수 있습니다.



### Token 정보 엔드포인트 설정하기 

일반적으로 `SecurityContext` 에 있는 bearer 토큰은 user와 연관이 있기 때문에 클라이언트 인증 방식에서 토큰 정보 엔드포인트가 필요할 수 있습니다.

```yaml
security:
  oauth2:
    client:
      clientId: client-id
      clientSecret: client-secret
    resource:
      tokenInfoUri: https://idp.example.com/oauth2/check_token
```

By default, this will use Basic authentication, using the configured credentials, to authenticate against the token info endpoint.

### 유저 정보 엔드포인트 설정하기

기본적으로 리소스 서버는 요청을 인증하는 것이 아니라, 권한을 부여하는 것이기 때문에 유저 정보 엔드포인트를 호출하는 것은 일반적인 사례가 아닙니다.

유저 정보 엔드포인트는 다음과 같이 지정할 수 있습니다.

```yaml
security:
  oauth2:
    resource:
      userInfoUri: https://idp.example.com/oauth2/userinfo
```

리소스 서버는 Bearer 토큰을 전송하고 결과로 Authentication 객체를 받습니다.

#### 유저 정보 요청 커스터마이징 하기

내부적으로 리소스 서버는 `/userinfo` 를 호출하기 위해 `Oauth2RestTemplate` 을 사용합니다. 그 때 호출을 하기 위해 필터를 추가하거나 다른 로직을 수행이 필요할 수 있습니다. 커스터마이징을 하려면 다음과 같이 `UserInfoRestTemplateCustomizer` bean을 만들면 됩니다.

```java
@Bean
public UserInfoRestTemplateCustomizer customHeader() {
	return restTemplate ->
			restTemplate.getInterceptors().add(new MyCustomInterceptor());
}
```

이 bean은 `UserInfoTemplateFactory` 클래스에 넘겨지며 `/userinfo` 엔드포인트에 대해 설정을 합니다.

### 권한 역할 커스터마이징

Spring Seucirty와 비슷하게, 엔드포인트에대한 권한 역할을 커스터마이징 할 수 있습니다.

```java
public class HasAuthorityConfig
		extends ResourceServerConfigurerAdapter {

	@Override
	public void configure(HttpSecurity http) throws Exception {
		// @formatter:off
		http
			.authorizeRequests()
				.antMatchers("/flights/**").hasAuthority("#oauth2.hasScope('message:read')")
				.anyRequest().authenticated();
		// @formatter:on
	}
```

Though, note that if a server is configured both as a resource server and as an authorization server, then there are certain endpoint that require special handling. To avoid configuring over the top of those endpoints (like `/token`), it would be better to isolate your resource server endpoints to a targeted directory like so:

```java
public class ResourceServerEndpointConfig
		extends ResourceServerConfigurerAdapter {

	@Override
	public void configure(HttpSecurity http) throws Exception {
		// @formatter:off
		http
			.antMatchers("/resourceA/**", "/resourceB/**")
			.authorizeRequests()
				.antMatchers("/resourceA/**").hasAuthority("#oauth2.hasScope('resourceA:read')")
				.antMatchers("/resourceB/**").hasAuthority("#oauth2.hasScope('resourceB:read')")
				.anyRequest().authenticated();
		// @formatter:on
	}
```

As the above configuration will target your resource endpoints and not affect authorization server-specific endpoints.



### 덜 쓰는 기능 들

#### Token 타입 변경하기

구글과 다른 서드파티 업체들은 사용자 정보 엔드포인트로 전송되는 토큰의 이름을 엄격하게 다룹니다. 기본적으로는 `Bearer` 이름이 붙지만, `security.oauth2.resource.token-type` 설정으로 변경이 가능합니다

#### Filter 순서 변경하기

OAuth2는 `security.oauth2.resource.filter-order` 에 명시된 필터 체인 순서대로 리소스를 보호합니다. 

기본으로 필터는 `AuthorizationServerConfigurereAdapter` 가 제일 먼저오고, 그 다음 `ResourceServerConfigurerAdapter` 그 다음 `WebSecurityConfigurerAdapter` 가 옵니다.

이 말은 모든 애플리케이션 엔드포인트가 다음의 두가지를 만족하지 않으면 bearer 토큰 인증이 필요하다는 뜻 입니다.

1. 필터 순서가 바뀌거나 
2. The `ResourceServerConfigurerAdapter` set of authorized requests is narrowed

첫번 째는 필터 순서를 바꾸는 건데, WebSecurityConfigurerAdapter를 ResourceServerConfigurerAdapter 앞에 다음과 같이 이동하여 수행 할 수 있습니다.

```java
@Order(2)
@EnableWebSecurity
public WebSecurityConfig extends WebSecurityConfigurerAdapter {
	// ...
}
```

|      | Resource Server’s default `@Order` value is 3 which is why the example sets Web’s `@Order` to 2, so that it’s evaluated earlier. |
| ---- | ------------------------------------------------------------ |
|      |                                                              |

While this may work, it’s a little odd since we may simply trade one problem:

> `ResourceServerConfigurerAdapter` is handling requests it shouldn’t

For another:

> `WebSecurityConfigurerAdapter` is handling requests it shouldn’t

The more robust solution, then, is to indicate to `ResourceServerConfigurerAdapter` which endpoints should be secured by bearer token authentication.

For example, the following configures Resource Server to secure the web application endpoints that begin with `/rest`:

```java
@EnableResourceServer
public ResourceServerConfig extends ResourceServerConfigurerAdapter {
	@Override
    protected void configure(HttpSecurity http) {
        http
            .requestMatchers()
                .antMatchers("/rest/**")
            .authorizeRequests()
                .anyRequest().authenticated();
    }
}
```



#### /error 엔드포인트 허가하기

리소스 서버는 인증 프로세스 로직을 탈 때 `Oauth2ClientContext` <u>request-scoped</u> 빈에 의존 합니다. 그리고 에러 상황이 발생하면, 리소스 서버는 ERROR 서블릿 디스패처로 포워드 합니다.

기본적으로 <u>request-scoped</u> 빈은 ERROR 디스패치가 사용하지 못합니다. 그러므로 `Oauth2ClientContext` bean not being available. 이라는 메세지를 보게 됩니다.

가장 간단한 방법은 `/error` 엔드포인트를 허가해주어 리소스 서버가 인증을 시도하거나 요청을 하지 않게 하는 방법입니다.

```java
public class PermitErrorConfig extends ResourceServerConfigurerAdapter {
    @Override
	public void configure(HttpSecurity http) throws Exception {
		// @formatter:off
		http
			.authorizeRequests()
				.antMatchers("/error").permitAll()
				.anyRequest().authenticated();
		// @formatter:on
	}
}
```

Other solutions are to configure Spring so that the `RequestContextFilter` is registered with the error dispatch or to register a `RequestContextListener` bean.



## 클라이언트

웹 애플리케이션을 OAuth2 클라이언트로 만들고 싶으면, @EnableOAuth2Client 어노테이션을 추가하고, `OAuth2RestOperations` 의 필수 클래스인 `OAuth2ClientContext` 와 `OAuth2ProtectedResourceDetails` 를 만들어야 합니다. 스프링 부트는 이 두개의 클래스를 자동으로 빈으로 만들어 주지 않기 때문에, 다음과 같이 만들어야 합니다.

```java
@Bean
public OAuth2RestTemplate oauth2RestTemplate(OAuth2ClientContext oauth2ClientContext,
        OAuth2ProtectedResourceDetails details) {
    return new OAuth2RestTemplate(details, oauth2ClientContext);
}
```

>  You may want to add a qualifier and review your configuration, as more than one `RestTemplate` may be defined in your application.

아래의 설정은 `security.oauth2.client.*` 의 값을 인증정보로 사용합니다. 그러나 추가적으로 인증서버의 토큰 url을 알아야 할 수도 있습니다. 

**application.yml**

```yaml
security:
  oauth2:
    client:
      clientId: bd1c0a783ccdd1c9b9e4
      clientSecret: 1a9030fbca47a5b2c28e92f19050bb77824b5ad1
      accessTokenUri: https://github.com/login/oauth/access_token
      userAuthorizationUri: https://github.com/login/oauth/authorize
      clientAuthenticationScheme: form
```

이 설정은 `OAuth2RestTemplate` 을 사용할 때 깃헙에 인증을 받기위해 리다이렉트 합니다. 

클라이언트가 액세스 토큰을 얻기 위한 요청의 범위를 제한하려면 `security.oauth2.client.scope` 로 설정할 수 있습니다. 기본적으로 범위 값은 비어 있지만, 인증서버가 기본 값을 설정하기 나름 입니다.(usually depending on the settings in the client registration that it holds).

>  There is also a setting for `security.oauth2.client.client-authentication-scheme`, which defaults to `header` (but you might need to set it to `form` if, like Github for instance, your OAuth2 provider does not like header authentication). In fact, the `security.oauth2.client.*` properties are bound to an instance of `AuthorizationCodeResourceDetails`, so all of its properties can be specified.

>  In a non-web application, you can still create an `OAuth2RestOperations`, and it is still wired into the `security.oauth2.client.*` configuration. In this case, you are asking for is a “client credentials token grant” if you use it (and there is no need to use `@EnableOAuth2Client` or `@EnableOAuth2Sso`). To prevent that infrastructure being defined, remove the `security.oauth2.client.client-id` from your configuration (or make it be an empty string).



## Single Sign on

OAuth2 클라이언트를 사용하여 프로바이더에서 사용자 세부 정보를 가져온 다음 Spring Security 용 인증 토큰으로 변환 할 수 있습니다. 리소스 서버는 `user-info-uri` 속성을 통해 SSO를 지원합니다.  이것은 OAuth2를 기반한 SSO 프로토콜의 기초이며, 스프링 부트는 `@EnableOauth2Sso` 를 제공하여 쉽게 제공해줍니다. 방금 보여드린 깃헙 클라이언트는 모든 리소스가 보호되고 있으며 깃헙의 `/user/` 엔드포인트를 이용해 인증을 받습니다. (The Github client shown in the preceding section can protect all its resources and authenticate by using the Github `/user/` endpoint, by adding that annotation and declaring where to find the endpoint (in addition to the `security.oauth2.client.*` configuration already listed earlier):)

**예제 1 : application.yml**

```yaml
security:
  oauth2:
# ...
  resource:
    userInfoUri: https://api.github.com/user
    preferTokenInfo: false
```

기본적으로 모든 경로는 보호되고 있기 때문에, 인증받지 않은 유저에게 보여줄 home 페이지는 존재하지 않으며 로그인 하도록 초대해야 합니다(?)(`/login` 경로를 방문하거나, `security.oauth2.sso.login-path` 에 지정된 경로)

보호할 접근 규칙이나 경로를 커스터마이징 하려면 



To customize the access rules or paths to protect s(o you can add a “home” page for instance,) you can add `@EnableOAuth2Sso` to a `WebSecurityConfigurerAdapter`. The annotation causes it to be decorated and enhanced with the necessary pieces to get the `/login` path working. In the following example, we simply allow unauthenticated access to the home page at `/` and keep the default for everything else:

```java
@Configuration
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .mvcMatchers("/").permitAll()
                .anyRequest().authenticated();
    }
}
```

Also, note that, since all endpoints are secure by default, this includes any default error handling endpoints — for example, the `/error` endpoint. This means that, if there is some problem during Single Sign On that requires the application to redirect to the `/error` page, this can cause an infinite redirect between the identity provider and the receiving application.

First, think carefully about making an endpoint insecure, as you may find that the behavior is simply evidence of a different problem. However, this behavior can be addressed by configuring the application to permit `/error`, as the following example shows:

```java
@Configuration
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .authorizeRequests()
                .antMatchers("/error").permitAll()
                .anyRequest().authenticated();
    }
}
```





## 참고자료

https://docs.spring.io/spring-security-oauth2-boot/docs/current/reference/html5/#oauth2-boot-authorization-server-password-grant-authentication-configuration