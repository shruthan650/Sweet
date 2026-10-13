package com.shruthan.sweet.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiAccessTokenFilterTest {

    private static final String TOKEN = "local-test-api-token-0123456789-abcd";

    @Test
    void rejectsApiRequestsWithoutTheConfiguredBearerToken() throws Exception {
        ApiAccessTokenFilter filter = new ApiAccessTokenFilter(TOKEN);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/cities");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        assertEquals("Bearer", response.getHeader("WWW-Authenticate"));
        assertNull(chain.getRequest());
    }

    @Test
    void allowsApiRequestsWithTheConfiguredBearerToken() throws Exception {
        ApiAccessTokenFilter filter = new ApiAccessTokenFilter(TOKEN);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/cities");
        request.addHeader("Authorization", "Bearer " + TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }

    @Test
    void productionFilterRequiresALongToken() {
        assertThrows(IllegalStateException.class, () -> new ApiAccessTokenFilter("short"));
    }
}
