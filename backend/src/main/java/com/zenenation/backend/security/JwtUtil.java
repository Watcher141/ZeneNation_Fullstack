package com.zenenation.backend.security;

import com.zenenation.backend.exception.UnauthorizedException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Handles everything JWT:
 *  - Generate access tokens
 *  - Generate refresh tokens
 *  - Extract claims (email, expiry etc.) from tokens
 *  - Validate tokens
 *
 * ACCESS TOKEN  → short lived (1 day)  — used on every API call
 * REFRESH TOKEN → long lived (7 days)  — used only to get a new access token
 *
 * Both tokens are signed with HMAC-SHA256 using the secret from application.yml.
 * The refresh token carries a "type" claim so we can reject
 * an access token being used as a refresh token and vice versa.
 */
@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    // -------------------------------------------------------------------------
    // TOKEN GENERATION
    // -------------------------------------------------------------------------

    /**
     * Generate an access token for the given user.
     * Embeds the user's email as the subject and role as a claim.
     * Used after login and after token refresh.
     */
    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        // Store role for quick access without DB lookup
        claims.put("role", userDetails.getAuthorities()
                .stream()
                .findFirst()
                .map(Object::toString)
                .orElse("ROLE_USER"));
        return buildToken(claims, userDetails.getUsername(), accessTokenExpiration);
    }

    /**
     * Generate a refresh token for the given user.
     * Longer lived. Only carries "type" claim — minimal data.
     * NOT to be used for API authorization — only for getting new access tokens.
     */
    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        return buildToken(claims, userDetails.getUsername(), refreshTokenExpiration);
    }

    /**
     * Core token builder — used by both generateAccessToken and generateRefreshToken.
     */
    private String buildToken(Map<String, Object> extraClaims, String subject, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)                                        // email
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    // -------------------------------------------------------------------------
    // TOKEN VALIDATION
    // -------------------------------------------------------------------------

    /**
     * Validates that:
     * 1. Token signature is correct (not tampered with)
     * 2. Token is not expired
     * 3. Token subject (email) matches the UserDetails
     * 4. Token type is "access" (not a refresh token being misused)
     */
    public boolean isAccessTokenValid(String token, UserDetails userDetails) {
        try {
            final String email = extractEmail(token);
            final String tokenType = extractClaim(token, claims -> claims.get("type", String.class));
            return email.equals(userDetails.getUsername())
                    && !isTokenExpired(token)
                    && "access".equals(tokenType);
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Validates a refresh token.
     * Checks signature, expiry, and that it is specifically a refresh token.
     */
    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        try {
            final String email = extractEmail(token);
            final String tokenType = extractClaim(token, claims -> claims.get("type", String.class));
            return email.equals(userDetails.getUsername())
                    && !isTokenExpired(token)
                    && "refresh".equals(tokenType);
        } catch (JwtException e) {
            log.warn("Invalid refresh token: {}", e.getMessage());
            return false;
        }
    }

    // -------------------------------------------------------------------------
    // CLAIM EXTRACTION
    // -------------------------------------------------------------------------

    /**
     * Extract the email (subject) from a token.
     * Called in JwtAuthFilter to identify the user.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract the expiration date from a token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic claim extractor.
     * Takes a function that picks which claim to extract.
     * Example: extractClaim(token, Claims::getSubject)
     *          extractClaim(token, claims -> claims.get("role", String.class))
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parse and return ALL claims from the token.
     * Throws JwtException if the token is invalid or tampered with.
     * The signing key verification happens here automatically.
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT token is expired");
            throw new UnauthorizedException("Token has expired. Please login again.");
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported");
            throw new UnauthorizedException("Unsupported token format.");
        } catch (MalformedJwtException e) {
            log.warn("JWT token is malformed");
            throw new UnauthorizedException("Invalid token.");
        } catch (SecurityException e) {
            log.warn("JWT signature validation failed");
            throw new UnauthorizedException("Invalid token signature.");
        } catch (IllegalArgumentException e) {
            log.warn("JWT token is empty or null");
            throw new UnauthorizedException("Token is missing.");
        }
    }

    /**
     * Check if a token's expiry date has passed.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Build the HMAC-SHA256 signing key from the base64-encoded secret.
     * Called every time we sign or verify a token.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
