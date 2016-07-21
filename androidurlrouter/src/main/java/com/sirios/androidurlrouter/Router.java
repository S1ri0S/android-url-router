package com.sirios.androidurlrouter;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class that provides routing functionality in that it associates<br/>
 * predefined routes(URLs) with activities, fragments or generic actions ({@link RouterAction})
 *
 * @author S1ri0S
 */
@SuppressWarnings("unused")
public class Router {

    public static final String ROUTE = "route";
    public static final String ROUTE_QUERY_PARAMS = "route_query_params";
    public static final String ROUTE_EXTRA_ARGUMENTS = "route_extra_arguments";
    private static final String LOG_TAG = "Router";

    public static final int FLAG_ADD_TO_BACKSTACK = 100;
    public static final int FLAG_POP_CURRENT_FRAGMENT = 101;
    public static final int FLAG_REPLACE_FRAGMENT = 103;
    public static final int FLAG_START_ACTIVITY_FOR_RESULT = 104;
    public static final int FLAG_OVERRIDE_SAME_ROUTE = 105;

    private static Router router = new Router();

    private HashMap<String, Class<? extends Activity>> activityRoutes;
    private HashMap<String, Class<? extends Fragment>> fragmentRoutes;
    private HashMap<String, RouterAction> actionRoutes;

    private Context context;
    private FragmentManager fragmentManager;
    private int fragmentContainerView;
    private int[] fragmentTransactionAnimations;

    public static String currentRoute;
    public static Bundle currentArguments;

    public Router() {
        activityRoutes = new HashMap<>();
        fragmentRoutes = new HashMap<>();
        actionRoutes = new HashMap<>();

        fragmentTransactionAnimations = new int[]{0, 0, 0, 0};
    }

    /**
     * Set this Router's application context
     *
     * @param activityContext The <STRONG>activity</STRONG> (not application) context
     * @return The Router instance to use for method chaining
     */
    public Router setContext(Context activityContext) {
        context = (Activity)activityContext;

        return this;
    }

    public Context getContext() {
        return context;
    }

    public Router setFragmentManager(FragmentManager fragmentManager) {
        this.fragmentManager = fragmentManager;
        return this;
    }

    public Router setFragmentContainerView(int fragmentContainerView) {
        this.fragmentContainerView = fragmentContainerView;
        return this;
    }

    public static synchronized Router getInstance() {
        return router;
    }

    /**
     * Define animation resources to apply to fragment transitions made by the Router
     *
     * @param enter    Animation resource when fragment enters screen
     * @param exit     Animation resource when fragment exits screen
     * @param popEnter Animation resource when fragment enters screen after the backstack is popped
     * @param popExit  Animation resource when fragment exits screen after the backstack is popped
     * @return The Router instance to use for method chaining
     */
    public Router setFragmentTransactionAnimations(int enter, int exit, int popEnter, int popExit) {

        fragmentTransactionAnimations[0] = enter;
        fragmentTransactionAnimations[1] = exit;
        fragmentTransactionAnimations[2] = popEnter;
        fragmentTransactionAnimations[3] = popExit;

        return this;
    }

    /**
     * Resets the router's properties
     *
     * @return Router for method chaining
     */
    public Router reset() {
        activityRoutes.clear();
        fragmentRoutes.clear();
        actionRoutes.clear();
        fragmentTransactionAnimations = new int[]{0, 0, 0, 0};
        currentRoute = null;
        context = null;

        return this;
    }

    /**
     * Map a route to a routable
     *
     * @param route The route to map
     * @param clazz The activity class to assign to the route
     * @return Router for method chaining
     */
    public Router registerActivityRoute(String route, Class<? extends Activity> clazz) {
        checkForDuplicates(route);
        activityRoutes.put(route, clazz);

        return this;
    }

    public Router registerActivityRoute(int routeStringRes, Class<? extends Activity> clazz) {
        return registerActivityRoute(getContext().getString(routeStringRes), clazz);
    }

    public Router registerFragmentRoute(String route, Class<? extends Fragment> clazz) {
        checkForDuplicates(route);
        fragmentRoutes.put(route, clazz);

        return this;
    }

    public Router registerFragmentRoute(int routeStringRes, Class<? extends Fragment> clazz) {
        return registerFragmentRoute(getContext().getString(routeStringRes), clazz);
    }

