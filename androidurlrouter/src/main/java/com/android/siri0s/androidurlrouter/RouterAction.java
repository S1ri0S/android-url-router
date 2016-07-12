package com.android.siri0s.androidurlrouter;

import android.content.Context;

/**
 * Interface used to map routes to generic actions
 *
 * @author S1ri0S
 */
public interface RouterAction {

    void doAction(Context activityContext, ActionRoute actionRoute);
}
