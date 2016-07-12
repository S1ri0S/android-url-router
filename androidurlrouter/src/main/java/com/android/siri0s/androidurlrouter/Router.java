package com.android.siri0s.androidurlrouter;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that provides routing functionality in that it associates
 * predefined routes with predefined fragments <br/> (will also add activities in the future)
 * thus rendering correct fragment instantiation painless
 *
 * @author Georgios Sofronas
 *
 */
public class Router {

	public static final String ROUTE = "route";
	public static final String ROUTE_ARGS = "routeArgs";

	private static Router router = new Router();
	private HashMap<String, Class<? extends Fragment>> routes;
    private HashMap<String, RouterAction> routeActions;
    private Route resolvedRoute;
	private Context context;
	private int[] mAnimationResources;
    public static String currentRoute;
    private ArrayList<String> routeHistory;

    private Class<? extends Fragment> fallbackFragment;

    public static String currentFrag;
    public static Bundle currentFragArgs;

	public Router() {
		routes = new HashMap<String, Class<? extends Fragment>>();
        routeActions = new HashMap<String, RouterAction>();
        mAnimationResources = new int[] {0, 0, 0, 0};
        routeHistory = new ArrayList<>();
    }

	/**
	 * Set this Router's application context
	 *
	 * @param context The application context
	 *
	 * @return The Router instance to use for method chaining
	 */
	public Router setContext(Context context) {
		this.context = context.getApplicationContext();

		return this;
	}

	public Context getContext() {
		return this.context;
	}

	public HashMap<String, Class<? extends Fragment>> getRoutes() {
		return this.routes;
	}

	public static synchronized Router sharedRouter() {
		return router;
	}

	/**
	 * Define animation resources to apply to fragment transitions made by the Router
	 *
	 * @param enter Animation resource when fragment enters screen
	 * @param exit Animation resource when fragment exits screen
	 * @param popEnter Animation resource when fragment enters screen after the backstack is popped
	 * @param popExit Animation resource when fragment exits screen after the backstack is popped
	 *
	 * @return The Router instance to use for method chaining
	 */
	public Router setFragmentAnimations(int enter, int exit, int popEnter, int popExit) {

		mAnimationResources[0] = enter;
		mAnimationResources[1] = exit;
		mAnimationResources[2] = popEnter;
		mAnimationResources[3] = popExit;

		return this;
	}

    /* Call before instantiating */
    public Router reset() {
        routes.clear();
        routeActions.clear();
        mAnimationResources = new int[] {0, 0, 0, 0};
        currentRoute = null;
        currentFrag = null;
        context = null;
        fallbackFragment = null;

        return this;
    }

	/**
	 * Map a route to a fragment
	 *
	 * @param route The route to map
	 * @param fragmentClazz The fragment class associated with the route
	 */
	public Router mapRoute(String route, Class<? extends Fragment> fragmentClazz) {
        checkForDuplicates(route);
		routes.put(route, fragmentClazz);

		return this;
	}

	public Router mapRoute(int routeStringRes, Class<? extends Fragment> fragmentClazz) {
		String route = getContext().getString(routeStringRes);

		checkForDuplicates(route);

		routes.put(route, fragmentClazz);

		return this;
	}

    public Router mapRouteAction(String route, RouterAction action) {
        checkForDuplicates(route);
        routeActions.put(route, action);

        return this;
    }

    public Router mapRouteAction(int routeStringRes, RouterAction action) {
        String route = getContext().getResources().getString(routeStringRes);
        checkForDuplicates(route);
        routeActions.put(route, action);

        return this;
    }

	private void checkForDuplicates(String route) {

		for (Map.Entry<String, Class<? extends Fragment>> entry : routes.entrySet()) {
            if (entry.getKey().equals(route)) {
                //throw new DuplicateRouteException("A route with the name " + route + " already exists");
            }
		}

        for (Map.Entry<String, RouterAction> entry : routeActions.entrySet()) {
            if (entry.getKey().equals(route))
                throw new DuplicateRouteException("A route with the name " + route + " already exists");
        }
	}

