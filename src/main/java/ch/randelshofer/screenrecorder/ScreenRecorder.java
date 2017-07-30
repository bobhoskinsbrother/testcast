package ch.randelshofer.screenrecorder;

import ch.randelshofer.media.MovieWriter;
import ch.randelshofer.media.avi.AVIWriter;
import ch.randelshofer.media.color.Colors;
import ch.randelshofer.media.image.Images;
import ch.randelshofer.media.quicktime.QuickTimeWriter;
import de.affinitas.TestCastService;

import javax.sound.sampled.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.max;

public class ScreenRecorder implements TestCastService {

    private File file;
    private CursorColor cursor;
    private VideoFormat format;
    private ColorDepth depth;
    private MovieWriter writer;
    private long startTime;
    private long previousFrameTime;
    private float frameRate = 24;
    private float mouseFrameRate = 24;
    private int aviKeyFrameInterval = (int) (max(frameRate, mouseFrameRate) * 60);
    private int quickTimeKeyFrameInterval = (int) max(frameRate, mouseFrameRate);
    private long maxFrameDuration = 1000;
    private Robot robot;
    private Rectangle rectangle;
    private BufferedImage capturedScreenImage;
    private List<MouseCapture> mouseCaptures;
    private BufferedImage capturedScreenWithMouseImage;
    private Graphics2D capturedScreenWithMouseGraphics;
    private ScheduledThreadPoolExecutor screenTimer;
    private ScheduledThreadPoolExecutor mouseTimer;
    private BufferedImage mouseCursorImage;
    private Point mouseCursorImageOffset = new Point(-8, -5);
    private final Object threadSyncObject = new Object();
    private float audioRate;
    private Thread audioThread;
    private AudioFormat audioFormat;

    public ScreenRecorder(File file) {
        this(file, VideoFormat.AVI, 24, ColorDepth.MILLIONS, CursorColor.BLACK, 44100);
    }

    public ScreenRecorder(File file, VideoFormat format, int frameRate, ColorDepth depth, CursorColor cursor, float audioRate) {
        this.format = format;
        this.depth = depth;
        this.cursor = cursor;
        this.audioRate = audioRate;
        this.file = file;
        this.mouseFrameRate = frameRate;
        this.frameRate = frameRate;

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice screenDevice = ge.getDefaultScreenDevice();
        GraphicsConfiguration cfg = screenDevice.getDefaultConfiguration();
        rectangle = cfg.getBounds();
        try {
            robot = new Robot(cfg.getDevice());
        } catch (AWTException e) {
            throw new RuntimeException("Unable to create a robot to capture the screen", e);
        }
        if (depth == ColorDepth.MILLIONS) {
            capturedScreenWithMouseImage = new BufferedImage(rectangle.width, rectangle.height, BufferedImage.TYPE_INT_RGB);
        } else if (depth == ColorDepth.THOUSANDS) {
            capturedScreenWithMouseImage = new BufferedImage(rectangle.width, rectangle.height, BufferedImage.TYPE_USHORT_555_RGB);
        } else if (depth == ColorDepth.DOZENS) {
            capturedScreenWithMouseImage = new BufferedImage(rectangle.width, rectangle.height, BufferedImage.TYPE_BYTE_INDEXED, Colors.createMacColors());
        } else {
            throw new UnsupportedOperationException("Unsupported color depth " + depth);
        }
        capturedScreenWithMouseGraphics = capturedScreenWithMouseImage.createGraphics();
        capturedScreenWithMouseGraphics.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
        capturedScreenWithMouseGraphics.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
        capturedScreenWithMouseGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        mouseCaptures = Collections.synchronizedList(new LinkedList<MouseCapture>());
        if (cursor == CursorColor.BLACK) {
            mouseCursorImage = Images.toBufferedImage(Images.createImage(ScreenRecorder.class, "/ch/randelshofer/media/images/Cursor.black.png"));
        } else {
            mouseCursorImage = Images.toBufferedImage(Images.createImage(ScreenRecorder.class, "/ch/randelshofer/media/images/Cursor.white.png"));
        }

        createMovieWriter();
    }

