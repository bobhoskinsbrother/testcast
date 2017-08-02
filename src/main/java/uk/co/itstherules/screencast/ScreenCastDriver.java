package uk.co.itstherules.screencast;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.util.List;
import java.util.stream.Collectors;

public class ScreenCastDriver extends ChromeDriver {

    public ScreenCastDriver(ChromeOptions options) {
        super(options);
    }

    @Override
    public List<WebElement> findElements(By by) {
        return super.findElements(by).stream().map(ScreenCastWebElement::new).collect(Collectors.toList());
    }

    @Override
    public WebElement findElement(By by) {
        return new ScreenCastWebElement(super.findElement(by));
    }

}
