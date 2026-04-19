package com.social_media_be.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.util.SerializationUtils;

import java.util.Base64;
import java.util.Optional;

public class CookieUtils {

  public static Optional<Cookie> getCookie(HttpServletRequest request, String name) {
    Cookie[] cookies = request.getCookies();

    if (cookies != null && cookies.length > 0) {
      for (Cookie cookie : cookies) {
        if (cookie.getName().equals(name)) {
          return Optional.of(cookie);
        }
      }
    }

    return Optional.empty();
  }

  public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
    ResponseCookie cookie = ResponseCookie.from(name, value)
      .path("/")
      .httpOnly(true)
      .secure(true)
      .maxAge(maxAge)
      .sameSite("None") // Cho phép gửi trên cùng domain localhost:3000 -> localhost:8080
      .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
  }

  public static void deleteCookie(HttpServletRequest request, HttpServletResponse response, String name) {
    Cookie[] cookies = request.getCookies();

    if (cookies != null && cookies.length > 0) {
      for (Cookie cookie : cookies) {
        if (cookie.getName().equals(name)) {
          ResponseCookie deleteCookie = ResponseCookie.from(name, "")
            .path("/")
            .httpOnly(true)
            .secure(true)
            .maxAge(maxAge)
            .sameSite("None") // Cho phép gửi trên cùng domain localhost:3000 -> localhost:8080
            .build();
          response.addHeader(HttpHeaders.SET_COOKIE, deleteCookie.toString());
        }
      }
    }
  }

  public static String serialize(Object object) {
    return Base64.getUrlEncoder()
      .encodeToString(SerializationUtils.serialize(object));
  }

  public static <T> T deserialize(Cookie cookie, Class<T> cls) {
    return cls.cast(SerializationUtils.deserialize(
      Base64.getUrlDecoder().decode(cookie.getValue())));
  }
}
