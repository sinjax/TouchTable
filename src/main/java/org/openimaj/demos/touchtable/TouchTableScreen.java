package org.openimaj.demos.touchtable;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.image.colour.RGBColour;
import org.openimaj.image.pixel.ConnectedComponent;
import org.openimaj.math.geometry.line.Line2d;
import org.openimaj.math.geometry.point.Point2d;
import org.openimaj.math.geometry.point.Point2dImpl;
import org.openimaj.math.geometry.transforms.HomographyModel;
import org.openimaj.util.pair.IndependentPair;

import Jama.Matrix;


public class TouchTableScreen extends JFrame implements Runnable {

	/**
	 * A touchtable full screen jframe
	 */
	private static final long serialVersionUID = -966931575089952536L;
	private MBFImage image;
	private Mode mode;
	private Matrix transform;
	
	interface Mode{
		public class DRAWING implements Mode {

			private TouchTableScreen touchScreen;
			private ArrayList<Point2d> points;
			

			public DRAWING(TouchTableScreen touchScreen) {
				this.touchScreen = touchScreen;
				points = new ArrayList<Point2d>();
			}

			@Override
			public void acceptTouch(List<Point2d> filtered) {
				this.setDrawingPoints(filtered);
			}

			private synchronized void setDrawingPoints(List<Point2d> filtered) {
				this.points.addAll(filtered);
			}

			@Override
			public void drawToImage(MBFImage image) {
				List<Point2d> toDraw = this.getDrawingPoints();
				for (Point2d point2d : toDraw) {
					Point2d trans = point2d.transform(this.touchScreen.transform);
					image.drawPoint(trans, RGBColour.BLUE, 10);
				}
			}

			@SuppressWarnings("unchecked")
			private synchronized List<Point2d> getDrawingPoints() {
				List<Point2d> toRet = (List<Point2d>) this.points.clone();
				this.points.clear();
				return toRet;
			}

		}

		class CALIBRATION implements Mode{
			
			private static Point2d TOP_LEFT = null;
			private static Point2d TOP_RIGHT = null;
			private static Point2d BOTTOM_LEFT = null;
			private static Point2d BOTTOM_RIGHT = null;
			private ArrayList<Point2d> touchArray;
			private TouchTableScreen touchScreen;

			public CALIBRATION(TouchTableScreen touchTableScreen){
				this.touchArray = new ArrayList<Point2d>();
				TOP_LEFT = new Point2dImpl(30f,30f);
				TOP_RIGHT = new Point2dImpl(touchTableScreen.image.getWidth()-30f,30f);
				BOTTOM_LEFT = new Point2dImpl(30f,touchTableScreen.image.getHeight()-30f);
				BOTTOM_RIGHT = new Point2dImpl(touchTableScreen.image.getWidth()-30f,touchTableScreen.image.getHeight()-30f);
				this.touchScreen = touchTableScreen;
			}
			public void drawToImage(MBFImage image) {
				image.fill(RGBColour.WHITE);
				switch (this.touchArray.size()) {
				case 0:
					drawTarget(image,TOP_LEFT);
					break;
				case 1:
					drawTarget(image,TOP_RIGHT);
					break;
				case 2:
					drawTarget(image,BOTTOM_LEFT);
					break;
				case 3:
					drawTarget(image,BOTTOM_RIGHT);
					break;
				default:
					break;
				}
			}

			private void drawTarget(MBFImage image, Point2d point){
				image.drawPoint(point, RGBColour.RED, 10);
			}
			@Override
			public void acceptTouch(List<Point2d> filtered) {
				Point2d pixelToAdd = filtered.get(0);
				Point2d lastPointAdded = null;
				if(this.touchArray.size() != 0) lastPointAdded = this.touchArray.get(this.touchArray.size() - 1);
				if(
					lastPointAdded == null || 
					Line2d.distance(pixelToAdd, lastPointAdded) > TouchTableDemo.SMALLEST_POINT_DIAMETER
				) {
					this.touchArray.add(pixelToAdd);
				}
				
				if(this.touchArray.size() == 4){
					calibrate();
				}
			}
			private void calibrate() {
				HomographyModel m = new HomographyModel(10f);
				List<IndependentPair<Point2d, Point2d>> matches = new ArrayList<IndependentPair<Point2d, Point2d>>();
			
				matches.add(IndependentPair.pair(TOP_LEFT, this.touchArray.get(0)));
				matches.add(IndependentPair.pair(TOP_RIGHT, this.touchArray.get(1)));
				matches.add(IndependentPair.pair(BOTTOM_LEFT, this.touchArray.get(2)));
				matches.add(IndependentPair.pair(BOTTOM_RIGHT, this.touchArray.get(3)));
				
				m.estimate(matches);
				touchScreen.setMatrix(m.getTransform());
				touchScreen.mode = new Mode.DRAWING(touchScreen);
			}
		};

		public void drawToImage(MBFImage image );

		public void acceptTouch(List<Point2d> filtered);
	}
	
	public TouchTableScreen(){
		this.setUndecorated(true);
	}
	public void setMatrix(Matrix transform) {
		this.transform = transform;
	}
	public void init(){
		int width = this.getWidth();
		int height = this.getHeight();
		
		System.out.println("WidthxHeight = " + width + "x" + height);
		image = new MBFImage(width,height,ColourSpace.RGB);
		this.mode = new Mode.CALIBRATION(this);
		
		Thread t = new Thread(this);
		t.start();
	}
	public void touchEvent(List<Point2d> filtered) {
		this.mode.acceptTouch(filtered);
	}
	@Override
	public void run() {
		while(true){
			this.mode.drawToImage(this.image);
			DisplayUtilities.display(this.image, this);
		}
	}

}
