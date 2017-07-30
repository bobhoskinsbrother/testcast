package uk.co.itstherules.screencast;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ScreenCastDriver implements WebDriver {

    private final WebDriver delegate;

    public ScreenCastDriver(WebDriver delegate) {
        this.delegate = delegate;
    }

    @Override
    public void get(String s) {
        delegate.get(s);
    }

    @Override
    public String getCurrentUrl() {
        return delegate.getCurrentUrl();
    }

    @Override
    public String getTitle() {
        return delegate.getTitle();
    }

    @Override
    public List<WebElement> findElements(By by) {
        return delegate.findElements(by).stream().map(ScreenCastWebElement::new).collect(Collectors.toList());
    }

    @Override
    public WebElement findElement(By by) {
        return new ScreenCastWebElement(delegate.findElement(by));
    }

    @Override
    public String getPageSource() {
        return delegate.getPageSource();
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public void quit() {
        delegate.quit();
    }

    @Override
    public Set<String> getWindowHandles() {
        return delegate.getWindowHandles();
    }

    @Override
    public String getWindowHandle() {
        return delegate.getWindowHandle();
    }

    @Override
    public TargetLocator switchTo() {
        return delegate.switchTo();
    }

    @Override
    public Navigation navigate() {
        return delegate.navigate();
    }

    @Override
    public Options manage() {
        return delegate.manage();
    }
}
