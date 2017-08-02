package uk.co.itstherules.examples;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import uk.co.itstherules.TestUtils;
import uk.co.itstherules.snapshot.SnapShot;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExampleSnapShotTest {

    private WebDriver driver;

    @BeforeEach
    public void setupWebDriver() {
        driver = TestUtils.preCookedWebDriver();
    }

    @AfterEach
    public void shutdownWebDriver() {
        driver.close();
    }

    @Test
    @SnapShot
    public void canCaptureOnFailure() throws Exception {
        driver.get("http://www.google.com");
        WebElement searchBox = driver.findElement(By.name("q"));
        searchBox.sendKeys("TestCast");
        assertEquals("Altavista", driver.getTitle());
    }

}