	/**
	 * Attach the fragment associated with the route
	 * to the calling activity's view hierarchy
	 *
	 * @param route The route to execute
	 * @param fragManager The fragment manager instance of the calling activity
	 * @param containerView The container view to which the fragment is attached
	 * @param addToBackStack True if you want the fragment transaction to be added to the backstack, false otherwise
	 * @param extraFragArgs Fragment arguments you may wish to add to the fragment to be instantiated
     * @param popCurrent True if you wish current fragment to be popped from the backstack, false otherwise
	 */
	public void execRoute(String route, FragmentManager fragManager, int containerView, boolean addToBackStack,
                          Bundle extraFragArgs, boolean popCurrent) {

        if (currentRoute != null && currentRoute.equals(route)) {
            return;
        }

        Fragment fragment;
        resolvedRoute = new Route();
        boolean found = resolveRoute(route);
        resolvedRoute.setRoute(route);

        if (resolvedRoute instanceof ActionRoute) {
            ActionRoute r = (ActionRoute) resolvedRoute;

            if (extraFragArgs != null) {
                for (String entry : extraFragArgs.keySet()) {
                    r.getWildcards().put(entry, extraFragArgs.getString(entry));
                }
            }

            r.getResult().doAction(context, r);

            return;
        }

        if (found) {
            fragment = assembleFragment();
            currentFrag = fragment.getClass().getSimpleName();
        } else {
            if (fallbackFragment != null) {
                resolvedRoute = new FragmentRoute();
                resolvedRoute.setRoute(route);
                resolvedRoute.setResult(fallbackFragment);
                fragment = assembleFragment();
            } else {
                throw new RouteNotFoundException("The route ("+route+") is not registered and there is no fallback");
            }
        }

		if (fragment != null && extraFragArgs != null) {

			Bundle fragArgs = fragment.getArguments();
			fragArgs.putAll(extraFragArgs);
			fragment.setArguments(fragArgs);
            currentFragArgs = extraFragArgs;
		} else {
            currentFragArgs = null;
        }

		if (fragment != null) {

			Log.d("Router: ", "Initiating fragment " + fragment.getClass().getName());
			Log.d("Router:", "Route -> " + route);

            if (popCurrent) {
                fragManager.popBackStackImmediate();
            }

            /*fragManager.beginTransaction()
                    .setCustomAnimations(mAnimationResources[0], mAnimationResources[1],
                            mAnimationResources[2], mAnimationResources[3])
                    .setCustomAnimations(mAnimationResources[0], mAnimationResources[1])
                    .add(containerView, fragment)
                    .addToBackStack(route)
                    .commit();*/

			if (addToBackStack) {

				fragManager.beginTransaction()
						   .setCustomAnimations(mAnimationResources[0], mAnimationResources[1],
								   mAnimationResources[2], mAnimationResources[3])
						   .replace(containerView, fragment)
						   .addToBackStack(route)
						   .commit();
			} else {

				fragManager.beginTransaction()
						   .setCustomAnimations(mAnimationResources[0], mAnimationResources[1],
								   mAnimationResources[2], mAnimationResources[3])
						   .replace(containerView, fragment)
						   .commit();
			}

            currentRoute = route;
            routeHistory.add(route);

        } else
			//throw new RouteNotFoundException("The route '" + route + "' does not exist");
            Toast.makeText(context, "Route is either unimplemented or not connected", Toast.LENGTH_SHORT).show();
	}

    public void execRoute(String route, FragmentManager fragManager, int containerView, boolean addToBackStack, Bundle extraArgs) {

        execRoute(route, fragManager, containerView, addToBackStack, extraArgs, false);
    }

