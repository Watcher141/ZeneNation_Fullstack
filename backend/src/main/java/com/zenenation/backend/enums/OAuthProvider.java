package com.zenenation.backend.enums;

/**
 * Identifies how a user account was created / how they log in.
 *
 * LOCAL   → Registered manually with email + password
 * GOOGLE  → Logged in via Google OAuth2
 *
 * Why do we need this?
 * A user who signed up via Google has NO password in our database.
 * If they try to use "Forgot Password", we need to tell them:
 * "Your account uses Google login — please sign in with Google."
 *
 * This also prevents account merging issues where two accounts
 * exist for the same email (one LOCAL, one GOOGLE).
 * We handle this in the OAuth2 success handler — if email already
 * exists as LOCAL, we block the OAuth2 login and ask them to
 * use their password instead.
 */
public enum OAuthProvider {

    /**
     * Account created via email + password registration.
     */
    LOCAL,

    /**
     * Account created / linked via Google OAuth2.
     */
    GOOGLE
}