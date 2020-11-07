package donghyeon.dev.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;

@EnableResourceServer
@Configuration
public class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {
    /**
     * OAuth 토큰에 의해 보호되는 리소스(자원) 서버 설정
     * 리소스에 대한 접근 권한을 부여
     */

    // TODO
}
