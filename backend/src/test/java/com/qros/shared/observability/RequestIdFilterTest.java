package com.qros.shared.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

class RequestIdFilterTest {

    @Test
    void preservesSafeIncomingRequestIdAndClearsMdc() throws Exception {
        RequestIdFilter filter = new RequestIdFilter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = (req, res) -> assertThat(MDC.get("requestId")).isEqualTo("req-123");
        when(request.getHeader(RequestIdFilter.HEADER_NAME)).thenReturn("req-123");

        filter.doFilterInternal(request, response, chain);

        verify(response).setHeader(RequestIdFilter.HEADER_NAME, "req-123");
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void replacesUnsafeIncomingRequestId() throws Exception {
        RequestIdFilter filter = new RequestIdFilter();
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);
        when(request.getHeader(RequestIdFilter.HEADER_NAME)).thenReturn("bad request id");

        filter.doFilterInternal(request, response, chain);

        verify(response)
                .setHeader(
                        org.mockito.ArgumentMatchers.eq(RequestIdFilter.HEADER_NAME),
                        org.mockito.ArgumentMatchers.anyString());
        verify(chain).doFilter(request, response);
    }
}
