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


Please check out ```uk.co.itstherules.examples.ExampleScreenCastTest```
 
It's a fairly complete example.  
The Notifier built in is client / server based, and can be configured to run remotely.
