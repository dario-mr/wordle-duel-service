package com.dariom.wds.config.swagger;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.dariom.wds.config.security.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer;
import org.springdoc.webmvc.ui.SwaggerIndexTransformer;
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;
import org.springframework.web.servlet.resource.ResourceTransformerChain;
import org.springframework.web.servlet.resource.TransformedResource;

/**
 * Customizes the Springdoc Swagger UI to support application-specific CSRF settings.
 *
 * <p>Springdoc’s built-in CSRF support assumes the default {@code XSRF-TOKEN} cookie
 * and {@code X-XSRF-TOKEN} header. This configuration post-processes the generated
 * {@code swagger-initializer.js} to replace those defaults with the application’s custom CSRF
 * cookie and header names, ensuring that “Try it out” requests from Swagger UI include the correct
 * CSRF header.</p>
 */
@Configuration
public class SwaggerUiCsrfConfig {

  @Bean
  public SwaggerIndexTransformer swaggerIndexTransformer(
      SwaggerUiConfigProperties swaggerUiConfigProperties,
      SwaggerUiOAuthProperties swaggerUiOAuthProperties,
      SwaggerWelcomeCommon swaggerWelcomeCommon,
      ObjectMapperProvider objectMapperProvider,
      SecurityProperties securityProperties
  ) {
    return new CsrfSwaggerIndexTransformer(
        swaggerUiConfigProperties,
        swaggerUiOAuthProperties,
        swaggerWelcomeCommon,
        objectMapperProvider,
        securityProperties
    );
  }

  static String patchCsrfTokenNames(String js, String cookieName, String headerName) {
    if (js == null) {
      return null;
    }

    if (!js.contains("XSRF-TOKEN") && !js.contains("X-XSRF-TOKEN")) {
      return js;
    }

    return js.replace("XSRF-TOKEN", cookieName)
        .replace("X-XSRF-TOKEN", headerName);
  }

  static final class CsrfSwaggerIndexTransformer extends SwaggerIndexPageTransformer {

    private final SecurityProperties securityProperties;

    CsrfSwaggerIndexTransformer(
        SwaggerUiConfigProperties swaggerUiConfig,
        SwaggerUiOAuthProperties swaggerUiOAuthProperties,
        SwaggerWelcomeCommon swaggerWelcomeCommon,
        ObjectMapperProvider objectMapperProvider,
        SecurityProperties securityProperties) {
      super(swaggerUiConfig, swaggerUiOAuthProperties, swaggerWelcomeCommon, objectMapperProvider);
      this.securityProperties = securityProperties;
    }

    @Override
    public Resource transform(HttpServletRequest request, Resource resource,
        ResourceTransformerChain transformerChain)
        throws IOException {
      var transformed = super.transform(request, resource, transformerChain);

      // only patch the swagger initializer js
      var filename = resource.getFilename();
      if (filename == null || !filename.equals("swagger-initializer.js")) {
        return transformed;
      }

      String js;
      try (var is = transformed.getInputStream()) {
        js = StreamUtils.copyToString(is, UTF_8);
      }

      var csrf = securityProperties.csrf();
      if (csrf == null || csrf.cookieName() == null || csrf.headerName() == null) {
        return transformed;
      }

      var patched = SwaggerUiCsrfConfig.patchCsrfTokenNames(js, csrf.cookieName(),
          csrf.headerName());
      if (patched.equals(js)) {
        return transformed;
      }

      return new TransformedResource(transformed, patched.getBytes(UTF_8));
    }
  }
}
