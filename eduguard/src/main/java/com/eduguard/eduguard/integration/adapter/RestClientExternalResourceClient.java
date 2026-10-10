package com.eduguard.eduguard.integration.adapter;

import com.eduguard.eduguard.integration.config.IntegrationProviderProperties;
import com.eduguard.eduguard.integration.model.IntegrationType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.net.SocketTimeoutException;
import java.util.List;

@Component
public class RestClientExternalResourceClient implements ExternalResourceClient {
    private static final ParameterizedTypeReference<List<ProviderResourceResponse>> RESOURCE_LIST =
            new ParameterizedTypeReference<>() {};
    private final RestClient.Builder restClientBuilder;

    public RestClientExternalResourceClient(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    @Override
    public List<ProviderResourceResponse> fetchResources(
            IntegrationType integration,
            IntegrationProviderProperties properties) {
        try {
            List<ProviderResourceResponse> response = client(properties)
                    .get()
                    .uri(uriBuilder -> uriBuilder.path("/resources").queryParam("limit", 50).build())
                    .headers(headers -> addCredentials(headers, properties))
                    .retrieve()
                    .body(RESOURCE_LIST);
            if (response == null) {
                throw new IntegrationCallException(IntegrationFailure.MALFORMED_RESPONSE);
            }
            return response;
        } catch (IntegrationCallException exception) {
            throw exception;
        } catch (ResourceAccessException exception) {
            throw new IntegrationCallException(isTimeout(exception)
                    ? IntegrationFailure.TIMEOUT : IntegrationFailure.UNAVAILABLE);
        } catch (RestClientResponseException exception) {
            throw new IntegrationCallException(IntegrationFailure.UNAVAILABLE);
        } catch (RestClientException exception) {
            throw new IntegrationCallException(hasCause(exception, HttpMessageNotReadableException.class)
                    ? IntegrationFailure.MALFORMED_RESPONSE : IntegrationFailure.UNAVAILABLE);
        }
    }

    @Override
    public boolean isAvailable(IntegrationType integration, IntegrationProviderProperties properties) {
        try {
            Integer status = client(properties)
                    .get()
                    .uri("/health")
                    .headers(headers -> addCredentials(headers, properties))
                    .retrieve()
                    .toBodilessEntity()
                    .getStatusCode()
                    .value();
            return status >= 200 && status < 300;
        } catch (IntegrationCallException exception) {
            return false;
        } catch (ResourceAccessException | RestClientResponseException exception) {
            return false;
        } catch (RestClientException exception) {
            return false;
        }
    }

    private RestClient client(IntegrationProviderProperties properties) {
        if (properties.getBaseUrl() == null || properties.getBaseUrl().isBlank()) {
            throw new IntegrationCallException(IntegrationFailure.UNAVAILABLE);
        }
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return restClientBuilder.clone()
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    private void addCredentials(org.springframework.http.HttpHeaders headers,
                                IntegrationProviderProperties properties) {
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            headers.setBearerAuth(properties.getApiKey());
        }
    }

    private boolean isTimeout(Throwable throwable) {
        return hasCause(throwable, SocketTimeoutException.class)
                || hasCause(throwable, HttpTimeoutException.class);
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
