package uk.co.itstherules.screencast.extensions;

import uk.co.itstherules.BaseExtension;
import uk.co.itstherules.IO;
import uk.co.itstherules.TestCastService;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ScreenCastExtension extends BaseExtension<ScreenCast> {

    private final Class<ScreenCast> annotationClass = ScreenCast.class;
    private final ExtensionContext.Namespace namespace = ExtensionContext.Namespace.create("uk", "co", "itstherules"," ScreenCast");
    private final String directoryName = "./screencasts";

    @Override
    public String getDirectoryName() {
        return directoryName;
    }

    @Override
    public Class<ScreenCast> getAnnotationClass() {
        return annotationClass;
    }

    @Override
    public ExtensionContext.Namespace getNamespace() {
        return namespace;
    }

    @Override
    public TestCastService makeService(String fileName) {
        fileName = new IO(getDirectoryName()).makeMovieName(fileName);
        return new ScreenCastService(fileName);
    }
    @Override
    protected Class<? extends TestCastService> serviceClass() {
        return ScreenCastService.class;
    }


}