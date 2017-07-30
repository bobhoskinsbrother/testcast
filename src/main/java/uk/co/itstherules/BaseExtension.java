package uk.co.itstherules;

import org.junit.jupiter.api.extension.*;
import org.junit.platform.commons.util.AnnotationUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public abstract class BaseExtension<T extends Annotation> implements BeforeTestExecutionCallback, AfterTestExecutionCallback {

    @Override
    public void beforeTestExecution(TestExtensionContext context) {
        if (!shouldBeRecorded(context)) return;
        new IO(getDirectoryName()).makeDirectoryIfNotExists();
        launchAndStore(context);
    }

    @Override
    public void afterTestExecution(TestExtensionContext context) {
        if (!shouldBeRecorded(context)) return;
        TestCastService testCastService = loadTestCastService(context);
        try {
            testCastService.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public abstract String getDirectoryName();

    public abstract Class<T> getAnnotationClass();

    public abstract ExtensionContext.Namespace getNamespace();

    public abstract TestCastService makeService(String fileName);

    private boolean shouldBeRecorded(ExtensionContext context) {
        return context.getElement()
                .map(el -> AnnotationUtils.isAnnotated(el, getAnnotationClass()))
                .orElse(false);
    }

    private void launchAndStore(ExtensionContext context) {
        Method method = context.getTestMethod().get();
        String methodName = method.getName();
        String fileName = method.getDeclaringClass().getCanonicalName() + "." + methodName;
        TestCastService service = makeService(fileName);
        try {
            service.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        context.getStore(getNamespace()).put("testcast_service_" + methodName, service);
    }

    private TestCastService loadTestCastService(ExtensionContext context) {
        Method method = context.getTestMethod().get();
        String methodName = method.getName();
        return context.getStore(getNamespace()).get("testcast_service_" + methodName, serviceClass());
    }

    protected abstract Class<? extends TestCastService> serviceClass();

}