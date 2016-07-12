package com.android.siri0s.androidurlrouter;

import java.util.Map;

/**
 * Created by siri0s on 3/31/16.
 */
public class Route<T> {

    private T result;
    private String route;
    private String mappedRoute;
    private String cleanRoute; /* Route without query params */
    private Map<String, String> wildcards;
    private Map<String, String> queryParams;

    public String getMappedRoute() {
        return mappedRoute;
    }

    public void setMappedRoute(String mappedRoute) {
        this.mappedRoute = mappedRoute;
    }

    public Map<String, String> getWildcards() {
        return wildcards;
    }

    public void setWildcards(Map<String, String> wildcards) {
        this.wildcards = wildcards;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public T getResult() {
        return result;
    }

    public void setResult(T result) {
        this.result = result;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getCleanRoute() {
        return cleanRoute;
    }

    public void setCleanRoute(String cleanRoute) {
        this.cleanRoute = cleanRoute;
    }
}
