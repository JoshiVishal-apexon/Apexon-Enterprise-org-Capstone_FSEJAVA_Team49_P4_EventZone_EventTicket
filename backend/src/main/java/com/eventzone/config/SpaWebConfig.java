package com.eventzone.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.util.List;

/**
 * Serves the built React app (copied to classpath:/static by the Maven build)
 * from the same port as the API, so the whole app lives on one origin with no
 * dev proxy and no CORS.
 *
 * <p>React Router owns paths like /organiser and /events/{id}. Those have no
 * server-side mapping, so a plain static handler would 404 on refresh or on a
 * pasted deep link. This resolver returns index.html for any unmatched path and
 * lets the client router take over -- while explicitly refusing to do so for
 * API and infrastructure prefixes, which must keep returning real 404s rather
 * than a page of HTML.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    /** Prefixes that must never fall back to index.html. */
    private static final List<String> NON_SPA_PREFIXES = List.of(
            "api/", "actuator/", "h2-console/", "v3/api-docs", "swagger-ui");

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }

                        if (NON_SPA_PREFIXES.stream().anyMatch(resourcePath::startsWith)) {
                            return null;
                        }

                        // Absent when the backend runs without a frontend build;
                        // returning null then yields a normal 404 instead of a 500.
                        Resource index = new ClassPathResource("static/index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}
