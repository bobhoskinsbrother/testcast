package de.affinitas.screencast.extensions;

import de.affinitas.BaseExtension;
import de.affinitas.IO;
import de.affinitas.TestCastService;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ScreenCastExtension extends BaseExtension<ScreenCast> {

    private final Class<ScreenCast> annotationClass = ScreenCast.class;
    private final ExtensionContext.Namespace namespace = ExtensionContext.Namespace.create("de", "affinitas", "ScreenCast");
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
        fileName = new IO(getDirectoryName()).makeFileName(fileName);
        return new ScreenCastService(fileName);
    }
    @Override
    protected Class<? extends TestCastService> serviceClass() {
        return ScreenCastService.class;
    }


}