package com.zenenation.backend.security;

import com.zenenation.backend.entity.Cart;
import com.zenenation.backend.entity.User;
import com.zenenation.backend.enums.OAuthProvider;
import com.zenenation.backend.enums.Role;
import com.zenenation.backend.repository.CartRepository;
import com.zenenation.backend.repository.UserRepository;
import com.zenenation.backend.service.impl.WelcomeCouponService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final WelcomeCouponService welcomeCouponService;
    private final JwtUtil jwtUtil;

    @Value("${app.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email   = oAuth2User.getAttribute("email");
        String name    = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");
        String googleId = oAuth2User.getAttribute("sub");

        log.info("OAuth2 login success for email: {}", email);

        User user = userRepository.findByEmail(email)
                .map(existingUser -> handleExistingUser(existingUser, googleId, picture))
                .orElseGet(() -> createNewOAuthUser(email, name, picture, googleId));

        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password("")
                .disabled(!user.getIsActive())
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .authorities(List.of(new SimpleGrantedAuthority(user.getRole().name())))
                .build();

        String accessToken  = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("accessToken",  accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("userId",       user.getId())
                .build().toUriString();

        log.debug("Redirecting OAuth2 user to: {}", redirectUri);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private User handleExistingUser(User user, String googleId, String picture) {
        if (user.getProvider() == OAuthProvider.LOCAL) {
            log.warn("OAuth2 login attempted for LOCAL account: {}", user.getEmail());
            return user;
        }
        user.setProviderId(googleId);
        if (picture != null) user.setProfileImageUrl(picture);
        user.setIsEmailVerified(true);
        return userRepository.save(user);
    }

    private User createNewOAuthUser(String email, String name, String picture, String googleId) {
        log.info("Creating new OAuth2 user for email: {}", email);

        User newUser = User.builder()
                .email(email)
                .name(name != null ? name : email)
                .password(null)
                .role(Role.ROLE_USER)
                .provider(OAuthProvider.GOOGLE)
                .providerId(googleId)
                .profileImageUrl(picture)
                .isActive(true)
                .isEmailVerified(true)
                .build();

        User savedUser = userRepository.save(newUser);

        // Create cart for new user
        cartRepository.save(Cart.builder().user(savedUser).build());

        // Generate personal welcome coupon
        try {
            welcomeCouponService.generateWelcomeCoupon(savedUser);
        } catch (Exception e) {
            log.warn("Failed to generate welcome coupon for OAuth2 user {}: {}", email, e.getMessage());
        }

        return savedUser;
    }
}