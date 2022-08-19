package org.example;

public class Assertions {
    public static void assertEquals(Object expected, Object actual, String message) {
        if(!expected.equals(actual)) {
            throw new AssertionException(String.format("Expected %s, but was %s. %s", expected.toString(), actual.toString(), message));
        }
    }

    public static void assertNotEquals(Object expected, Object actual, String message) {
        if(expected.equals(actual)) {
            throw new AssertionException(String.format("Expected %s, but was %s. %s", expected.toString(), actual.toString(), message));
        }
    }
}