    public Router registerActionRoute(String route, RouterAction action) {
        checkForDuplicates(route);
        actionRoutes.put(route, action);

        return this;
    }

    public Router registerActionRoute(int routeStringRes, RouterAction action) {
        return registerActionRoute(getContext().getString(routeStringRes), action);
    }

    /**
     * Check for duplicate routes when mapping a route
     *
     * @param route The route to compare to
     */
    private void checkForDuplicates(String route) {

        for (Map.Entry<String, Class<? extends Activity>> entry : activityRoutes.entrySet()) {
            if (entry.getKey().equals(route)) {
                throw new DuplicateRouteException("A route with the name "
                        + route
                        + " already exists mapped to "
                        + entry.getValue().getSimpleName());
            }
        }

        for (Map.Entry<String, Class<? extends Fragment>> entry : fragmentRoutes.entrySet()) {
            if (entry.getKey().equals(route))
                throw new DuplicateRouteException("A route with the name "
                        + route
                        + " already exists mapped to "
                        + entry.getValue().getSimpleName());
        }

        for (Map.Entry<String, RouterAction> entry : actionRoutes.entrySet()) {
            if (entry.getKey().equals(route)) {
                throw new DuplicateRouteException("A route with the name "
                        + route
                        + " already exists mapped to "
                        + entry.getValue().getClass().getSimpleName());
            }
        }
    }

    private void checkCanSupportFragmentTransactions() {
        if (fragmentManager == null) {
            throw new IllegalStateException("You haven't provided a fragment manager. Use Router.setFragmentManager");
        }
        if (fragmentContainerView == 0) {
            throw new IllegalStateException("You haven't provided a fragment container view id. Use Router.setFragmentContainerView");
        }
    }

    /**
     * Execute the appropriate routable for the given route.<br/>
     * For use with Fragment routables, allows addToBackStack and popCurrent parameters.
     *
     * @param route The route to execute
     * @param args  Extra arguments you wish to pass as fragment arguments or intent data to the Routable object
     * @param flags Flags for signaling specific required actions (e.g. adding a fragment transaction to the backstack)
     */
    public void execRoute(String route, Bundle args, int... flags) {

        Route resolvedRoute;
        resolvedRoute = resolveRoute(route);
        List<Integer> activeFlags = new ArrayList<>();

        Log.d("Router:", "Route -> " + route);

        if (flags.length > 0) {
            for (int i : flags) {
                activeFlags.add(i);
            }
        }

        /* If it's the same route don't do anything unless FLAG_OVERRIDE_SAME_ROUTE is given */
        if (currentRoute != null && currentRoute.equals(route) && !activeFlags.contains(FLAG_OVERRIDE_SAME_ROUTE)) {
            return;
        }

        if (resolvedRoute != null) {
            resolvedRoute.setRoute(route);

            currentRoute = route;
            currentArguments = args;

            if (resolvedRoute instanceof ActionRoute) {
                ActionRoute actionRoute = ((ActionRoute) resolvedRoute);
                actionRoute.getResult().setRouteArguments(new Bundle());

                if (args != null) {
                    actionRoute.getResult().addRouteArguments(args);
                }

                for (Map.Entry<String, String> entry : actionRoute.getWildcards().entrySet()) {
                    actionRoute.getResult().getRouteArguments().putString(entry.getKey(), entry.getValue());
                }

                Bundle queryParams = new Bundle();
                for (Map.Entry<String, String> entry : actionRoute.getQueryParams().entrySet()) {
                    queryParams.putString(entry.getKey(), entry.getValue());
                }
                actionRoute.getResult().getRouteArguments().putBundle(ROUTE_QUERY_PARAMS, queryParams);

                Log.d(LOG_TAG, "Executing router action " + actionRoute.getResult().getClass().getSimpleName());
                actionRoute.getResult().doAction(context, resolvedRoute);

            } else if (resolvedRoute instanceof FragmentRoute) {
                checkCanSupportFragmentTransactions();

                Fragment fragment = assembleFragment(resolvedRoute);

                if (args != null) {
                    Bundle fragArgs = fragment.getArguments();
                    fragArgs.putAll(args);
                    fragment.setArguments(fragArgs);
                }

                if (fragmentManager.getBackStackEntryCount() == 0 && activeFlags.contains(FLAG_ADD_TO_BACKSTACK)) {
                    Log.w(LOG_TAG, "Backstack is empty. If this is the first fragment in the activity's view hierarchy," +
                            " perhaps you shouldn't add it to the backstack.");
                }
                Log.d("Router: ", "Initiating fragment " + fragment.getClass().getName());

                if (activeFlags.contains(FLAG_POP_CURRENT_FRAGMENT)) {
                    fragmentManager.popBackStackImmediate();
                }

                FragmentTransaction transaction = fragmentManager.beginTransaction();
                transaction.setCustomAnimations(fragmentTransactionAnimations[0], fragmentTransactionAnimations[1],
                        fragmentTransactionAnimations[2], fragmentTransactionAnimations[3]);


                if (activeFlags.contains(FLAG_REPLACE_FRAGMENT)) {
                    transaction.replace(fragmentContainerView, fragment);
                } else {
                    transaction.add(fragmentContainerView, fragment);
                }

                if (activeFlags.contains(FLAG_ADD_TO_BACKSTACK)) {
                    transaction.addToBackStack(route + "_" + String.valueOf(System.currentTimeMillis()));
                }

                transaction.commit();

            } else if (resolvedRoute instanceof ActivityRoute) {
                Intent intent = assembleIntent(resolvedRoute, args);
                context.startActivity(intent);
            }
        } else {
            throw new RouteNotFoundException("The provided route: " + route + " is not mapped");
        }
    }

