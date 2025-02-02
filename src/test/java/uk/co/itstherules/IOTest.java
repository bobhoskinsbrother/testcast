package uk.co.itstherules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IOTest {


    @Test
    public void canMakeProperMovieName() {
        IO unit = new IO("./reports");
        String reply = unit.makeMovieName("fred");
        assertEquals(reply, "./reports/fred.avi");
    }


    @Test
    public void canMakeProperImageName() {
        IO unit = new IO("./reports");
        String reply = unit.makeImageName("fred");
        assertEquals(reply, "./reports/fred.jpg");
    }

    @Test
    public void canMakeProperFilenameWhenAlreadyExists() {
        final int[] count = new int[]{0};
        IO unit = new IO("./reports") {
            @Override
            boolean fileExists(String fileName) {
                if (count[0] == 0) {
                    count[0]++;
                    return true;
                }
                return false;
            }
        };
        String reply = unit.makeMovieName("fred");
        assertEquals(reply, "./reports/fred(1).avi");
    }

    @Test
    public void canMakeProperFilenameWhenTwoAlreadyExists() {
        final int[] count = new int[]{0};
        IO unit = new IO("./reports") {
            @Override
            boolean fileExists(String fileName) {
                if (count[0] == 0 || count[0] == 1) {
                    count[0]++;
                    return true;
                }
                return false;
            }
        };
        String reply = unit.makeMovieName("fred");
        assertEquals(reply, "./reports/fred(2).avi");
    }

}
