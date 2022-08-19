package org.example;

import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class Runner {
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_RESET = "\u001B[0m";

    public static void run(String packageName) {
        Set<Class> classes = findAllClassesUsingReflectionsLibrary(packageName);
        AtomicLong testSuccesfulCount = new AtomicLong(0);
        AtomicLong testFailedCount = new AtomicLong(0);

        classes.forEach(clazz -> {
            Map<Class<?>, List<Method>> methods = getMethodsAnnotatedWithMethodXY(clazz);
            if (!methods.isEmpty()) {
                try {
                    Object instance = createInstance(clazz);
                    if(methods.get(Test.class) != null) {
                        List<Method> testMethods = methods.get(Test.class);
                        testMethods.forEach(method -> {
                            try {
                                if(methods.get(Before.class) != null) {
                                    List<Method> beforeMethods = methods.get(Before.class);
                                    beforeMethods.forEach(beforeMethod -> {
                                        try {
                                            beforeMethod.invoke(instance);
                                        } catch (IllegalAccessException | InvocationTargetException e) {
                                            e.printStackTrace();
                                            throw new RuntimeException("Before method failed", e.getCause());
                                        }
                                    });
                                }
                                method.invoke(instance);
                                System.out.println(String.format(ANSI_GREEN + "Test %s passed." + ANSI_RESET, method.getName()));
                                testSuccesfulCount.incrementAndGet();
                                if(methods.get(After.class) != null) {
                                    List<Method> afterMethods = methods.get(After.class);
                                    afterMethods.forEach(afterMethod -> {
                                        try {
                                            afterMethod.invoke(instance);
                                        } catch (IllegalAccessException | InvocationTargetException e) {
                                            e.printStackTrace();
                                            throw new RuntimeException("After method failed", e.getCause());
                                        }
                                    });
                                }
                            } catch (IllegalAccessException | InvocationTargetException e) {
                                //e.printStackTrace();
                                if (e instanceof InvocationTargetException && ((InvocationTargetException) e).getTargetException() instanceof AssertionException) {
                                    System.out.println(String.format(ANSI_RED + "Test %s failed: %s" + ANSI_RESET, method.getName(), ((InvocationTargetException) e).getTargetException().getMessage()));
                                    testFailedCount.incrementAndGet();
                                } else {
                                    throw new RuntimeException("Test method failed", e.getCause());
                                }
                            }
                        });
                    }
                } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        });
        System.out.println(String.format(ANSI_GREEN + "Tests passed: %s, "  + ANSI_RESET +  ANSI_YELLOW + "Tests failed: %s" + ANSI_RESET, testSuccesfulCount.get(), testFailedCount.get()));
    }

    public static Object createInstance(Class clazz) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<?> ctor = clazz.getConstructors()[0];
        return ctor.newInstance();
    }

    public static Set<Class> findAllClassesUsingReflectionsLibrary(String packageName) {
        Reflections reflections = new Reflections(packageName, new SubTypesScanner(false));
        return reflections.getSubTypesOf(Object.class)
                .stream()
                .collect(Collectors.toSet());
    }

    public static Map<Class<?>, List<Method>> getMethodsAnnotatedWithMethodXY(final Class<?> type) {
        final Map<Class<?>, List<Method>> methods = new HashMap<>();
        Class<?> klass = type;
        while (klass != Object.class && klass != null) { // need to iterated thought hierarchy in order to retrieve methods from above the current instance
            // iterate though the list of methods declared in the class represented by klass variable, and add those annotated with the specified annotation
            for (final Method method : klass.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Test.class)) {
                    methods.compute(Test.class, (key, value) -> {
                        if (value == null) {
                            value = new ArrayList<>();
                        }
                        value.add(method);
                        return value;

                    });
                }
                if (method.isAnnotationPresent(Before.class)) {
                    methods.compute(Before.class, (key, value) -> {
                        if (value == null) {
                            value = new ArrayList<>();
                        }
                        value.add(method);
                        return value;

                    });
                }
                if (method.isAnnotationPresent(After.class)) {
                    methods.compute(After.class, (key, value) -> {
                        if (value == null) {
                            value = new ArrayList<>();
                        }
                        value.add(method);
                        return value;

                    });
                }
            }
            // move to the upper class in the hierarchy in search for more methods
            klass = klass.getSuperclass();
        }
        return methods;
    }
}
