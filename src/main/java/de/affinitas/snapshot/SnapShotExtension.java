package de.affinitas.snapshot;

import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestExtensionContext;

import java.lang.reflect.Method;

public class SnapShotExtension implements TestExecutionExceptionHandler {

    @Override
    public void handleTestExecutionException(TestExtensionContext context, Throwable throwable) throws Throwable {
        String fileName = fileName(context);
        new SnapShotService().screen(fileName);
        throw throwable;
    }

    private String fileName(TestExtensionContext context) {
        Method method = context.getTestMethod().get();
        SnapShot annotation = method.getAnnotation(SnapShot.class);
        if ("".equals(annotation.fileName())) {
            return method.getDeclaringClass().getCanonicalName() + "." + method.getName();
        }
        return annotation.fileName();
    }
}
