package donghyeon.dev.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;

@RequiredArgsConstructor
@EnableAuthorizationServer
@Configuration
public class AuthorizationServerConfiguration extends AuthorizationServerConfigurerAdapter {
    /**
     * 인증 서버 설정
     * AuthorizationServerConfigurer의 구현체를 상속받아 구현
     * 인증 및 endPoints를 설정하고 JWT 토큰을 발급함
     */

    // TODO
}
