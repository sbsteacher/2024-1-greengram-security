package com.green.greengram.security.oauth2;

import com.green.greengram.common.AppProperties;
import com.green.greengram.common.CookieUtils;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

/*
    OAuth2 인증 과정 중에 쿠키에 저장하는 데이터는 보안 때문. CSRF 공격을 방지하기 위해 state값을 사용.
    OAuth2AuthorizationRequest를 인가코드(인증코드) 받을 때까지 사용 > access token받은 이후에는
    다시 사용할 가치가 없기 때문에 세션에서 삭제.

    만약 토큰이 만료가 되어 권한부여 요청을(인증/인가 코드를 요청) 다시 하는 경우 이전의 세션이 존재한다면 현재를 사용하는 것이
    아니라 예전꺼를 사용하기 때문에 문제될 가능성이 있음. 그래서 세션에서 삭제를 하낟.
    인가/인증 코드가 1회용인 것 처럼  OAuth2AuthorizationRequest 객체도 1회용으로 사용.
    (인가/인증 코드는 요청 보낼때마다 값이 달라진다)

    스프링 시큐리티 OAuth 처리 때 사용하는 필터가 2개가 있음.
    OAuth2AuthorizationRequestRedirectFilter(as 가필터), OAuth2LoginAuthenticationFilter(as 나필터)

    OAuth2AuthorizationRequest(as A)는 소셜로그인 요청할 때마다 생성되는 객체
    1단계 인가코드(임시코드, 인증코드)를 요청할 때 A를 사용함
    2단계 Access Token을 요청한 이후에는 A를 사용할 일이 발생하지 않기 때문에 Cookie에서 삭제

    세션을 이용해서 처리하는 방식은 확장이 불리함. >> 쿠키로 해결. 그래서 이전에 세션에서 삭제를 처리를
    removeAuthorizationRequest 메소드에서 했던 것 같음.

    가필터에서 removeAuthorizationRequest 메소드를 호출해서 리턴받은 값을 활용한다.
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationRequestBasedOnCookieRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {
    private final CookieUtils cookieUtils;
    private final AppProperties appProperties;



    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        log.info("CookieRepository - loadAuthorizationRequest");
        return cookieUtils.getCookie(request
                                    , appProperties.getOauth2().getAuthorizationRequestCookieName()
                                    , OAuth2AuthorizationRequest.class
        );
    }

    @Override
    public void saveAuthorizationRequest (OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        log.info("CookieRepository - saveAuthorizationRequest");
        if(authorizationRequest == null) {
            this.removeAuthorizationRequestCookies(response);
            return;
        }
        cookieUtils.setCookie(response
                , appProperties.getOauth2().getAuthorizationRequestCookieName()
                , authorizationRequest
                , appProperties.getOauth2().getCookieExpirySeconds());

        //FE로 돌아갈 redirect 주소값 (즉, FE가 redirect_uri 파라미터로 백엔드에 보내준 값)
        String redirectUriAfterLogin = request.getParameter(appProperties.getOauth2().getRedirectUriParamCookieName());
        log.info("redirectUriAfterLogin: {}", redirectUriAfterLogin);
        if(StringUtils.isNotBlank(redirectUriAfterLogin)) {
            cookieUtils.setCookie(response
                    , appProperties.getOauth2().getRedirectUriParamCookieName()
                    , redirectUriAfterLogin
                    , appProperties.getOauth2().getCookieExpirySeconds()
            );
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        log.info("CookieRepository - removeAuthorizationRequest");
        return this.loadAuthorizationRequest(request); //null
    }

    public void removeAuthorizationRequestCookies(HttpServletResponse response) {
        log.info("CookieRepository - removeAuthorizationRequestCookies");
        cookieUtils.deleteCookie(response, appProperties.getOauth2().getAuthorizationRequestCookieName());
        cookieUtils.deleteCookie(response, appProperties.getOauth2().getRedirectUriParamCookieName());
    }
}
