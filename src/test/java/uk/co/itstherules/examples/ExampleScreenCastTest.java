package uk.co.itstherules.examples;

import uk.co.itstherules.screencast.Notifier;
import uk.co.itstherules.screencast.ScreenCastDriver;
import uk.co.itstherules.screencast.extensions.ScreenCast;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.awt.*;

public class ExampleScreenCastTest {

    @Test
    @ScreenCast
    public void canCaptureBrowsingGoogle() throws Exception {

        // setup chrome so the screen is on top and thus can be recorded
        WebDriver driver = preCookedWebDriver();

        /*
         * Optional splashScreen for the beginning of the screencast
         * This will look on the classpath for the image, and resize the splash
         * notifier based on the dimensions of the image
         */
        Notifier.splashScreen("google_logo.jpg");

        driver.get("http://www.google.com");

        Notifier.notify("This is where we search for stuff on the internet");

        WebElement searchBox = driver.findElement(By.name("q"));
        searchBox.sendKeys("TestCast");

        Notifier.notify("We'll now look for how popular TestCast is");

        searchBox.submit();

        Notifier.notify("Not very popular is it? :( ");
        Thread.sleep(1000);

        driver.quit();
    }

    private WebDriver preCookedWebDriver() {
        System.setProperty("webdriver.chrome.driver", "bin/chromedriver");
        ChromeOptions options = screenCastChromeOptions();
        ChromeDriver delegate = new ChromeDriver(options);
        setAsFullSizeForMacWithChrome(delegate);
        focusOnBrowserWindow(delegate);

        // Use the ScreenCastDriver this is so typing and clicking looks more human (it's optional)
        return new ScreenCastDriver(delegate);
    }

    private void focusOnBrowserWindow(WebDriver driver) {
        JavascriptExecutor.class.cast(driver).executeScript("alert(\"Start Recording Window\")");
        driver.switchTo().alert().accept();
    }

    private void setAsFullSizeForMacWithChrome(WebDriver driver) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        driver.manage().window().setPosition(new org.openqa.selenium.Point(0, 0));
        driver.manage().window().setSize(new org.openqa.selenium.Dimension((int) screenSize.getWidth(), (int) screenSize.getHeight()));
    }

    private ChromeOptions screenCastChromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--test-type");
        options.addArguments("--js-flags=--expose-gc");
        options.addArguments("--enable-precise-memory-info");
        options.addArguments("--disable-popup-blocking");
        options.addArguments("--disable-default-apps");
        options.addArguments("--disable-infobars");
        return options;
    }

}
