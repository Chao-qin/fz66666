package com.fashion.supplychain.system.orchestration;

import com.fashion.supplychain.common.AuthTokenService;
import com.fashion.supplychain.common.TokenSubject;
import com.fashion.supplychain.system.entity.User;
import com.fashion.supplychain.system.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserLoginHelperTest {

    @Mock
    private UserService userService;
    @Mock
    private AuthTokenService authTokenService;
    @InjectMocks
    private UserLoginHelper helper;

    @Test
    void platformLoginUsesTheVerifiedRoleAndDoesNotRequireATenant() {
        User credentials = credentials();
        credentials.setTenantId(42L);
        User account = new User();
        account.setId(7L);
        account.setUsername("operator");
        account.setIsSuperAdmin(true);
        when(userService.login("operator", "test-password", null)).thenReturn(account);
        when(authTokenService.issueToken(any(TokenSubject.class), isNull())).thenReturn("test-token");

        assertEquals("test-token", helper.loginWithToken(credentials, true).get("token"));

        ArgumentCaptor<TokenSubject> subject = ArgumentCaptor.forClass(TokenSubject.class);
        verify(authTokenService).issueToken(subject.capture(), isNull());
        assertTrue(subject.getValue().isSuperAdmin());
        assertNull(subject.getValue().getTenantId());
    }

    @Test
    void platformLoginRejectsCompanyAccountsEvenWithValidCredentials() {
        User credentials = credentials();
        credentials.setIsSuperAdmin(true);
        User account = new User();
        account.setTenantId(42L);
        account.setIsSuperAdmin(false);
        when(userService.login("operator", "test-password", null)).thenReturn(account);

        assertThrows(IllegalStateException.class, () -> helper.loginWithToken(credentials, true));
        verifyNoInteractions(authTokenService);
    }

    @Test
    void companyLoginStillUsesTheSelectedTenant() {
        User credentials = credentials();
        credentials.setTenantId(42L);
        User account = new User();
        account.setTenantId(42L);
        when(userService.login("operator", "test-password", 42L)).thenReturn(account);
        when(authTokenService.issueToken(any(TokenSubject.class), isNull())).thenReturn("test-token");

        assertEquals("test-token", helper.loginWithToken(credentials, false).get("token"));
    }

    private User credentials() {
        User credentials = new User();
        credentials.setUsername("operator");
        credentials.setPassword("test-password");
        return credentials;
    }
}
