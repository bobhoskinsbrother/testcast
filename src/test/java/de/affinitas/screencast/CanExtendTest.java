package de.affinitas.screencast;

import de.affinitas.testcast.extensions.TestCast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CanExtendTest {

    @Test
    @TestCast
    public void canExtend() throws Exception {
        RecordScreen.main("");
        assertEquals(true, true);
    }

    @Test
    @TestCast
    public void canExtendAnother() throws Exception {
        RecordScreen.main("");
        assertEquals(true, true);
    }

}
