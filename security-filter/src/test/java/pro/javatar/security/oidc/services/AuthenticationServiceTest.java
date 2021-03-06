package pro.javatar.security.oidc.services;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import pro.javatar.security.oidc.model.TokenDetails;
import pro.javatar.security.oidc.utils.JwtTokenGenerator;
import pro.javatar.security.oidc.utils.KeyUtils;

import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collection;

public class AuthenticationServiceTest {

    private TokenService tokenService;
    private OidcAuthenticationHelper oidcAuthenticationHelper;
    private PublicKeyCacheService publicKeyCacheService;
    private AuthenticationService service;

    @Before
    public void setUp() throws Exception {
        tokenService = mock(TokenService.class);
        publicKeyCacheService = mock(PublicKeyCacheService.class);

        OidcConfiguration oidcConfiguration = new OidcConfiguration();
        oidcConfiguration.setClientId("producer-service");
        oidcConfiguration.setCheckTokenType(true);
        oidcConfiguration.setCheckIsActive(true);

        OAuth2AuthorizationFlowService auth2AuthorizationFlowService =
                new OAuth2AuthorizationFlowService();
        auth2AuthorizationFlowService.setPublicKeyCacheService(publicKeyCacheService);
        auth2AuthorizationFlowService.setOidcConfiguration(oidcConfiguration);

        oidcAuthenticationHelper = new OidcAuthenticationHelper();
        oidcAuthenticationHelper.setOidcConfiguration(oidcConfiguration);
        oidcAuthenticationHelper.setAuth2AuthorizationFlowService(auth2AuthorizationFlowService);

        service = new AuthenticationService(this.tokenService, oidcAuthenticationHelper);
    }

    @Test
    public void authenticateByTokenDetails() throws Exception {
        JwtTokenGenerator tokenGenerator =
                new JwtTokenGenerator("http://localhost:8080/auth/test-realm",
                        "producer-service", Arrays.asList("USER_READ", "USER_WRITE"));
        String accessToken = tokenGenerator.generateJwtAccessToken();
        TokenDetails tokenDetails = new TokenDetails();
        tokenDetails.setAccessToken(accessToken);

        String publicKey = KeyUtils.importPublicKeyToPem(tokenGenerator.getIdpPair().getPublic());
        when(publicKeyCacheService.getPublicKeyByRealm("test-realm")).thenReturn(publicKey);

        service.authenticateByTokenDetails(tokenDetails);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        TokenDetails credentials = (TokenDetails) authentication.getCredentials();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        assertThat(credentials.getAccessToken(), is(accessToken)) ;

        assertThat(authorities.size(), is(2));
        assertThat(authorities.contains(new SimpleGrantedAuthority("USER_READ")), is(true));
        assertThat(authorities.contains(new SimpleGrantedAuthority("USER_WRITE")), is(true));
    }

    @Test
    public void authenticateByRefreshToken() throws Exception {
        JwtTokenGenerator tokenGenerator =
                new JwtTokenGenerator("http://localhost:8080/auth/test-realm",
                        "producer-service", Arrays.asList("USER_READ", "USER_WRITE"));
        String accessToken = tokenGenerator.generateJwtAccessToken();
        TokenDetails tokenDetails = new TokenDetails();
        tokenDetails.setAccessToken(accessToken);
        String refreshToken = "refresh-token";

        when(tokenService.getTokenByRefreshToken(refreshToken)).thenReturn(tokenDetails);

        String publicKey = KeyUtils.importPublicKeyToPem(tokenGenerator.getIdpPair().getPublic());
        when(publicKeyCacheService.getPublicKeyByRealm("test-realm")).thenReturn(publicKey);

        service.authenticateByRefreshToken(refreshToken);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        TokenDetails credentials = (TokenDetails) authentication.getCredentials();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        assertThat(credentials.getAccessToken(), is(accessToken)) ;

        assertThat(authorities.size(), is(2));
        assertThat(authorities.contains(new SimpleGrantedAuthority("USER_READ")), is(true));
        assertThat(authorities.contains(new SimpleGrantedAuthority("USER_WRITE")), is(true));
    }
}