	/**
	 * Calls {@link #execRoute(String, FragmentManager, int, boolean, Bundle, boolean)} with addToBackStack set to false
	 * and no extra fragment arguments
	 */
	public void execRoute(String route, FragmentManager fragManager, int containerView) {

		execRoute(route, fragManager, containerView, false, null, false);
	}

	/**
	 * Calls {@link #execRoute(String, FragmentManager, int, boolean, Bundle, boolean)} with addToBackStack set to false
	 */
	public void execRoute(String route, FragmentManager fragManager, int containerView, Bundle extraFragArgs) {

		execRoute(route, fragManager, containerView, false, extraFragArgs, false);
	}

	/**
	 * Calls {@link #execRoute(String, FragmentManager, int, boolean, Bundle, boolean)} with no extra fragment arguments
	 */
	public void execRoute(String route, FragmentManager fragManager, int containerView, boolean addToBackStack) {

		execRoute(route, fragManager, containerView, true, null, false);
	}

    /**
     * Get the appropriate regexp for the given mapped route.
     *
     * @param mappedRoute The mapped route
     * @param addLineBounds Include line bounds (^$) in the regex
     *
     * @return The regex for the mapped route
     */
    private String createMappedRouteRegex(String mappedRoute, boolean addLineBounds) {
        String wildcardRegex = "\\{\\w+\\}";
        //String fixedRegex = "([\\\\w|\\\\-|_]+|\\\\d+)";
        String fixedRegex = "([\\\\w|\\\\-|_\\\\.]+|\\\\d+)";
        StringBuilder regexBuilder = new StringBuilder();
        if (addLineBounds) {
            //regexBuilder.append("^" + mappedRoute + "(\\/(\\w+|\\-)+)?$");
            regexBuilder.append("^" + mappedRoute + "(\\/([a-zA-Z0-9]|\\-)+)?$");
        } else {
            regexBuilder.append("(" + mappedRoute + ")");
        }
        String regex = regexBuilder.toString();
        regex = regex.replaceAll(wildcardRegex, fixedRegex);
        regex = regex.replaceAll("\\.", "\\\\.");
        //regex = regex.replaceAll("\\?", "\\\\?");
        //System.out.println(regex);

        return regex;
    }

    private Fragment assembleFragment() {
        FragmentRoute fragRoute = (FragmentRoute) resolvedRoute;
        Fragment frag = Fragment.instantiate(context, fragRoute.getResult().getName());
        Bundle args = new Bundle();

        if (fragRoute.getWildcards() != null && !fragRoute.getWildcards().isEmpty()) {
            for (Map.Entry<String, String> entry : fragRoute.getWildcards().entrySet()) {
                args.putString(entry.getKey(), entry.getValue());
            }
        }

        if (fragRoute.getQueryParams() != null && !fragRoute.getQueryParams().isEmpty()) {
            for (Map.Entry<String, String> entry : fragRoute.getQueryParams().entrySet()) {
                args.putString("query_" + entry.getKey(), entry.getValue());
            }
        }

        args.putString(ROUTE, fragRoute.getRoute());
        frag.setArguments(args);

        return frag;
    }

