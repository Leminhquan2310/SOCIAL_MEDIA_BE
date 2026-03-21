package com.social_media_be.config.security.oauth2;

import com.social_media_be.config.AppProperties;
import com.social_media_be.entity.RefreshToken;
import com.social_media_be.entity.User;
import com.social_media_be.entity.UserPrincipal;

import com.social_media_be.repository.UserRepository;
import com.social_media_be.service.JWTService;
import com.social_media_be.service.RefreshTokenService;
import com.social_media_be.utils.CookieUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JWTService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;

    private final AppProperties appProperties;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenDurationMs;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String targetUrl = determineTargetUrl(request, response, authentication);
        log.info("Redirecting to: " + targetUrl);
        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {
        Optional<String> redirectUri = CookieUtils.getCookie(request,
                        HttpCookieOAuth2AuthorizationRequestRepository.REDIRECT_URI_PARAM_COOKIE_NAME)
                .map(Cookie::getValue);

        if (redirectUri.isPresent() && !isAuthorizedRedirectUri(redirectUri.get())) {
            throw new RuntimeException("Sorry! We've got an Unauthorized Redirect URI");
        }

        String targetUrl = redirectUri.orElse(getDefaultTargetUrl());

        // ✅ LẤY USER PRINCIPAL
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        // ✅ LẤY USER TỪ DATABASE
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ✅ TẠO ACCESS TOKEN
        String accessToken = jwtService.generateToken(userPrincipal);

        // ✅ TẠO REFRESH TOKEN UUID
        RefreshToken refreshTokenObj = refreshTokenService.createRefreshToken(user.getId());
        String refreshTokenString = refreshTokenObj.getToken();

        // ✅ LƯU REFRESH TOKEN VÀO HTTP-ONLY COOKIE
        int cookieMaxAge = (int) (refreshTokenDurationMs / 1000);
        CookieUtils.addCookie(response, "refreshToken", refreshTokenString, cookieMaxAge);

        log.info("OAuth2 login successful for user: {}, provider: {}",
                user.getEmail(), user.getAuthProvider());

        // ✅ CHỈ REDIRECT VỀ TARGET URL (Token đã nằm trong Cookie)
        // Frontend sẽ tự động gọi Silent Refresh để lấy Access Token sau khi redirect
        return UriComponentsBuilder.fromUriString(targetUrl)
                .build().toUriString();
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }

    private boolean isAuthorizedRedirectUri(String uri) {
        URI clientRedirectUri = URI.create(uri);

        return appProperties.getOauth2().getAuthorizedRedirectUris()
                .stream()
                .anyMatch(authorizedRedirectUri -> {
                    URI authorizedURI = URI.create(authorizedRedirectUri);
                    return authorizedURI.getHost().equalsIgnoreCase(clientRedirectUri.getHost())
                            && authorizedURI.getPort() == clientRedirectUri.getPort();
                });
    }
}