    /**
     * Calls {@link Router#execRoute(String, Bundle, int...)} with no extra arguments.
     *
     * @param route The route to execute
     */
    public void execRoute(String route, int... flags) {
        execRoute(route, null, flags);
    }

    /**
     * Get the appropriate regexp for the given mapped route.
     *
     * @param mappedRoute   The mapped route
     * @param addLineBounds Include line bounds (^$) in the regex
     * @return The regex for the mapped route
     */
    private String createMappedRouteRegex(String mappedRoute, boolean addLineBounds) {
        String stringWildcardRegex = "s:\\{[^(/|#|!)]+\\}";
        String integerWildcardRegex = "i:\\{[^(/|#|!)]+\\}";
        String stringFixedRegex = "[^(/|#|!|?)]+";
        String integerFixedRegex = "(\\\\d+)";
        StringBuilder regexBuilder = new StringBuilder();
        if (addLineBounds) {
            //regexBuilder.append("^" + mappedRoute + "(\\/(\\w+|\\-)+)?$");
            regexBuilder
                    .append("^")
                    .append(mappedRoute)
                    .append("(\\/([a-zA-Z]|\\-)+)?$");
        } else {
            regexBuilder
                    .append("(")
                    .append(mappedRoute)
                    .append(")");
        }
        String regex = regexBuilder.toString();
        regex = regex.replaceAll(integerWildcardRegex, integerFixedRegex);
        regex = regex.replaceAll(stringWildcardRegex, stringFixedRegex);
        regex = regex.replaceAll("\\.", "\\\\.");
        regex = regex.replaceAll("\\{", "\\\\{");
        regex = regex.replaceAll("\\}", "\\\\}");

        return regex;
    }

    private Intent assembleIntent(Route resolvedRoute, Bundle args) {
        ActivityRoute route = (ActivityRoute) resolvedRoute;
        Intent intent = new Intent(context, route.getResult());
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (null != args) {
            intent.putExtra(ROUTE_EXTRA_ARGUMENTS, args);
        }

        if (route.getWildcards() != null && !route.getWildcards().isEmpty()) {
            Map<String, String> wc = route.getWildcards();
            for (Map.Entry<String, String> entry : wc.entrySet()) {
                intent.putExtra(entry.getKey(), entry.getValue());
            }
        }

        Bundle qparams = new Bundle();
        if (route.getQueryParams() != null && !route.getQueryParams().isEmpty()) {
            Map<String, String> qp = route.getQueryParams();
            for (Map.Entry<String, String> entry : qp.entrySet()) {
                qparams.putString(entry.getKey(), entry.getValue());
            }
        }
        intent.putExtra(ROUTE_QUERY_PARAMS, qparams);

        return intent;
    }

