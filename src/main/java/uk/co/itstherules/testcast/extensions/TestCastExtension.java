package uk.co.itstherules.testcast.extensions;

import ch.randelshofer.screenrecorder.ScreenRecorder;
import uk.co.itstherules.BaseExtension;
import uk.co.itstherules.IO;
import uk.co.itstherules.TestCastService;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

import java.io.File;

public class TestCastExtension extends BaseExtension<TestCast> {

    private final Class<TestCast> annotationClass = TestCast.class;
    private final Namespace namespace = Namespace.create("uk", "co", "itstherules", "TestCast");
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
        fileName = new IO(getDirectoryName()).makeMovieName(fileName);
        return new ScreenRecorder(new File(fileName));
    }

    @Override
    protected Class<? extends TestCastService> serviceClass() {
        return ScreenRecorder.class;
    }

}
