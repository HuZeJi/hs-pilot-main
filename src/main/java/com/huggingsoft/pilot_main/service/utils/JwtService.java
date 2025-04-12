package com.huggingsoft.pilot_main.service.utils;

import com.hsoft.model.entities.v1.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails; // Assuming you use Spring Security UserDetails
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service responsible for handling JWT (JSON Web Token) operations:
 * generation, validation, and claim extraction.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String jwtSecretString; // Injected Base64 encoded secret from properties

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs; // Injected expiration time from properties

    private SecretKey jwtSigningKey; // Parsed SecretKey object

    /**
     * Initializes the signing key after properties are injected.
     * This ensures the key is ready before any JWT operations are performed.
     */
    @jakarta.annotation.PostConstruct // Use jakarta annotation
    protected void init() {
        this.jwtSigningKey = getSigningKey();
        log.info("JWT Signing Key initialized.");
    }

    /**
     * Extracts the username (subject) from the JWT token.
     *
     * @param token The JWT token string.
     * @return The username stored in the token's subject claim.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from the JWT token.
     *
     * @param token The JWT token string.
     * @return The expiration date.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts a specific claim from the JWT token using a claims resolver function.
     *
     * @param token          The JWT token string.
     * @param claimsResolver A function that takes Claims and returns the desired claim value.
     * @param <T>            The type of the claim value.
     * @return The extracted claim value.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Generates a JWT token for the given UserDetails.
     * Includes username as subject and default claims (issuedAt, expiration).
     *
     * @param user The UserDetails object representing the authenticated user.
     * @return The generated JWT token string.
     */
    public String generateToken(User user) {
        return generateToken(new HashMap<>(), user); // Generate with no extra claims
    }

    /**
     * Generates a JWT token with extra claims for the given UserDetails.
     *
     * @param extraClaims Additional claims to include in the token payload.
     * @param user The UserDetails object representing the authenticated user.
     * @return The generated JWT token string.
     */
    public String generateToken(Map<String, Object> extraClaims, User user) {
        log.debug("Generating token for user: {}", user.getUsername());
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(user.getUsername()) // Usually username
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                // Sign with the parsed SecretKey and HS256 algorithm
                .signWith(jwtSigningKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validates the JWT token against the UserDetails.
     * Checks if the username matches and if the token is not expired.
     * The signature is implicitly validated when parsing claims.
     *
     * @param token       The JWT token string.
     * @param userDetails The UserDetails object to validate against.
     * @return true if the token is valid for the user, false otherwise.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token validation failed for user {}: {}", (userDetails != null ? userDetails.getUsername() : "unknown"), e.getMessage());
            return false;
        }
    }

    /**
     * Checks if the JWT token has expired.
     *
     * @param token The JWT token string.
     * @return true if the token has expired, false otherwise.
     * @throws JwtException if the token cannot be parsed (invalid format, etc.)
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Parses the JWT token and returns all claims.
     * This method implicitly validates the token's signature and expiration.
     *
     * @param token The JWT token string.
     * @return The Claims object containing all data from the token payload.
     * @throws ExpiredJwtException if the token is expired.
     * @throws UnsupportedJwtException if the token format is not supported.
     * @throws MalformedJwtException if the token is malformed.
     * @throws SignatureException if the signature validation fails.
     * @throws IllegalArgumentException if the token string is null or empty.
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(jwtSigningKey) // Use the parsed SecretKey
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.debug("JWT token is expired: {}", e.getMessage());
            throw e; // Re-throw specific exceptions if needed upstream
        } catch (UnsupportedJwtException e) {
            log.warn("JWT token is unsupported: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.warn("Invalid JWT token format: {}", e.getMessage());
            throw e;
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Decodes the Base64 encoded secret string and creates a SecretKey instance.
     *
     * @return The SecretKey used for signing and verifying tokens.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(this.jwtSecretString);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