    /**
     * Create an appropriate Route object
     *
     * @param route The given route
     * @return true if Route object valid, false otherwise
     */
    private boolean resolveRoute(String route) {
        String queryRegex = "\\?((\\w+=[\\w|,]+)&?)+$";
        Pattern queryPattern = Pattern.compile(queryRegex);
        Matcher queryMatcher = queryPattern.matcher(route);
        boolean routeFound = false;
        resolvedRoute.setMappedRoute("");

        if (queryMatcher.find()) {
            resolvedRoute.setQueryParams(resolveQueryParams(route
                    .substring(queryMatcher.start() + 1, queryMatcher.end())));
            route = route.replaceAll(queryRegex, "");
        }
        resolvedRoute.setCleanRoute(route);

        for (Map.Entry<String, Class<? extends Fragment>> entry : routes.entrySet()) {
            String regex = createMappedRouteRegex(entry.getKey(), true);

            if (route.matches(regex)) {
                Map<String, String> queries = resolvedRoute.getQueryParams();
                resolvedRoute = new FragmentRoute();

                routeFound = true;
                resolvedRoute.setQueryParams(queries);
                resolvedRoute.setMappedRoute(entry.getKey());
                resolvedRoute.setResult(entry.getValue());
                resolvedRoute.setWildcards(resolveWildcards(route, entry.getKey()));
                break;
            }
        }

        for (Map.Entry<String, RouterAction> entry : routeActions.entrySet()) {
            String regex = createMappedRouteRegex(entry.getKey(), true);

            if (route.matches(regex)) {
                resolvedRoute = new ActionRoute();

                routeFound = true;
                resolvedRoute.setMappedRoute(entry.getKey());
                resolvedRoute.setResult(entry.getValue());
                resolvedRoute.setWildcards(resolveWildcards(route, entry.getKey()));
                break;
            }
        }

        return resolvedRoute.getMappedRoute().length() > 0 && resolvedRoute.getResult() != null;
    }

    /**
     * Extract the query parameters from the provided route if any.
     *
     * @param queryString The query string of the url
     * @return Map containing query parameters in key - value format
     */
    private HashMap<String, String> resolveQueryParams(String queryString) {
        HashMap<String, String> params = new HashMap<String, String>();
        String[] qparams = queryString.split("&");

        for (String p : qparams) {
            String key = p.split("=")[0];
            String val = p.split("=")[1];
            params.put(key, val);
        }

        return params;
    }

    /**
     * Extract the wildcard names and values and return them as Map of key-value pairs
     *
     * @param route The given route
     * @param mappedRoute The mapped route
     * @return Wildcard key - value pairs
     */
    private Map<String, String> resolveWildcards(String route, String mappedRoute) {
        Map<String, String> wc = new HashMap<String, String>();

        if (!routeHasWildCards(mappedRoute)) {
            return wc;
        }

        String wildcardRegex = "(\\{\\w+\\})";
        String fixedRegex = createMappedRouteRegex(mappedRoute, false);

        Pattern wcp = Pattern.compile(wildcardRegex);
        Pattern fxp = Pattern.compile(fixedRegex);

        Matcher wcm = wcp.matcher(mappedRoute);
        Matcher fxm = fxp.matcher(route);

        fxm.find();
        int cnt = 1;
        while (wcm.find()) {
            String var = mappedRoute.substring(wcm.start() + 1, wcm.end() - 1);
            String value = fxm.group(cnt + 1);
            wc.put(var, value);
            cnt++;
        }

        return wc;
    }

    /**
     * Check if given route contains wildcards
     *
     * @param route The mapped route
     * @return
     */
    private boolean routeHasWildCards(String route) {
        String wildcardRegex = "\\{\\w+\\}";
        Pattern wcPat = Pattern.compile(wildcardRegex);
        Matcher wcMatch = wcPat.matcher(route);

        return wcMatch.find();
    }

    public void popRouteHistory() {
        routeHistory.remove(routeHistory.size() - 1);
    }

    public Class<? extends Fragment> getFallbackFragment() {
        return fallbackFragment;
    }

    public Router setFallbackFragment(Class<? extends Fragment> fallbackFragment) {
        this.fallbackFragment = fallbackFragment;
        return this;
    }

    public boolean isValidRoute(String route) {
        return resolveRoute(route);
    }

    /**
	 * Thrown if a given route is not found.
	 */
	@SuppressWarnings("serial")
	public static class RouteNotFoundException extends RuntimeException {

		public RouteNotFoundException(String message) {
			super(message);
		}
	}

	/**
	 * Thrown if a route is duplicate
	 */
	@SuppressWarnings("serial")
	public static class DuplicateRouteException extends RuntimeException {

		public DuplicateRouteException(String message) {
			super(message);
		}
	}

}
