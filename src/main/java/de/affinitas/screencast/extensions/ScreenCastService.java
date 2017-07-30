package de.affinitas.screencast.extensions;

import ch.randelshofer.screenrecorder.ScreenRecorder;
import de.affinitas.TestCastService;
import de.affinitas.screencast.server.PopUpConfiguration;
import de.affinitas.screencast.server.PopUpServer;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScreenCastService implements TestCastService {

    private final ExecutorService recorderExecutor;
    private final ScreenRecorder recorder;
    private final ExecutorService popupServerExecutor;
    private final PopUpServer popUpService;

    public ScreenCastService(String fileName) {

        recorder = new ScreenRecorder(new File(fileName));
        recorderExecutor = Executors.newSingleThreadExecutor();

        popUpService = new PopUpServer(new PopUpConfiguration());
        popupServerExecutor = Executors.newSingleThreadExecutor();

    }

    @Override
    public void start() {
        startService(popupServerExecutor, popUpService);
        startService(recorderExecutor, recorder);
    }

    @Override
    public void stop() throws IOException {
        stopService(recorderExecutor, recorder);
        stopService(popupServerExecutor, popUpService);
    }

    private void startService(ExecutorService executorService, TestCastService server) {
        executorService.execute(() -> {
            try {
                server.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
    private void stopService(ExecutorService executorService, TestCastService service) {
        if (service == null) {
            return;
        }
        try {
            service.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        executorService.shutdown();
    }

}
