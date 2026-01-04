package com.dariom.wds.config;

import static com.dariom.wds.config.TraceIdFilter.TRACE_ID_HEADER;
import static com.dariom.wds.config.TraceIdFilter.TRACE_ID_MDC_KEY;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {

  private final TraceIdFilter filter = new TraceIdFilter();

  @Test
  void doFilter_missingHeader_generatesTraceIdAndCleansUpMdc() throws Exception {
    // Arrange
    var request = new MockHttpServletRequest("GET", "/health");
    var response = new MockHttpServletResponse();

    FilterChain chain = (req, res) -> {
      var traceIdInMdc = MDC.get(TRACE_ID_MDC_KEY);
      assertThat(traceIdInMdc).isNotBlank();
      assertThat(((MockHttpServletResponse) res).getHeader(TRACE_ID_HEADER)).isEqualTo(
          traceIdInMdc);
    };

    // Act
    filter.doFilter(request, response, chain);

    // Assert
    assertThat(MDC.get(TRACE_ID_MDC_KEY)).isNull();
    assertThat(response.getHeader(TRACE_ID_HEADER)).isNotBlank();
  }

  @Test
  void doFilter_existingHeader_propagatesSameTraceId() throws ServletException, IOException {
    // Arrange
    var request = new MockHttpServletRequest("GET", "/health");
    request.addHeader(TRACE_ID_HEADER, "trace-123");

    var response = new MockHttpServletResponse();

    FilterChain chain = (req, res) -> {
      assertThat(MDC.get(TRACE_ID_MDC_KEY)).isEqualTo("trace-123");
      assertThat(((MockHttpServletResponse) res).getHeader(TRACE_ID_HEADER)).isEqualTo("trace-123");
    };

    // Act
    filter.doFilter(request, response, chain);

    // Assert
    assertThat(MDC.get(TRACE_ID_MDC_KEY)).isNull();
  }
}
