//package org.jeecg.config.security;
//
//import com.nimbusds.jose.jwk.Curve;
//import com.nimbusds.jose.jwk.ECKey;
//import com.nimbusds.jose.jwk.JWKSet;
//import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
//import com.nimbusds.jose.jwk.source.JWKSource;
//import com.nimbusds.jose.proc.SecurityContext;
//import lombok.AllArgsConstructor;
//import lombok.SneakyThrows;
//import lombok.extern.slf4j.Slf4j;
//import org.jeecg.common.system.util.JwtUtil;
//import org.jeecg.config.security.app.AppGrantAuthenticationConvert;
//import org.jeecg.config.security.app.AppGrantAuthenticationProvider;
//import org.jeecg.config.security.password.PasswordGrantAuthenticationConvert;
//import org.jeecg.config.security.password.PasswordGrantAuthenticationProvider;
//import org.jeecg.config.security.phone.PhoneGrantAuthenticationConvert;
//import org.jeecg.config.security.phone.PhoneGrantAuthenticationProvider;
//import org.jeecg.config.security.social.SocialGrantAuthenticationConvert;
//import org.jeecg.config.security.social.SocialGrantAuthenticationProvider;
//import org.jeecg.config.shiro.ignore.InMemoryIgnoreAuth;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.annotation.Order;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.security.config.Customizer;
//import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
//import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
//import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
//import org.springframework.security.crypto.password.NoOpPasswordEncoder;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.security.oauth2.jwt.JwtDecoder;
//import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
//import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
//import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
//import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
//import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
//import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
//import org.springframework.security.oauth2.server.authorization.token.*;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
//import org.springframework.web.cors.CorsConfiguration;
//
//import java.security.KeyPair;
//import java.security.KeyPairGenerator;
//import java.security.SecureRandom;
//import java.security.interfaces.ECPrivateKey;
//import java.security.interfaces.ECPublicKey;
//import java.util.Arrays;
//
///**
// * spring authorization server核心配置
// * @author eightmonth@qq.com
// * @date 2024/1/2 9:29
// */
//@Configuration
//@EnableWebSecurity
//@EnableMethodSecurity
//@AllArgsConstructor
//@Slf4j
//public class SecurityConfig {
//
//    private JdbcTemplate jdbcTemplate;
//    private OAuth2AuthorizationService authorizationService;
//    private JeecgAuthenticationConvert jeecgAuthenticationConvert;
//
//    @Bean
//    @Order(1)
//    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
//            throws Exception {
//        // 使用新的配置方式替代弃用的applyDefaultSecurity
//        http.securityMatcher(new AntPathRequestMatcher("/oauth2/**"))
//                .authorizeHttpRequests(authorize ->
//                        authorize.anyRequest().authenticated()
//                )
//                .csrf(csrf -> csrf.disable())
//                .with(new OAuth2AuthorizationServerConfigurer(), oauth2 -> {
//                    oauth2
//                            .tokenEndpoint(tokenEndpoint -> tokenEndpoint
//                                    .accessTokenRequestConverter(new PasswordGrantAuthenticationConvert())
//                                    .authenticationProvider(new PasswordGrantAuthenticationProvider(authorizationService, tokenGenerator()))
//                            )
//                            .tokenEndpoint(tokenEndpoint -> tokenEndpoint
//                                    .accessTokenRequestConverter(new PhoneGrantAuthenticationConvert())
//                                    .authenticationProvider(new PhoneGrantAuthenticationProvider(authorizationService, tokenGenerator()))
//                            )
//                            .tokenEndpoint(tokenEndpoint -> tokenEndpoint
//                                    .accessTokenRequestConverter(new AppGrantAuthenticationConvert())
//                                    .authenticationProvider(new AppGrantAuthenticationProvider(authorizationService, tokenGenerator()))
//                            )
//                            .tokenEndpoint(tokenEndpoint -> tokenEndpoint
//                                    .accessTokenRequestConverter(new SocialGrantAuthenticationConvert())
//                                    .authenticationProvider(new SocialGrantAuthenticationProvider(authorizationService, tokenGenerator()))
//                            )
//                            //开启OpenID Connect 1.0（其中oidc为OpenID Connect的缩写）。 访问 /.well-known/openid-configuration即可获取认证信息
//                            .oidc(Customizer.withDefaults());
//                });
//
//        //请求接口异常处理：无Token和Token无效的情况
//        http.exceptionHandling(exceptions -> exceptions
//                .authenticationEntryPoint((request, response, authException) -> {
//                    // 记录详细的异常信息 - 未认证
//                    log.error("接口访问失败(未认证)，请求路径：{}，错误信息：{}", request.getRequestURI(), authException.getMessage(), authException);
//                    JwtUtil.responseError(response, 401, "Token格式错误或已过期");
//                })
//                .accessDeniedHandler((request, response, accessDeniedException) -> {
//                    // 记录详细的异常信息 - token无效或权限不足
//                    log.error("接口访问失败(token无效或权限不足)，请求路径：{}，错误信息：{}", request.getRequestURI(), accessDeniedException.getMessage(), accessDeniedException);
//                    JwtUtil.responseError(response, 403, "权限不足");
//                })
//        );
//
//        return http.build();
//    }
//
//    @Bean
//    @Order(2)
//    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http)
//            throws Exception {
//        http
//                //设置所有请求都需要认证，未认证的请求都被重定向到login页面进行登录
//                .authorizeHttpRequests((authorize) -> authorize
//                        .requestMatchers(InMemoryIgnoreAuth.get().stream().map(AntPathRequestMatcher::antMatcher).toList().toArray(new AntPathRequestMatcher[0])).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/sys/cas/client/validateLogin")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/sys/randomImage/**")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/sys/checkCaptcha")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/sys/login")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/sys/mLogin")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/sys/logout")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/sys/thirdLogin/**")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/sys/getEncryptedString")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/sys/sms")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/sys/phoneLogin")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/sys/user/checkOnlyUser")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/sys/user/register")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/sys/user/phoneVerification")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/sys/user/passwordChange")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/auth/2step-code")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/sys/common/static/**")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/sys/common/pdf/**")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/generic/**")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/sys/getLoginQrcode/**")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/sys/getQrcodeToken/**")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/sys/checkAuth")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/doc.html")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/**/*.js")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/**/*.css")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/**/*.html")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/**/*.svg")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/**/*.pdf")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/**/*.jpg")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/**/*.png")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/**/*.gif")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/**/*.ico")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/**/*.ttf")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/**/*.woff")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/**/*.woff2")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/druid/**")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/swagger-ui.html")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/swagger**/**")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/webjars/**")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/v3/**")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/WW_verify*")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/sys/annountCement/show/**")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/api/getUserInfo")).permitAll()
//
//                        //积木报表排除
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/jmreport/**")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/**/*.js.map")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/**/*.css.map")).permitAll()
//                        //积木BI大屏和仪表盘排除
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/drag/view")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/drag/onlDragDatasetHead/getLoginUser")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/drag/page/queryById")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/drag/page/addVisitsNumber")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/drag/page/queryTemplateList")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/drag/share/view/**")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/drag/onlDragDatasetHead/getAllChartData")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/drag/onlDragDatasetHead/getTotalData")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/drag/mock/json/**")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/jimubi/view")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/jimubi/share/view/**")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/drag/onlDragDatasetHead/getMapDataByCode")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/drag/onlDragDatasetHead/getTotalDataByCompId")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/drag/onlDragDatasetHead/queryAllById")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/drag/onlDragDatasetHead/getDictByCodes")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/dragChannelSocket/**")).permitAll()
//                        //大屏模板例子
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/test/bigScreen/**")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/bigscreen/template1/**")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/bigscreen/template1/**")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/websocket/**")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/newsWebsocket/**")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/vxeSocket/**")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/test/seata/**")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/error")).permitAll()
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/openapi/call/**")).permitAll()
//                        // APP版本信息
//                        .requestMatchers(AntPathRequestMatcher.antMatcher("/sys/version/app3version")).permitAll()
//                        .anyRequest().authenticated()
//                )
//                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
//                .cors(cors -> cors
//                        .configurationSource(req -> {
//                            CorsConfiguration config = new CorsConfiguration();
//                            config.applyPermitDefaultValues();
//                            config.setAllowedMethods(Arrays.asList("HEAD", "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
//                            return config;
//                        }))
//                .csrf(AbstractHttpConfigurer::disable)
//                // 配置OAuth2资源服务器，并添加JWT异常处理
//                .oauth2ResourceServer(oauth2 -> oauth2
//                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jeecgAuthenticationConvert))
//                        .authenticationEntryPoint((request, response, authException) -> {
//                            // 处理JWT解析失败的情况
//                            log.error("JWT验证失败，请求路径：{}，错误信息：{}", request.getRequestURI(), authException.getMessage(), authException);
//                            JwtUtil.responseError(response, 401, "Token格式错误或已过期");
//                        })
//                        .accessDeniedHandler((request, response, accessDeniedException) -> {
//                            // 处理权限不足的情况
//                            log.error("权限验证失败，请求路径：{}，错误信息：{}", request.getRequestURI(), accessDeniedException.getMessage(), accessDeniedException);
//                            JwtUtil.responseError(response, 403, "权限不足");
//                        })
//                )
//                // 全局异常处理
//                .exceptionHandling(exceptions -> exceptions
//                        .authenticationEntryPoint((request, response, authException) -> {
//                            // 记录详细的异常信息 - 未认证
//                            log.error("接口访问失败(未认证)，请求路径：{}，错误信息：{}", request.getRequestURI(), authException.getMessage(), authException);
//                            JwtUtil.responseError(response, 401, "Token格式错误或已过期");
//                        })
//                        .accessDeniedHandler((request, response, accessDeniedException) -> {
//                            // 记录详细的异常信息 - token无效或权限不足
//                            log.error("接口访问失败(token无效或权限不足)，请求路径：{}，错误信息：{}", request.getRequestURI(), accessDeniedException.getMessage(), accessDeniedException);
//                            JwtUtil.responseError(response, 403, "权限不足");
//                        })
//                );
//        return http.build();
//    }
//
//    /**
//     * 数据库保存注册客户端信息
//     */
//    @Bean
//    public RegisteredClientRepository registeredClientRepository() {
//        return new JdbcRegisteredClientRepository(jdbcTemplate);
//    }
//
//    /**
//     *配置 JWK，为JWT(id_token)提供加密密钥，用于加密/解密或签名/验签
//     * JWK详细见：https://datatracker.ietf.org/doc/html/draft-ietf-jose-json-web-key-41
//     */
//    @Bean
//    @SneakyThrows
//    public JWKSource<SecurityContext> jwkSource() {
//        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
//        // 如果不设置secureRandom，会存在一个问题，当应用重启后，原有的token将会全部失效，因为重启的keyPair与之前已经不同
//        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
//        // 重要！生产环境需要修改！
//        secureRandom.setSeed("jeecg".getBytes());
//        keyPairGenerator.initialize(256, secureRandom);
//        KeyPair keyPair = keyPairGenerator.generateKeyPair();
//        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
//        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
//
//        ECKey jwk = new ECKey.Builder(Curve.P_256, publicKey)
//                .privateKey(privateKey)
//                .keyID("jeecg")
//                .build();
//        JWKSet jwkSet = new JWKSet(jwk);
//        return new ImmutableJWKSet<>(jwkSet);
//    }
//
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return NoOpPasswordEncoder.getInstance();
//    }
//
//    /**
//     * 配置jwt解析器
//     */
//    @Bean
//    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
//        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
//    }
//
//    /**
//     *配置token生成器
//     */
//    @Bean
//    OAuth2TokenGenerator<?> tokenGenerator() {
//        JwtGenerator jwtGenerator = new JwtGenerator(new NimbusJwtEncoder(jwkSource()));
//        OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();
//        OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();
//        return new DelegatingOAuth2TokenGenerator(
//                new JeecgOAuth2AccessTokenGenerator(new NimbusJwtEncoder(jwkSource())),
//                new OAuth2RefreshTokenGenerator()
//        );
//    }
//}
package org.jeecg.config.security;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.config.security.app.AppGrantAuthenticationConvert;
import org.jeecg.config.security.app.AppGrantAuthenticationProvider;
import org.jeecg.config.security.password.PasswordGrantAuthenticationConvert;
import org.jeecg.config.security.password.PasswordGrantAuthenticationProvider;
import org.jeecg.config.security.phone.PhoneGrantAuthenticationConvert;
import org.jeecg.config.security.phone.PhoneGrantAuthenticationProvider;
import org.jeecg.config.security.social.SocialGrantAuthenticationConvert;
import org.jeecg.config.security.social.SocialGrantAuthenticationProvider;
import org.jeecg.config.shiro.ignore.InMemoryIgnoreAuth;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * spring authorization server核心配置
 * @author eightmonth@qq.com
 * @date 2024/1/2 9:29
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@AllArgsConstructor
@Slf4j
public class SecurityConfig {

