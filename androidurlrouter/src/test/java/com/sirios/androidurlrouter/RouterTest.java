package com.sirios.androidurlrouter;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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
        router.registerActionRoute("app://www.app.com/files/i:{fileId}", new MockAction1());
        router.registerFragmentRoute("app://www.app.com/laws/i:{lawId}/articles/i:{articleId}", MockFragment1.class);
        router.registerFragmentRoute("app://www.app.com/pdfViewer/s:{filename}", MockFragment3.class);
        router.registerActivityRoute("app://www.app.com/articles/i:{articleId}/related", MockActivity1.class);
        router.registerFragmentRoute("app://www.app.com/webview", MockFragment4.class);

        router
                .registerFragmentRoute("app://www.app.com/laws/notes/article/i:{articleRevisionId}/s:{noteNumber}", MockFragment1.class)
                .registerFragmentRoute("app://www.app.com/laws/notes/paragraph/i:{paragraphRevisionId}/s:{noteNumber}", MockFragment1.class)
                .registerFragmentRoute("app://www.app.com/laws/notes/article/i:{articleRevisionId}", MockFragment3.class)
                .registerFragmentRoute("app://www.app.com/laws/notes/paragraph/i:{paragraphRevisionId}", MockFragment4.class);
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

    @Test
    public void testActionRoute() throws Exception {
        Route route = router.resolveRoute("app://www.app.com/files/1843?inline=1");

        assertTrue(route.getResult() instanceof MockAction1);
        assertTrue(route.getWildcards().containsKey("fileId"));
        assertTrue(route.getQueryParams().containsKey("inline"));
    }

    @Test
    public void testStringRoute() throws Exception {
        Route route = router.resolveRoute("app://www.app.com/pdfViewer/lakis-4236-lalakis.pdf");

        assertEquals(route.getResult(), MockFragment3.class);
        assertTrue(route.getWildcards().containsKey("filename"));
        assertEquals(route.getWildcards().get("filename"), "lakis-4236-lalakis.pdf");
    }

    @Test
    public void testRouteWithEncodedUrl() throws Exception {
        Route route = router.resolveRoute("app://www.app.com/webview?url=http%3A%2F%2Fwww.app.com%2Fanalysis");

        assertEquals(route.getResult(), MockFragment4.class);
        assertTrue(route.getQueryParams().containsKey("url"));
        assertEquals(route.getQueryParams().get("url"), "http://www.app.com/analysis");
    }

    @Test
    public void testRouteEndingWithString() throws Exception {
        Route route = router.resolveRoute("app://www.app.com/laws/notes/article/19357/1");

        assertEquals(route.getResult(), MockFragment1.class);
        assertTrue(route.getWildcards().containsKey("articleRevisionId"));
        assertTrue(route.getWildcards().containsKey("noteNumber"));
    }
    
    @Test
    public void testNullRoute() throws Exception {
        Route route = router.resolveRoute("app://www.app.com/articles/article/19541");

        assertNull(route);
    }

    @Test
    public void testDifferentScheme() throws Exception {
        Route route = router.resolveRoute("wrong://www.app.com/webview?url=http%3A%2F%2Fwww.app.com%2Fanalysis");

        assertNull(route);
    }

    @Test
    public void testDifferentHost() throws Exception {
        Route route = router.resolveRoute("app://www.wrong.com/webview?url=http%3A%2F%2Fwww.app.com%2Fanalysis");

        assertNull(route);
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