    protected void createMovieWriter() {
        try {
            if (format == VideoFormat.AVI) {
                writer = createAviWriter();
            } else if (format == VideoFormat.QUICKTIME) {
                writer = createMovWriter();
            } else {
                throw new UnsupportedOperationException("Unsupported format " + format);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private MovieWriter createMovWriter() throws IOException {
        QuickTimeWriter writer = new QuickTimeWriter(file);
        writer.addVideoTrack(QuickTimeWriter.VIDEO_ANIMATION, 1000, rectangle.width, rectangle.height, depth.getValue(), quickTimeKeyFrameInterval);
        if (audioRate > 0) {
            audioFormat = new AudioFormat(audioRate, 16, 1, true, true);
            writer.addAudioTrack(audioFormat);
        }
        if (depth == ColorDepth.DOZENS) {
            writer.setVideoColorTable(0, (IndexColorModel) capturedScreenWithMouseImage.getColorModel());
        }
        return writer;
    }

    private MovieWriter createAviWriter() throws IOException {
        AVIWriter writer = new AVIWriter(file);
        writer.addVideoTrack(AVIWriter.VIDEO_SCREEN_CAPTURE, 1, (int) mouseFrameRate, rectangle.width, rectangle.height, depth.getValue(), aviKeyFrameInterval);
        if (depth == ColorDepth.DOZENS) {
            writer.setPalette(0, (IndexColorModel) capturedScreenWithMouseImage.getColorModel());
        }
        return writer;
    }


    /**
     * Starts the screen recorder.
     */
    public void start() throws Exception {
        startTime = previousFrameTime = System.currentTimeMillis();
        screenTimer = new ScheduledThreadPoolExecutor(1);
        screenTimer.scheduleAtFixedRate(() -> {
            try {
                grabScreen();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }, (int) (1000 / frameRate), (int) (1000 / frameRate), TimeUnit.MILLISECONDS);
        mouseTimer = new ScheduledThreadPoolExecutor(1);
        mouseTimer.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                grabMouse();
            }
        }, (int) (1000 / mouseFrameRate), (int) (1000 / mouseFrameRate), TimeUnit.MILLISECONDS);

        if (audioRate > 0 && (writer instanceof QuickTimeWriter)) {
            startAudio();
        }
    }

    /**
     * Starts audio capture.
     */
    private void startAudio() {
        DataLine.Info info = new DataLine.Info(
                TargetDataLine.class, audioFormat);
        final TargetDataLine line;
        try {
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);
            line.start();
            final int bufferSize;
            if (audioFormat.getFrameSize() != AudioSystem.NOT_SPECIFIED) {
                bufferSize = (int) audioFormat.getSampleRate()
                        * audioFormat.getFrameSize();
            } else {
                bufferSize = (int) audioFormat.getSampleRate();
            }
            audioThread = new Thread() {

                @Override
                public void run() {
                    byte buffer[] = new byte[bufferSize];

                    try {
                        while (audioThread == this) {
                            int count = line.read(buffer, 0, buffer.length);
                            if (count > 0) {
                                synchronized (threadSyncObject) {
                                    int sampleCount = count * 8 / audioFormat.getSampleSizeInBits();
                                    writer.writeSamples(1, sampleCount, buffer, 0, count, 1, true);
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    } finally {
                        line.close();
                    }
                }
            };
            audioThread.start();
        } catch (LineUnavailableException ex) {
            // FIXME - Instead of silently suppressing audio recording, we should
            // print an error message to the user
            ex.printStackTrace();
        }
    }

    /**
     * Stops the screen recorder.
     */
    public void stop() throws Exception {
        mouseTimer.shutdown();
        screenTimer.shutdown();
        Thread T = audioThread;
        audioThread = null;

        try {
            mouseTimer.awaitTermination((int) (1000 / mouseFrameRate), TimeUnit.MILLISECONDS);
            screenTimer.awaitTermination((int) (1000 / frameRate), TimeUnit.MILLISECONDS);
            if (T != null) {
                T.join();
            }
        } catch (InterruptedException ex) {
            // nothing to do
        }
        synchronized (threadSyncObject) {
            if(writer!=null) {
                writer.close();
                writer = null;
            }
        }
        capturedScreenWithMouseGraphics.dispose();
        capturedScreenWithMouseImage.flush();
    }

    /**
     * Grabs a screen, generates video images with pending mouse captures
     * and writes them into the movie file.
     */
    private void grabScreen() throws IOException {
        // Capture the screen
        capturedScreenImage = robot.createScreenCapture(new Rectangle(0, 0, rectangle.width, rectangle.height));
        long now = System.currentTimeMillis();
        capturedScreenWithMouseGraphics.drawImage(capturedScreenImage, 0, 0, null);


        // Generate video frames with mouse cursor painted on them
        boolean hasMouseCapture = false;
        if (cursor != CursorColor.NONE) {

            Point previous = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
            while (!mouseCaptures.isEmpty() && mouseCaptures.get(0).getTime() < now) {
                MouseCapture pc = mouseCaptures.remove(0);
                if (pc.getTime() > previousFrameTime) {
                    hasMouseCapture = true;
                    Point p = pc.getP();
                    p.x -= rectangle.x;
                    p.y -= rectangle.y;
                    synchronized (threadSyncObject) {
                        if (!writer.isVFRSupported() || p.x != previous.x || p.y != previous.y || pc.getTime() - previousFrameTime > maxFrameDuration) {
                            previous.x = p.x;
                            previous.y = p.y;

                            // draw cursor
                            capturedScreenWithMouseGraphics.drawImage(mouseCursorImage, p.x + mouseCursorImageOffset.x, p.y + mouseCursorImageOffset.y, null);
                            if (writer == null) {
                                return;
                            }
                            try {
                                writer.writeFrame(0, capturedScreenWithMouseImage, (int) (pc.getTime() - previousFrameTime));
                            } catch (Throwable t) {
                                System.out.flush();
                                t.printStackTrace();
                                System.err.flush();
                                System.exit(10);
                            }
                            previousFrameTime = pc.getTime();
                            // erase cursor
                            capturedScreenWithMouseGraphics.drawImage(capturedScreenImage, //
                                    p.x + mouseCursorImageOffset.x, p.y + mouseCursorImageOffset.y,//
                                    p.x + mouseCursorImageOffset.x + mouseCursorImage.getWidth() - 1, p.y + mouseCursorImageOffset.y + mouseCursorImage.getHeight() - 1,//
                                    p.x + mouseCursorImageOffset.x, p.y + mouseCursorImageOffset.y,//
                                    p.x + mouseCursorImageOffset.x + mouseCursorImage.getWidth() - 1, p.y + mouseCursorImageOffset.y + mouseCursorImage.getHeight() - 1,//
                                    null);
                        }

                        // Close file on a separate thread if file is full or an hour
                        // has passed.
                        if (writer.isDataLimitReached() || now - startTime > 60 * 60 * 1000) {
                            System.out.println("ScreenRecorder dataLimit:" + writer.isDataLimitReached() + " timeLimit=" + (now - startTime > 60 * 60 * 1000));
                            new Thread() {

                                @Override
                                public void run() {
                                    try {
                                        writer.close();
                                    } catch (IOException ex) {
                                        ex.printStackTrace();
                                    }

                                }
                            }.start();
                            createMovieWriter();
                            startTime = now;
                        }
                    }
                }
            }
        }
        if (!hasMouseCapture) {
            if (cursor != CursorColor.NONE) {
                PointerInfo info = MouseInfo.getPointerInfo();
                Point p = info.getLocation();
                capturedScreenWithMouseGraphics.drawImage(mouseCursorImage, p.x + mouseCursorImageOffset.x, p.x + mouseCursorImageOffset.y, null);
            }
            synchronized (threadSyncObject) {
                writer.writeFrame(0, capturedScreenWithMouseImage, (int) (now - previousFrameTime));
            }
            previousFrameTime = now;
        }
    }

    /**
     * Captures the mouse cursor.
     */
    private void grabMouse() {
        long now = System.currentTimeMillis();
        PointerInfo info = MouseInfo.getPointerInfo();

        mouseCaptures.add(new MouseCapture(now, info.getLocation()));
    }

}