    private JdbcTemplate jdbcTemplate;
    private OAuth2AuthorizationService authorizationService;
    private JeecgAuthenticationConvert jeecgAuthenticationConvert;

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
            throws Exception {
        // 使用新的配置方式
        http.securityMatcher("/oauth2/**")
                .authorizeHttpRequests(authorize ->
                        authorize.anyRequest().authenticated()
                )
                .csrf(AbstractHttpConfigurer::disable)
                .with(new OAuth2AuthorizationServerConfigurer(), oauth2 -> {
                    oauth2
                            .tokenEndpoint(tokenEndpoint -> tokenEndpoint
                                    .accessTokenRequestConverter(new PasswordGrantAuthenticationConvert())
                                    .authenticationProvider(new PasswordGrantAuthenticationProvider(authorizationService, tokenGenerator()))
                            )
                            .tokenEndpoint(tokenEndpoint -> tokenEndpoint
                                    .accessTokenRequestConverter(new PhoneGrantAuthenticationConvert())
                                    .authenticationProvider(new PhoneGrantAuthenticationProvider(authorizationService, tokenGenerator()))
                            )
                            .tokenEndpoint(tokenEndpoint -> tokenEndpoint
                                    .accessTokenRequestConverter(new AppGrantAuthenticationConvert())
                                    .authenticationProvider(new AppGrantAuthenticationProvider(authorizationService, tokenGenerator()))
                            )
                            .tokenEndpoint(tokenEndpoint -> tokenEndpoint
                                    .accessTokenRequestConverter(new SocialGrantAuthenticationConvert())
                                    .authenticationProvider(new SocialGrantAuthenticationProvider(authorizationService, tokenGenerator()))
                            )
                            //开启OpenID Connect 1.0（其中oidc为OpenID Connect的缩写）。 访问 /.well-known/openid-configuration即可获取认证信息
                            .oidc(Customizer.withDefaults());
                });

        //请求接口异常处理：无Token和Token无效的情况
        http.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    // 记录详细的异常信息 - 未认证
                    log.error("接口访问失败(未认证)，请求路径：{}，错误信息：{}", request.getRequestURI(), authException.getMessage(), authException);
                    JwtUtil.responseError(response, 401, "Token格式错误或已过期");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    // 记录详细的异常信息 - token无效或权限不足
                    log.error("接口访问失败(token无效或权限不足)，请求路径：{}，错误信息：{}", request.getRequestURI(), accessDeniedException.getMessage(), accessDeniedException);
                    JwtUtil.responseError(response, 403, "权限不足");
                })
        );

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http)
            throws Exception {
        http
                //设置所有请求都需要认证，未认证的请求都被重定向到login页面进行登录
                .authorizeHttpRequests((authorize) -> authorize
                        // 使用新的路径匹配方式
                        .requestMatchers(getPermitAllPaths()).permitAll()
                        .anyRequest().authenticated()
                )
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .cors(cors -> cors
                        .configurationSource(req -> {
                            CorsConfiguration config = new CorsConfiguration();
                            config.applyPermitDefaultValues();
                            config.setAllowedMethods(Arrays.asList("HEAD", "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
                            return config;
                        }))
                .csrf(AbstractHttpConfigurer::disable)
                // 配置OAuth2资源服务器，并添加JWT异常处理
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jeecgAuthenticationConvert))
                        .authenticationEntryPoint((request, response, authException) -> {
                            // 处理JWT解析失败的情况
                            log.error("JWT验证失败，请求路径：{}，错误信息：{}", request.getRequestURI(), authException.getMessage(), authException);
                            JwtUtil.responseError(response, 401, "Token格式错误或已过期");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            // 处理权限不足的情况
                            log.error("权限验证失败，请求路径：{}，错误信息：{}", request.getRequestURI(), accessDeniedException.getMessage(), accessDeniedException);
                            JwtUtil.responseError(response, 403, "权限不足");
                        })
                )
                // 全局异常处理
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            // 记录详细的异常信息 - 未认证
                            log.error("接口访问失败(未认证)，请求路径：{}，错误信息：{}", request.getRequestURI(), authException.getMessage(), authException);
                            JwtUtil.responseError(response, 401, "Token格式错误或已过期");
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            // 记录详细的异常信息 - token无效或权限不足
                            log.error("接口访问失败(token无效或权限不足)，请求路径：{}，错误信息：{}", request.getRequestURI(), accessDeniedException.getMessage(), accessDeniedException);
                            JwtUtil.responseError(response, 403, "权限不足");
                        })
                );
        return http.build();
    }

    /**
     * 获取所有需要放行的路径数组
     */
