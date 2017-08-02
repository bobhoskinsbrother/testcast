package uk.co.itstherules.examples;

import uk.co.itstherules.TestUtils;
import uk.co.itstherules.testcast.extensions.TestCast;
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
        ChromeOptions options = TestUtils.kioskModeOptions();
        WebDriver driver = new ChromeDriver(options);
        driver.get("http://www.google.com");
        WebElement searchBox = driver.findElement(By.name("q"));
        searchBox.sendKeys("TestCast");
        searchBox.submit();
        assertEquals(driver.getTitle(), "TestCast - Google Search");
        driver.quit();
    }

}
