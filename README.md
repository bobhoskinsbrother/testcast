## TestCast


This project is a JUnit 5 extension.

There are some useful annotations here:

#### @TestCast

Please check out ```uk.co.itstherules.examples.ExampleTestCastTest```

What does this do?  It records the screen and drops out an AVI video
For the example above it will drop out a video -> ./reports/uk.co.itstherules.examples.ExampleTestCastTest.canCaptureBrowsingGoogle.avi
Useful if you want to record the browser for intermittently failing frontend tests 

#### @SnapShot

Please check out ```uk.co.itstherules.examples.ExampleSnapShotTest```

What does this do?  It snap-shots the screen and drops out an jpg file when the test fails or has an error
For the example above it will drop out a jpg -> ./reports/uk.co.itstherules.examples.ExampleSnapShotTest.canCaptureOnFailure.jpg
Useful if you want to screen-grab the browser for intermittently failing frontend tests 


#### @ScreenCast

ScreenCast is a little different.  
I had this wacky idea a few years ago that instead of doing boring tutorial videos for your webapp, you could use happy 
path tests to document your key flows.  
What this ```@ScreenCast``` annotation does is start a desktop notification service in the background, which you can push
either a splash-screen graphic, or a message to display on the screen.  The notifier will pause the action
so you will have enough time to read the text.

Please check out ```uk.co.itstherules.examples.ExampleScreenCastTest```
 
It's a fairly complete example.  
The Notifier built in is client / server based, and can also be configured to run remotely.

Still to do: I'd like to write a "fast" and "record" mode where you can use the same tests to both test the flow and 
drop out a tutorial video.