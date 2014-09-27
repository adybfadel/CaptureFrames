import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import com.xuggle.xuggler.Global;

public class DecodeAndCaptureFrames extends Thread {

	public final int SECONDS_BETWEEN_FRAMES = 5;
	private final String DIR_STREAM = "/usr/local/WowzaStreamingEngine/content/";
	private final String DIR_FRAMES = "/usr/share/tomcat7/webapps/sonyguru/imgs/";
	
	private String streamFilename;

	public static void main(String[] args) {
		new DecodeAndCaptureFrames("mesa1").start();
		new DecodeAndCaptureFrames("mesa2").start();
		new DecodeAndCaptureFrames("mesa3").start();
		new DecodeAndCaptureFrames("mesa4").start();
		new DecodeAndCaptureFrames("mesa5").start();
		new DecodeAndCaptureFrames("mesa6").start();
		new DecodeAndCaptureFrames("mesa7").start();
		new DecodeAndCaptureFrames("mesa8").start();
	}

	public DecodeAndCaptureFrames(String streamFilename) {
		this.streamFilename = streamFilename;
	}

	@Override
	public void run() {
		while (true) {
			String streamFileUrl = DIR_STREAM + streamFilename + ".mp4";
			try {
				System.out.println("Trying to open streaming file '" + streamFileUrl + "'...");
				while (!new File(streamFileUrl).isFile()) { Thread.sleep(SECONDS_BETWEEN_FRAMES * 1000); }
				IMediaReader reader = ToolFactory.makeReader(streamFileUrl);
				System.out.println("Slicing streaming file '" + streamFileUrl + "' to '" + DIR_FRAMES + "'.");
				reader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);
				reader.addListener(new Capture(streamFilename));
				while (reader.readPacket() == null) {}
			} catch (Exception e) {}			
			try {
				Thread.sleep(SECONDS_BETWEEN_FRAMES * 1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	
	
	private class Capture extends MediaListenerAdapter {
		
		private String frameFilename;
		private int count = 1;
		public final long MICRO_SECONDS_BETWEEN_FRAMES = (long) (Global.DEFAULT_PTS_PER_SECOND * SECONDS_BETWEEN_FRAMES);
		private long mLastPtsWrite = Global.NO_PTS;
		private int mVideoStreamIndex = -1;
		
		public Capture(String streamFilename) {
			this.frameFilename = streamFilename;
		}
		
		public void onVideoPicture(IVideoPictureEvent event) {
			
			try {

				if (event.getStreamIndex() != mVideoStreamIndex) {
					if (-1 == mVideoStreamIndex)
						mVideoStreamIndex = event.getStreamIndex();
					else
						return;
				}

				if (mLastPtsWrite == Global.NO_PTS)
					mLastPtsWrite = event.getTimeStamp() - MICRO_SECONDS_BETWEEN_FRAMES;

				if (event.getTimeStamp() - mLastPtsWrite >= MICRO_SECONDS_BETWEEN_FRAMES) {

					File file = new File(DIR_FRAMES + frameFilename + "_" + count++ + ".png");
					ImageIO.write(event.getImage(), "png", file);

					if (count > 5)
						count = 1;
					
					mLastPtsWrite += MICRO_SECONDS_BETWEEN_FRAMES;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}

}
