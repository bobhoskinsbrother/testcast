package de.affinitas.testcast.extensions;

import ch.randelshofer.screenrecorder.ScreenRecorder;
import de.affinitas.BaseExtension;
import de.affinitas.IO;
import de.affinitas.TestCastService;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

import java.io.File;

public class TestCastExtension extends BaseExtension<TestCast> {

    private final Class<TestCast> annotationClass = TestCast.class;
    private final Namespace namespace = Namespace.create("de", "affinitas", "TestCast");
    private final String directoryName = "./reports";

    @Override
    public String getDirectoryName() {
        return directoryName;
    }

    @Override
    public Class<TestCast> getAnnotationClass() {
        return annotationClass;
    }

    @Override
    public Namespace getNamespace() {
        return namespace;
    }

    @Override
    public TestCastService makeService(String fileName) {
        fileName = new IO(getDirectoryName()).makeFileName(fileName);
        return new ScreenRecorder(new File(fileName));
    }

    @Override
    protected Class<? extends TestCastService> serviceClass() {
        return ScreenRecorder.class;
    }

}