    /**
     * Assemble the fragment assigning wildcards and query parameters where available
     *
     * @return The fragment to instantiate
     */
    private Fragment assembleFragment(Route resolvedRoute) {

        FragmentRoute fragRoute = ((FragmentRoute) resolvedRoute);
        Fragment frag = Fragment.instantiate(context, fragRoute.getResult().getName());
        Bundle args = new Bundle();

        if (fragRoute.getWildcards() != null && !fragRoute.getWildcards().isEmpty()) {
            Map<String, String> wc = fragRoute.getWildcards();
            for (Map.Entry<String, String> entry : wc.entrySet()) {
                args.putString(entry.getKey(), entry.getValue());
            }
        }

        if (fragRoute.getQueryParams() != null && !fragRoute.getQueryParams().isEmpty()) {
            Bundle queryParams = new Bundle();
            Map<String, String> qp = fragRoute.getQueryParams();
            for (Map.Entry<String, String> entry : qp.entrySet()) {
                queryParams.putString(entry.getKey(), entry.getValue());
            }
            args.putBundle(ROUTE_QUERY_PARAMS, queryParams);
        }

        args.putString(ROUTE, resolvedRoute.getRoute());
        frag.setArguments(args);

        return frag;
    }

    /**
     * Create an appropriate Route object for the given route
     *
     * @param route The given route
     * @return true if Route object valid, false otherwise
     */
    private Route resolveRoute(String route) {
        String queryRegex = "\\?((\\w+=[\\w|,]+)&?)+$";
        Pattern queryPattern = Pattern.compile(queryRegex);
        Matcher queryMatcher = queryPattern.matcher(route);
        Map<String, String> queryParams = new HashMap<>();

        if (queryMatcher.find()) {
            queryParams = resolveQueryParams(route.substring(queryMatcher.start() + 1, queryMatcher.end()));
            route = route.replaceAll(queryRegex, "");
        }

        for (Map.Entry<String, Class<? extends Activity>> entry : activityRoutes.entrySet()) {
            String regex = createMappedRouteRegex(entry.getKey(), true);

            if (route.matches(regex)) {
                ActivityRoute resolvedRoute = new ActivityRoute();

                resolvedRoute.setQueryParams(queryParams);
                resolvedRoute.setMappedRoute(entry.getKey());
                resolvedRoute.setResult(entry.getValue());
                resolvedRoute.setWildcards(resolveWildcards(route, entry.getKey()));

                return resolvedRoute;
            }
        }

        for (Map.Entry<String, Class<? extends Fragment>> entry : fragmentRoutes.entrySet()) {
            String regex = createMappedRouteRegex(entry.getKey(), true);

            if (route.matches(regex)) {
                FragmentRoute resolvedRoute = new FragmentRoute();

                resolvedRoute.setQueryParams(queryParams);
                resolvedRoute.setMappedRoute(entry.getKey());
                resolvedRoute.setResult(entry.getValue());
                resolvedRoute.setWildcards(resolveWildcards(route, entry.getKey()));

                return resolvedRoute;
            }
        }

        for (Map.Entry<String, RouterAction> entry : actionRoutes.entrySet()) {
            String regex = createMappedRouteRegex(entry.getKey(), true);

            if (route.matches(regex)) {
                ActionRoute resolvedRoute = new ActionRoute();

                resolvedRoute.setQueryParams(queryParams);
                resolvedRoute.setMappedRoute(entry.getKey());
                resolvedRoute.setResult(entry.getValue());
                resolvedRoute.setWildcards(resolveWildcards(route, entry.getKey()));

                return resolvedRoute;
            }
        }

        return null;
    }

    /**
     * Extract the query parameters from the provided route if any.
     *
     * @param queryString The query string of the url
     * @return Map containing query parameters in key - value format
     */
    private HashMap<String, String> resolveQueryParams(String queryString) {
        HashMap<String, String> params = new HashMap<>();
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
     * @param route       The given route
     * @param mappedRoute The mapped route
     * @return Wildcard key - value pairs
     */
    private Map<String, String> resolveWildcards(String route, String mappedRoute) {
        Map<String, String> wc = new HashMap<>();

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
     * @return True if route contains wildcards, false otherwise
     */
    private boolean routeHasWildCards(String route) {
        String wildcardRegex = "\\{\\w+\\}";
        Pattern wcPat = Pattern.compile(wildcardRegex);
        Matcher wcMatch = wcPat.matcher(route);

        return wcMatch.find();
    }

    public boolean isValidRoute(String route) {
        return resolveRoute(route) != null;
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
