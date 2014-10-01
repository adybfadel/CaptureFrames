import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import com.xuggle.xuggler.Global;

public class DecodeAndCaptureFrames extends Thread {

	public final int NUMER_OF_FRAMES = 6;
	public final int SECONDS_BETWEEN_FRAMES = 10;
	private final String DIR_STREAM = "/usr/local/WowzaStreamingEngine/content/sonyGuru";
	private final String DIR_FRAMES = "/usr/share/tomcat7/webapps/sonyguru/imgs";
	
	private String streamFilename;

	public static void main(String[] args) {
		
		try {
			Runtime.getRuntime().exec("sudo su");
			System.out.println("sudo su");
		} catch (Exception e1) {
			e1.printStackTrace();
			System.exit(1);
		}
		
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
			String streamFileUrl = DIR_STREAM + "/" + streamFilename + ".mp4";
			String streamCopyUrl = DIR_STREAM + "/" + streamFilename + "_copy.mp4";
			try {
				System.out.println("Trying to open streaming file '" + streamFileUrl + "'...");
				File streamFile = new File(streamFileUrl);
				while (!streamFile.isFile()) { Thread.sleep(SECONDS_BETWEEN_FRAMES * 10000); }
//				Thread.sleep(120000);
				Process process = Runtime.getRuntime().exec("cp -rf " + streamFileUrl + " " + streamCopyUrl);
				process.waitFor();
				IMediaReader reader = ToolFactory.makeReader(streamCopyUrl);
				System.out.println("Slicing streaming file '" + streamCopyUrl + "' to '" + DIR_FRAMES + "'.");
				reader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);
				reader.addListener(new Capture(streamCopyUrl));
				while (reader.readPacket() == null) {}
				Runtime.getRuntime().exec("rm -rf " + streamCopyUrl);
				System.out.println("Finished slicing '" + streamCopyUrl + "' to '" + DIR_FRAMES + "'.");
			} catch (Exception e) {}	
		}
	}
	
	private void toSmall(File large, File small, int larg, int alt) throws IOException {
		
		BufferedImage bufImg = ImageIO.read(large);
		double altImg = bufImg.getHeight();
		double larImg = bufImg.getWidth();
		double escala = 1.0;
		
		if (altImg > larImg)
			escala = alt / altImg;
		else
			escala = larg / larImg;
		
		// So ajusta altera as dimencoes da imagem se form reducao
		if (escala < 1.0) {
			larImg = larImg * escala;
			altImg = altImg * escala;
		}
		
		Double novaLarg = Math.floor(larImg) + (Math.floor(larImg) == larImg ? 0 : 1);
		Double novaAlt = Math.floor(altImg) + (Math.floor(altImg) == altImg ? 0 : 1);
		int x = new Double((larg - novaLarg) / 2).intValue();
		int y = new Double((alt - novaAlt) / 2).intValue();
		
		BufferedImage bufSmall = new BufferedImage(larg, alt, BufferedImage.TYPE_INT_RGB);
		
		Graphics2D graph2D = bufSmall.createGraphics();
		graph2D.fillRect(0, 0, larg, alt);
		graph2D.drawImage(bufImg.getScaledInstance(novaLarg.intValue(), novaAlt.intValue(), Image.SCALE_SMOOTH), x, y, null);
		
		ImageIO.write(bufSmall, "JPG", small);
			
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

					File large = new File(DIR_FRAMES + "/" + frameFilename + "_" + count++ + ".png");
					ImageIO.write(event.getImage(), "png", large);
					
					// Reduce file dimension
					File small = new File(DIR_FRAMES + "/" + frameFilename + "_" + count++ + ".jpg");
					toSmall(large, small, 640, 480);
					
					if (count > NUMER_OF_FRAMES)
						count = 1;
					
					mLastPtsWrite += MICRO_SECONDS_BETWEEN_FRAMES;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}

}