//    private String[] getPermitAllPaths() {
//        return new String[]{
//                // 动态忽略路径
//                InMemoryIgnoreAuth.get().toArray(new String[0]),
//
//                // 系统相关路径
//                "/sys/cas/client/validateLogin",
//                "/sys/randomImage/**",
//                "/sys/checkCaptcha",
//                "/sys/login",
//                "/sys/mLogin",
//                "/sys/logout",
//                "/sys/thirdLogin/**",
//                "/sys/getEncryptedString",
//                "/sys/sms",
//                "/sys/phoneLogin",
//                "/sys/user/checkOnlyUser",
//                "/sys/user/register",
//                "/sys/user/phoneVerification",
//                "/sys/user/passwordChange",
//                "/auth/2step-code",
//                "/sys/common/static/**",
//                "/sys/common/pdf/**",
//                "/generic/**",
//                "/sys/getLoginQrcode/**",
//                "/sys/getQrcodeToken/**",
//                "/sys/checkAuth",
//                "/sys/annountCement/show/**",
//                "/api/getUserInfo",
//                "/sys/version/app3version",
//
//                // 静态资源路径
//                "/",
//                "/doc.html",
//                "/**/*.js",
//                "/**/*.css",
//                "/**/*.html",
//                "/**/*.svg",
//                "/**/*.pdf",
//                "/**/*.jpg",
//                "/**/*.png",
//                "/**/*.gif",
//                "/**/*.ico",
//                "/**/*.ttf",
//                "/**/*.woff",
//                "/**/*.woff2",
//                "/**/*.js.map",
//                "/**/*.css.map",
//
//                // 监控和文档路径
//                "/druid/**",
//                "/swagger-ui.html",
//                "/swagger**/**",
//                "/webjars/**",
//                "/v3/**",
//                "/WW_verify*",
//
//                // 积木报表路径
//                "/jmreport/**",
//
//                // 积木BI大屏和仪表盘路径
//                "/drag/view",
//                "/drag/onlDragDatasetHead/getLoginUser",
//                "/drag/page/queryById",
//                "/drag/page/addVisitsNumber",
//                "/drag/page/queryTemplateList",
//                "/drag/share/view/**",
//                "/drag/onlDragDatasetHead/getAllChartData",
//                "/drag/onlDragDatasetHead/getTotalData",
//                "/drag/mock/json/**",
//                "/jimubi/view",
//                "/jimubi/share/view/**",
//                "/drag/onlDragDatasetHead/getMapDataByCode",
//                "/drag/onlDragDatasetHead/getTotalDataByCompId",
//                "/drag/onlDragDatasetHead/queryAllById",
//                "/drag/onlDragDatasetHead/getDictByCodes",
//                "/dragChannelSocket/**",
//
//                // 大屏模板例子
//                "/test/bigScreen/**",
//                "/bigscreen/template1/**",
//
//                // WebSocket路径
//                "/websocket/**",
//                "/newsWebsocket/**",
//                "/vxeSocket/**",
//
//                // 测试和其他路径
//                "/test/seata/**",
//                "/error",
//                "/openapi/call/**"
//        };
//    }
    private String[] getPermitAllPaths() {
        return Stream.concat(
                Arrays.stream(InMemoryIgnoreAuth.get().toArray(new String[0])),
                Stream.of(
                        "/sys/cas/client/validateLogin",
                        "/sys/randomImage/**",
                        "/sys/checkCaptcha",
                        "/sys/login",
                        "/sys/mLogin",
                        "/sys/logout",
                        "/sys/thirdLogin/**",
                        "/sys/getEncryptedString",
                        "/sys/sms",
                        "/sys/phoneLogin",
                        "/sys/user/checkOnlyUser",
                        "/sys/user/register",
                        "/sys/user/phoneVerification",
                        "/sys/user/passwordChange",
                        "/auth/2step-code",
                        "/sys/common/static/**",
                        "/sys/common/pdf/**",
                        "/generic/**",
                        "/sys/getLoginQrcode/**",
                        "/sys/getQrcodeToken/**",
                        "/sys/checkAuth",
                        "/sys/annountCement/show/**",
                        "/api/getUserInfo",
                        "/sys/version/app3version",
                        "/",
                        "/doc.html",
                        "/**/*.js",
                        "/**/*.css",
                        "/**/*.html",
                        "/**/*.svg",
                        "/**/*.pdf",
                        "/**/*.jpg",
                        "/**/*.png",
                        "/**/*.gif",
                        "/**/*.ico",
                        "/**/*.ttf",
                        "/**/*.woff",
                        "/**/*.woff2",
                        "/druid/**",
                        "/swagger-ui.html",
                        "/swagger**/**",
                        "/webjars/**",
                        "/v3/**",
                        "/WW_verify*",
                        "/jmreport/**",
                        "/**/*.js.map",
                        "/**/*.css.map",
                        "/drag/view",
                        "/drag/onlDragDatasetHead/getLoginUser",
                        "/drag/page/queryById",
                        "/drag/page/addVisitsNumber",
                        "/drag/page/queryTemplateList",
                        "/drag/share/view/**",
                        "/drag/onlDragDatasetHead/getAllChartData",
                        "/drag/onlDragDatasetHead/getTotalData",
                        "/drag/mock/json/**",
                        "/jimubi/view",
                        "/jimubi/share/view/**",
                        "/drag/onlDragDatasetHead/getMapDataByCode",
                        "/drag/onlDragDatasetHead/getTotalDataByCompId",
                        "/drag/onlDragDatasetHead/queryAllById",
                        "/drag/onlDragDatasetHead/getDictByCodes",
                        "/dragChannelSocket/**",
                        "/test/bigScreen/**",
                        "/bigscreen/template1/**",
                        "/websocket/**",
                        "/newsWebsocket/**",
                        "/vxeSocket/**",
                        "/test/seata/**",
                        "/error",
                        "/openapi/call/**"
                )
        ).toArray(String[]::new);
    }

    /**
     * 数据库保存注册客户端信息
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    /**
     *配置 JWK，为JWT(id_token)提供加密密钥，用于加密/解密或签名/验签
     * JWK详细见：https://datatracker.ietf.org/doc/html/draft-ietf-jose-json-web-key-41
     */
    @Bean
    @SneakyThrows
    public JWKSource<SecurityContext> jwkSource() {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
        // 如果不设置secureRandom，会存在一个问题，当应用重启后，原有的token将会全部失效，因为重启的keyPair与之前已经不同
        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
        // 重要！生产环境需要修改！
        secureRandom.setSeed("jeecg".getBytes());
        keyPairGenerator.initialize(256, secureRandom);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
        ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();

        ECKey jwk = new ECKey.Builder(Curve.P_256, publicKey)
                .privateKey(privateKey)
                .keyID("jeecg")
                .build();
        JWKSet jwkSet = new JWKSet(jwk);
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }

    /**
     * 配置jwt解析器
     */
    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    /**
     *配置token生成器
     */
    @Bean
    OAuth2TokenGenerator<?> tokenGenerator() {
        JwtGenerator jwtGenerator = new JwtGenerator(new NimbusJwtEncoder(jwkSource()));
        OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();
        OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();
        return new DelegatingOAuth2TokenGenerator(
                new JeecgOAuth2AccessTokenGenerator(new NimbusJwtEncoder(jwkSource())),
                new OAuth2RefreshTokenGenerator()
        );
    }
}