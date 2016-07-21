package com.sirios.androidurlrouter;

import android.content.Context;
import android.os.Bundle;

/**
 * Interface used to map routes to generic actions
 *
 * @author S1ri0S
 */
public abstract class RouterAction {

    Bundle routeArguments;

    public abstract void doAction(Context activityContext, Route route);

    public Bundle getRouteArguments() {
        return routeArguments;
    }

    public void setRouteArguments(Bundle routeArguments) {
        this.routeArguments = routeArguments;
    }

    public void addRouteArguments(Bundle args) {
        routeArguments.putBundle(Router.ROUTE_EXTRA_ARGUMENTS, args);
    }
}
