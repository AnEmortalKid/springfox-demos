package springox.swagger.aggregate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.DocExpansion;
import springfox.documentation.swagger.web.InMemorySwaggerResourcesProvider;
import springfox.documentation.swagger.web.ModelRendering;
import springfox.documentation.swagger.web.OperationsSorter;
import springfox.documentation.swagger.web.SwaggerResource;
import springfox.documentation.swagger.web.SwaggerResourcesProvider;
import springfox.documentation.swagger.web.TagsSorter;
import springfox.documentation.swagger.web.UiConfiguration;
import springfox.documentation.swagger.web.UiConfigurationBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class SwaggerWsEndpointsConfig {

    @Autowired
    private ResourcePatternResolver resourceResolver;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.OAS_30)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build();
    }

    @Bean
    UiConfiguration uiConfig() {
        final UiConfiguration uiConf = UiConfigurationBuilder.builder()
                .deepLinking(true)
                .displayOperationId(false)
                .defaultModelsExpandDepth(1)
                .defaultModelExpandDepth(1)
                .defaultModelRendering(ModelRendering.EXAMPLE)
                .displayRequestDuration(true)
                .docExpansion(DocExpansion.NONE)
                .filter(true)
                .maxDisplayedTags(null)
                .operationsSorter(OperationsSorter.METHOD)
                .showExtensions(false)
                .showCommonExtensions(false)
                .tagsSorter(TagsSorter.ALPHA)
                // disable try it out
                .supportedSubmitMethods(UiConfiguration.Constants.NO_SUBMIT_METHODS)
                .validatorUrl(null)
                .build();
        return uiConf;
    }

    /**
     * Load api definitions and assign them a name, the name will be the name of the folder that the definition belongs. Using conventions, a swagger 2 spec should be called swagger.json. An open api spec should be openapi.json
     */
    @Primary
    @Bean
    public SwaggerResourcesProvider swaggerResourcesProvider(InMemorySwaggerResourcesProvider defaultResourcesProvider) throws IOException {
        List<SwaggerResource> resources = new ArrayList<>(defaultResourcesProvider.get());

        Resource[] swaggerResources = resourceResolver.getResources("classpath:/docs/*/swagger.json");
        logger.info("Found {} swagger files", swaggerResources.length);

        for (Resource swagger : swaggerResources) {
            String description = swagger.getDescription();
            logger.trace("Creating SwaggerResource for {}", description);

            // class path resource [docs/service_name/swagger.json
            int lastSlash = description.lastIndexOf('/');
            int previousSlash = description.lastIndexOf('/', lastSlash - 1);
            String apiName = description.substring(previousSlash + 1, lastSlash);

            SwaggerResource wsResource = new SwaggerResource();
            wsResource.setName(apiName);
            wsResource.setSwaggerVersion("2.0");
            wsResource.setLocation("/docs/" + apiName + "/swagger.json");
            resources.add(wsResource);
        }

        Resource[] openAPIResources = resourceResolver.getResources("classpath:/docs/*/openapi.json");
        logger.info("Found {} swagger files", openAPIResources.length);

        for (Resource openAPI : openAPIResources) {
            String description = openAPI.getDescription();
            logger.trace("Creating SwaggerResource for {}", description);

            // class path resource [docs/service_name/swagger.json
            int lastSlash = description.lastIndexOf('/');
            int previousSlash = description.lastIndexOf('/', lastSlash - 1);
            String apiName = description.substring(previousSlash + 1, lastSlash);

            SwaggerResource wsResource = new SwaggerResource();
            wsResource.setName(apiName);
            wsResource.setSwaggerVersion("3.0");
            wsResource.setLocation("/docs/" + apiName + "/openapi.json");
            resources.add(wsResource);
        }

        return () -> resources;
    }

    @Bean
    WebMvcConfigurer configurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/docs/**").addResourceLocations("classpath:/docs/");
            }
        };
    }
}