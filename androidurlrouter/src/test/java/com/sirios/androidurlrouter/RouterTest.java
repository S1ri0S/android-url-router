package com.sirios.androidurlrouter;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
@RunWith(PowerMockRunner.class)
public class RouterTest {

    private Router router;

    @Before
    public void initRouter() {
        router = Router.getInstance();
        router.registerActionRoute("app://www.app.com/profile/settings/me/s:{slug}", new MockAction1());
        router.registerFragmentRoute("app://www.app.com/laws/i:{lawId}/articles/i:{articleId}", MockFragment1.class);
        router.registerActivityRoute("app://www.app.com/articles/i:{articleId}/related", MockActivity1.class);

    }

    @After
    public void resetRouter() {
        router.reset();
    }

    @Test
    public void testFragmentRoute() throws Exception {
        Router.RouteMatch routeMatch = router
                .checkRouteKeys("app://www.app.com/laws/1981/articles/14563/lala-lala-la?order=desc&bn=false",
                        router.getFragmentRoutes().keySet());
        Map<String, Comparable> args = routeMatch.getArguments();
        Route route = router.resolveRoute("app://www.app.com/laws/1981/articles/14563?order=desc&bn=false");

        assertEquals(routeMatch.getMatchedRoute(), "app://www.app.com/laws/i:{lawId}/articles/i:{articleId}");
        assertTrue(args.containsKey("lawId"));
        assertTrue(args.containsKey("articleId"));
        assertEquals(args.get("lawId"), 1981);
        assertEquals(args.get("articleId"), 14563);

        assertEquals(route.getResult(), MockFragment1.class);
        assertEquals(route.getCleanRoute(), "app://www.app.com/laws/1981/articles/14563?order=desc&bn=false");

    }

    @Test
    public void testActivityRoute() throws Exception {
        Route route = router.resolveRoute("app://www.app.com/articles/13746/related?page=1");

        assertEquals(route.getResult(), MockActivity1.class);
        assertEquals(route.getCleanRoute(), "app://www.app.com/articles/13746/related?page=1");
        assertTrue(route.getWildcards().containsKey("articleId"));
        assertEquals(route.getWildcards().get("articleId"), 13746);
        assertTrue(route.getQueryParams().containsKey("page"));
        assertEquals(route.getQueryParams().get("page"), "1");
    }

    @Test(expected = IllegalStateException.class)
    public void testExecRoute() throws Exception {
        router.execRoute("app://www.app.com/laws/1981/articles/14563/lala-lala-la?order=desc&bn=false");
    }

    @Test
    public void checkValidRoute() throws Exception {
        assertTrue(router.isValidRoute("app://www.app.com/laws/1981/articles/14563/lala-lala-la?order=desc&bn=false"));
        assertTrue(router.isValidRoute("app://www.app.com/articles/13746/related?page=1"));
    }

    @Test(expected = Router.DuplicateRouteException.class)
    public void checkDuplicateRouteException1() throws Exception {
        router.registerFragmentRoute("app://www.app.com/articles/i:{articleId}/related", MockFragment1.class);
    }

    @Test(expected = Router.DuplicateRouteException.class)
    public void checkDuplicateRouteException2() throws Exception {
        router.registerActivityRoute("app://www.app.com/laws/i:{lawId}/articles/i:{articleId}", MockActivity1.class);
    }

    @Test(expected = Router.DuplicateRouteException.class)
    public void checkDuplicateRouteException3() throws Exception {
        router.registerFragmentRoute("app://www.app.com/profile/settings/me/s:{slug}", MockFragment3.class);
    }

    public static class MockActivity1 extends Activity {
    }

    public static class MockFragment1 extends Fragment {
    }

    public static class MockAction1 extends RouterAction {
        @Override
        public void doAction(Context context, Route route) {
            // NOTHING
        }
    }

    public static class MockFragment3 extends Fragment {
    }

    public static class MockFragment4 extends Fragment {
    }
}