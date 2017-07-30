## TestCast


This project is a JUnit 5 extension.

There are some useful annotations here:

#### @TestCast

This annotation should be used at the method level, like so:

```$java
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
```

What does this do?  It records the screen and drops out an AVI video
For the example above it will drop out a video -> ./reports/fullName.of.TestClass.canCaptureBrowsingGoogle.avi
Useful if you want to record the browser for intermittently failing tests 


#### @ScreenCast

ScreenCast is a little different.  
I had this wacky idea a few years ago that instead of doing boring tutorial videos you could use happy 
path tests to document your key flows.  

You'd write your tests, add a few notifications, and watch while the screencast records itself.

```$java

    @Test
    @ScreenCast
    public void canCaptureBrowsingGoogle() throws Exception {
        System.setProperty("webdriver.chrome.driver", "bin/chromedriver");

        WebDriver driver = preCookedWebDriver();

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
```
For the example above it will drop out a video -> ./screencasts/fullName.of.TestClass.canCaptureBrowsingGoogle.avi
