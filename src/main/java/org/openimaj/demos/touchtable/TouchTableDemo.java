package org.openimaj.demos.touchtable;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.openimaj.image.FImage;
import org.openimaj.image.MBFImage;
import org.openimaj.image.connectedcomponent.ConnectedComponentLabeler;
import org.openimaj.image.pixel.ConnectedComponent;
import org.openimaj.image.processor.connectedcomponent.ConnectedComponentProcessor;
import org.openimaj.math.geometry.point.Point2d;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.VideoDisplayListener;
import org.openimaj.video.capture.VideoCapture;

public class TouchTableDemo implements VideoDisplayListener<MBFImage> {
	
	private static final int IMAGE_WIDTH = 640;
	private static final int IMAGE_HEIGHT = 480;
	public static final int SMALLEST_POINT_AREA = Math.max(1,(IMAGE_WIDTH*IMAGE_HEIGHT)/(60*20));
	public static final int BIGGEST_POINT_AREA = Math.max(1,(IMAGE_WIDTH*IMAGE_HEIGHT)/(30*10));
	public static final int SMALLEST_POINT_DIAMETER = (int) (SMALLEST_POINT_AREA/Math.PI);
	public static final int BIGGEST_POINT_DIAMETER = (int) (BIGGEST_POINT_AREA/Math.PI);
	private VideoCapture capture;
	private VideoDisplay<MBFImage> display;
	private ConnectedComponentLabeler labler;
	private TouchTableScreen touchTableScreen;
	private FImageBackgroundLearner backgroundLearner;

	public TouchTableDemo() throws IOException{
		this.capture = new VideoCapture(IMAGE_WIDTH,IMAGE_HEIGHT);
		this.display = VideoDisplay.createVideoDisplay(capture);
		this.labler = new ConnectedComponentLabeler(ConnectedComponent.ConnectMode.CONNECT_4);
		
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] devices = ge.getScreenDevices();
		if(devices.length > 1){
			// Then there is a touchscreen attached
			this.touchTableScreen = new TouchTableScreen();
			devices[1].setFullScreenWindow(this.touchTableScreen);
			this.touchTableScreen.init();	
		}
		
		this.backgroundLearner = new FImageBackgroundLearner();
		this.display.addVideoListener(this);

	}
	
	public static void main(String[] args) throws IOException {
		new TouchTableDemo();
	}

	@Override
	public void afterUpdate(VideoDisplay<MBFImage> display) {
		
	}

	@Override
	public void beforeUpdate(MBFImage frame) {
		FImage grey = frame.flatten();
		
		if(!this.backgroundLearner.ready()){
			grey.process(this.backgroundLearner);
			return;
		}
		grey.addInline(this.backgroundLearner.getBackground());
		grey.threshold(0.25f);
		List<ConnectedComponent> comps = labler.findComponents(grey);
		List<Point2d> filtered = new ArrayList<Point2d>();
		for (ConnectedComponent connectedComponent : comps) {
			int nPixels = connectedComponent.pixels.size();
//			System.out.println(nPixels);
//			System.out.println("Min is: " + VALID_PIXEL_THRESH_MIN);
//			System.out.println("Max is: " + VALID_PIXEL_THRESH_MAX);
			if(nPixels < SMALLEST_POINT_AREA || 
			   nPixels > BIGGEST_POINT_AREA) continue;
			filtered.add(connectedComponent.calculateCentroidPixel());
		}
		if(filtered.size() != 0)
			this.fireTouchEvent(filtered);
		
		frame.drawImage(new MBFImage(grey,grey,grey), 0, 0);
	}

	private void fireTouchEvent(List<Point2d> filtered) {
		this.touchTableScreen.touchEvent(filtered);
	}
}
