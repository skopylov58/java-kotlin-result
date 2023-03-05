package result;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.Permission;
import java.security.Principal;
import java.util.Optional;
import java.util.function.Function;

import org.junit.Test;

import result.Result.CheckedFunction;
import static result.Result.*;

public class ResultTest {
    
    @SuppressWarnings("preview")
    @Test
    public void testSwitch() throws Exception {
        
        Result<Integer> result = runCatching(this::getInt);

        if (result instanceof Success<Integer>(var i)) {
            System.out.println(i);
        }

        int res = switch (result) {
        case Success<Integer> s -> s.value() + 1;
        case Failure fail -> 0;
        };

        int res2 = switch (result) {
        case Success<Integer>(Integer i) -> i + 1;
        case Failure<Integer>(Exception e) -> -1;
        default -> 0;
        };
        
        System.out.println(result);
        
    }
    
    int getInt() throws Exception {
        throw new Exception("getInt");
    }
    
    @SuppressWarnings("preview")
    @Test
    public void testRecordPatternMatching() throws Exception {
        var s = Success.<Integer>of(1);
        
        if (s instanceof Success<Integer>(Integer value) ) {
            value.toHexString(0);
        }
        
    }

    record Point(String value) {};
    @SuppressWarnings("preview")
    @Test
    public void testName() throws Exception {
        var p = new Point("foo");
        
        if (p instanceof Point(String s) pp) {
            System.out.println(s);
            pp.value();
        }
        
    }
    
    @Test
    public void testFlatMap() throws Exception {
        var r = Success.of("a");
        CheckedFunction<String, URL> s2i = URL::new;
        Function<String, Result<URL>> f = Result.lift(s2i);
        var x = r.flatMap(f);
        
        assertTrue(x instanceof Failure);
        System.out.println(f);
    }
    
    @Test
    public void testCompose() throws Exception {
        var v1 = Success.of("a");
        var v2 = Success.of("b");
        
        var res = v1.flatMap(x -> v2.map(y -> x+y));
        assertTrue(res instanceof Success);
        var s = (Success) res;
        assertEquals("ab", s.value());
        
    }
    
    
    
    void covariance() {
        Optional<? extends Number> nopt = Optional.empty();
        Optional<Double> dopt = Optional.empty();
        nopt = dopt;
    }
    
    Principal    getUserPrincipalById(long id) throws Exception {return null;}
    Permission[] getUserPermissions(Principal user) throws Exception {return null;}
    
    Permission[] getPermissionsByIdTraditional(long userId) {
        try {
            return getUserPermissions(getUserPrincipalById(userId));
        } catch (Exception e) {
            return null;
        }
    }
    
    Permission[] getPermissionsById(long userId) {
        return runCatching(() -> getUserPrincipalById(userId))
        .mapCatching(this::getUserPermissions)
        .fold(Function.identity(), e -> null);
    }

    Permission[] getPermissionsById_00(long userId) {
        return runCatching(() -> getUserPermissions(getUserPrincipalById(userId)))
        .fold(x -> x, e -> null); //x->x is identity function
    }

    
    Permission[] getPermissionsById_1(long userId) {
        return Success.of(userId)
        .mapCatching(this::getUserPrincipalById)
        .mapCatching(this::getUserPermissions)
        .fold(Function.identity(), e -> null);
    }

    @SuppressWarnings("preview")
    Permission[] getPermissionsById_2(long userId) {
        return switch(Result.runCatching(() -> getUserPermissions(getUserPrincipalById(userId)))) {
            case Success<Permission[]>(Permission[] perms) -> perms;
            default -> null;
        };
    }

    Permission[] getPermissionsById_3(long userId) {
        var result = runCatching(() -> getUserPrincipalById(userId)).mapCatching(this::getUserPermissions);
        return switch (result) {
        case Success<Permission[]>(Permission[] perms) -> perms;
        default -> null;
        };
    }    
    
    @Test
    public void testUrl() {
        var urlResult = Result.runCatching(() -> new URL("foo/bar"));
        assertTrue(urlResult instanceof Failure);
        urlResult.onFailure(e -> assertTrue(e instanceof MalformedURLException));
    }
    
    
    Optional<Integer> getURLPortTraditional(String urlStr) {
        try {
            URL url = new URL(urlStr);
            int port = url.getPort();
            return port == -1 ? Optional.empty() : Optional.of(port);
        } catch (MalformedURLException e) {
            return Optional.empty();
        }
    }    
    
    //Extracts host portion of URL
    Optional<Integer> getURLPortWithSimplePatternMatching(String url) {
        var portResult = Result.runCatching(() -> new URL(url)).map(URL::getPort);
        return switch (portResult) {
        case Success<Integer> s -> s.value() == -1 ? Optional.empty() : Optional.of(s.value());
        case Failure f -> Optional.empty();
        };
    }

    //Extracts host portion of URL
    Optional<Integer> getURLPortWithRecordMatching(String url) {
        var portResult = Result.runCatching(() -> new URL(url)).map(URL::getPort);
        return switch (portResult) {
        case Success<Integer>(Integer port) -> port == -1 ? Optional.empty() : Optional.of(port);
        case Failure f -> Optional.empty();
        default -> Optional.empty();
        };
    }

    //Extracts host portion of URL
    Optional<Integer> getURLPortWithRecordMatchingInfere(String url) {
        var portResult = Result.runCatching(() -> new URL(url)).map(URL::getPort);
        return switch (portResult) {
        case Success<Integer>(Integer port) -> port == -1 ? Optional.empty() : Optional.of(port);
        case Failure f -> Optional.empty();
        default -> Optional.empty();
        };
    }

    //Extracts host portion of URL
    Optional<Integer> getURLPortWithMonad(String url) {
        return Result.runCatching(() -> new URL(url)).map(URL::getPort)
            .filter(port -> port != -1)
            .fold(port -> Optional.of(port), exception -> Optional.empty());
    }
}
