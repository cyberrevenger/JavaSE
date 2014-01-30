package abu.video;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import javax.imageio.ImageIO;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.common.NIOUtils;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.containers.mp4.Brand;
import org.jcodec.containers.mp4.MP4Packet;
import org.jcodec.containers.mp4.TrackType;
import org.jcodec.containers.mp4.muxer.FramesMP4MuxerTrack;
import org.jcodec.containers.mp4.muxer.MP4Muxer;
import org.jcodec.scale.AWTUtil;
import org.jcodec.scale.RgbToYuv420;


public class SequenceEncoder {
    private SeekableByteChannel ch;
    private Picture toEncode;
    private RgbToYuv420 transform;
    private H264Encoder encoder;
    private ArrayList<ByteBuffer> spsList;
    private ArrayList<ByteBuffer> ppsList;
    private FramesMP4MuxerTrack outTrack;
    private ByteBuffer _out;
    private int frameNo;
    private MP4Muxer muxer;

    public SequenceEncoder(File out) throws IOException {
        this.ch = NIOUtils.writableFileChannel(out);

        // Transform to convert between RGB and YUV
        transform = new RgbToYuv420(0, 0);

        // Muxer that will store the encoded frames
        muxer = new MP4Muxer(ch, Brand.MP4);

        // Add video track to muxer
        outTrack = muxer.addTrackForCompressed(TrackType.VIDEO, 25);

        // Allocate a buffer big enough to hold output frames
        _out = ByteBuffer.allocate(1920 * 1080 * 6);

        // Create an instance of encoder
        encoder = new H264Encoder();

        // Encoder extra data ( SPS, PPS ) to be stored in a special place of
        // MP4
        spsList = new ArrayList<ByteBuffer>();
        ppsList = new ArrayList<ByteBuffer>();

    }

    public void encodeImage(BufferedImage bi) throws IOException {
        if (toEncode == null) {
            toEncode = Picture.create(bi.getWidth(), bi.getHeight(), ColorSpace.YUV420);
        }

        // Perform conversion
        for (int i = 0; i < 3; i++)
            Arrays.fill(toEncode.getData()[i], 0);
        transform.transform(AWTUtil.fromBufferedImage(bi), toEncode);

        // Encode image into H.264 frame, the result is stored in '_out' buffer
        _out.clear();
        ByteBuffer result = encoder.encodeFrame(_out, toEncode);

        // Based on the frame above form correct MP4 packet
        spsList.clear();
        ppsList.clear();
        H264Utils.encodeMOVPacket(result, spsList, ppsList);

        // Add packet to video track
        outTrack.addFrame(new MP4Packet(result, frameNo, 25, 1, frameNo, true, null, frameNo, 0));

        frameNo++;
    }

    public void finish() throws IOException {
        // Push saved SPS/PPS to a special storage in MP4
        outTrack.addSampleEntry(H264Utils.createMOVSampleEntry(spsList, ppsList));

        // Write MP4 header and finalize recording
        muxer.writeHeader();
        NIOUtils.closeQuietly(ch);
    }
    
    public static void main(String[] args) throws IOException {
        SequenceEncoder encoder = new SequenceEncoder(new File("video.mp4"));
        for (int i = 1; i < 100; i++) {
            //BufferedImage bi = ImageIO.read(new File(String.format("img%08d.png", i)));
        	
            encoder.encodeImage(getSnapShot(10));
        }
        encoder.finish();
        System.exit(0);
    }
    
	public static BufferedImage getSnapShot(int seconds) {

  		Robot robot = null;
  		BufferedImage bi = null;
   		// remember to catch AWTException
   		try {
      		robot =new Robot();
      		// delay the time
			robot.delay(seconds);

			// encode to jpg file
            //OutputStream f = new FileOutputStream(filename);
            //JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(f);
            //encoder.encode(robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize())));
			bi = robot.createScreenCapture(new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()));

     	}
     	catch(AWTException e1) {}
     	//catch(IOException e2) {}
		return bi;

}

 
}
