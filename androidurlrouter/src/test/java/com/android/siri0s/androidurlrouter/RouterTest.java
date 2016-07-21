package com.android.siri0s.androidurlrouter;

import android.app.Activity;
import android.app.Fragment;
import android.util.Log;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
@RunWith(MockitoJUnitRunner.class)
public class RouterTest {

    private Router router;

    @Mock
    Log Log;

    @Before
    public void initRouter() {
        router = Router.getInstance();
        router.registerFragmentRoute("app://www.app.com/profile/settings/me/s:{slug}", MockFragment2.class);
        router.registerFragmentRoute("app://www.app.com/laws/i:{lawId}/articles/i:{articleId}", MockFragment1.class);
        router.registerActivityRoute("app://www.app.com/articles/i:{articleId}/related", MockActivity1.class);
    }

    @After
    public void resetRouter() {
        router.reset();
    }

    @Test
    public void createMappedRouteRegex() throws Exception {
        Method cmrr = Router.class.getDeclaredMethod("createMappedRouteRegex", String.class, boolean.class);
        cmrr.setAccessible(true);

        String regex = null;
        regex = (String) cmrr.invoke(router, "app://www.app.com/articles/i:{articleId}/related/s:{slug}", true);
        assertEquals(regex, "^app://www\\.app\\.com/articles/(\\d+)/related/[^(/|#|!|?)]+(\\/([a-zA-Z]|\\-)+)?$");
    }

    @Test
    public void testFragmentRouteArguments() throws Exception {
        Method rr = Router.class.getDeclaredMethod("resolveRoute", String.class);
        rr.setAccessible(true);

        Route route = (Route) rr.invoke(router, "app://www.app.com/laws/1981/articles/14563?order=desc");

        assertThat(route, CoreMatchers.instanceOf(FragmentRoute.class));
        assertTrue(route.getWildcards().containsKey("lawId"));
        assertTrue(route.getWildcards().containsKey("articleId"));
        assertEquals(route.getWildcards().get("lawId"), "1981");
        assertEquals(route.getWildcards().get("articleId"), "14563");
        assertTrue(route.getQueryParams().containsKey("order"));
        assertEquals(route.getQueryParams().get("order"), "desc");
    }

    @Test
    public void testActivityRouteArguments() throws Exception {
        Method rr = Router.class.getDeclaredMethod("resolveRoute", String.class);
        rr.setAccessible(true);

        Route route = (Route) rr.invoke(router, "app://www.app.com/articles/19581/related?page=4&order=desc");

        assertThat(route, CoreMatchers.instanceOf(ActivityRoute.class));
        assertTrue(route.getWildcards().containsKey("articleId"));
        assertTrue(route.getQueryParams().containsKey("page"));
        assertTrue(route.getQueryParams().containsKey("order"));

        assertEquals(route.getWildcards().get("articleId"), "19581");
        assertEquals(route.getQueryParams().get("page"), "4");
        assertEquals(route.getQueryParams().get("order"), "desc");
    }

    @Test(expected = IllegalStateException.class)
    public void testNoFragmentToolsThrowsException() throws Exception {
        router.execRoute("app://www.app.com/laws/13751/articles/12842", null, Router.FLAG_ADD_TO_BACKSTACK, Router.FLAG_REPLACE_FRAGMENT);
    }

    public static class MockActivity1 extends Activity {
    }

    public static class MockFragment1 extends Fragment {
    }

    public static class MockFragment2 extends Fragment {
    }

    public static class MockFragment3 extends Fragment {
    }

    public static class MockFragment4 extends Fragment {
    }
}