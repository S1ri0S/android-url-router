package com.sirios.androidurlrouter;

import java.util.Map;

/**
 * Route class that contains all necessary route information
 *
 * @author S1ri0S
 */
public class Route<T> {

    private T result;
    private String route;
    private String mappedRoute;
    private String cleanRoute; /* Route without query params */
    private Map<String, Comparable> wildcards;
    private Map<String, String> queryParams;

    public String getMappedRoute() {
        return mappedRoute;
    }

    public void setMappedRoute(String mappedRoute) {
        this.mappedRoute = mappedRoute;
    }

    public Map<String, Comparable> getWildcards() {
        return wildcards;
    }

    public void setWildcards(Map<String, Comparable> wildcards) {
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
