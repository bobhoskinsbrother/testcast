package uk.co.itstherules.screencast;

import org.openqa.selenium.*;

import java.util.List;
import java.util.stream.Collectors;

public class ScreenCastWebElement implements WebElement {

    private final WebElement delegate;

    public ScreenCastWebElement(WebElement delegate) {
        this.delegate = delegate;
    }

    @Override
    public void click() {
        try {
            Thread.sleep(300);
            delegate.click();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void submit() {
        try {
            Thread.sleep(300);
            delegate.submit();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void sendKeys(CharSequence... charSequences) {
        for (CharSequence charSequence : charSequences) {
            for (int i = 0; i < charSequence.length(); i++) {
                 String character = String.valueOf(charSequence.charAt(i));
                try {
                    Thread.sleep(200);
                    delegate.sendKeys(character);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public String getTagName() {
        return delegate.getTagName();
    }

    @Override
    public String getAttribute(String s) {
        return delegate.getAttribute(s);
    }

    @Override
    public boolean isSelected() {
        return delegate.isSelected();
    }

    @Override
    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    @Override
    public String getText() {
        return delegate.getText();
    }

    @Override
    public List<WebElement> findElements(By by) {
        return  delegate.findElements(by).stream().map(ScreenCastWebElement::new).collect(Collectors.toList());
    }

    @Override
    public WebElement findElement(By by) {
        return new ScreenCastWebElement(delegate.findElement(by));
    }

    @Override
    public boolean isDisplayed() {
        return delegate.isDisplayed();
    }

    @Override
    public Point getLocation() {
        return delegate.getLocation();
    }

    @Override
    public Dimension getSize() {
        return delegate.getSize();
    }

    @Override
    public Rectangle getRect() {
        return delegate.getRect();
    }

    @Override
    public String getCssValue(String s) {
        return delegate.getCssValue(s);
    }

    @Override
    public <X> X getScreenshotAs(OutputType<X> outputType) throws WebDriverException {
        return delegate.getScreenshotAs(outputType);
    }
}
