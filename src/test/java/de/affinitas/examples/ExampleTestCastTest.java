package de.affinitas.examples;

import de.affinitas.testcast.extensions.TestCast;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import static org.junit.Assert.assertEquals;

public class ExampleTestCastTest {

    @Test
    @TestCast
    public void canCaptureBrowsingGoogle() throws Exception {
        System.setProperty("webdriver.chrome.driver", "bin/chromedriver");
        ChromeOptions options = kioskModeOptions();
        WebDriver driver = new ChromeDriver(options);
        driver.get("http://www.google.com");
        WebElement searchBox = driver.findElement(By.name("q"));
        searchBox.sendKeys("TestCast");
        searchBox.submit();
        assertEquals(driver.getTitle(), "TestCast - Google Search");
        driver.quit();
    }

    private ChromeOptions kioskModeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--test-type");
        options.addArguments("--start-maximized");  // this is where the popover full screen magic happens on windows
        options.addArguments("--kiosk");            // this is where the popover full screen magic happens on mac / linux
        options.addArguments("--js-flags=--expose-gc");
        options.addArguments("--enable-precise-memory-info");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--disable-default-apps");
        options.addArguments("--disable-infobars");
        return options;
    }

}
