package com.example.token;

import org.apache.synapse.MessageContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TokenCacheMediatorTest {

    @Mock
    private MessageContext context;

    @InjectMocks
    private TokenCacheMediator mediator = new TokenCacheMediator();

    @BeforeEach
    public void beforeEach() throws Exception {
        // clear static fields
        setStaticField("tokenCache", null);
        setStaticField("tokenCacheExpiryTime", 0L);
    }

    @AfterEach
    public void afterEach() throws Exception {
        // clear static fields after test
        setStaticField("tokenCache", null);
        setStaticField("tokenCacheExpiryTime", 0L);
    }

    private void setStaticField(String name, Object value) throws Exception {
        Field f = TokenCacheMediator.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }

    private Object getStaticField(String name) throws Exception {
        Field f = TokenCacheMediator.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(null);
    }

    @Test
    public void saveWithToken_setsSavedAndCache() throws Exception {
        mediator.setAction("SAVE");
        when(context.getProperty("ACCESS_TOKEN")).thenReturn("TK123");

        boolean result = mediator.mediate(context);

        assertTrue(result);
        verify(context).setProperty("TOKEN_CACHE_STATUS", "SAVED");

        assertEquals("TK123", getStaticField("tokenCache"));
        long expiry = (Long) getStaticField("tokenCacheExpiryTime");
        assertTrue(expiry > System.currentTimeMillis());
    }

    @Test
    public void saveWithNoToken_setsSaveFailed() throws Exception {
        mediator.setAction("SAVE");
        when(context.getProperty("ACCESS_TOKEN")).thenReturn(null);

        boolean result = mediator.mediate(context);

        assertTrue(result);
        verify(context).setProperty("TOKEN_CACHE_STATUS", "SAVE_FAILED");
        assertNull(getStaticField("tokenCache"));
    }

    @Test
    public void checkMiss_setsMissAndNullAccessToken() throws Exception {
        mediator.setAction("CHECK");

        boolean result = mediator.mediate(context);

        assertTrue(result);
        verify(context).setProperty("TOKEN_CACHE_STATUS", "MISS");
        verify(context).setProperty(eq("ACCESS_TOKEN"), isNull());
    }

    @Test
    public void checkHit_setsHitAndReturnsToken() throws Exception {
        // populate static cache
        setStaticField("tokenCache", "CACHED_TOKEN");
        setStaticField("tokenCacheExpiryTime", System.currentTimeMillis() + 3600_000L);

        mediator.setAction("CHECK");

        boolean result = mediator.mediate(context);

        assertTrue(result);
        verify(context).setProperty("TOKEN_CACHE_STATUS", "HIT");
        verify(context).setProperty("ACCESS_TOKEN", "CACHED_TOKEN");
    }
}
