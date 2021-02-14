package wikigen.image;

import arc.files.Fi;
import arc.graphics.g2d.TextureAtlas.TextureAtlasData;
import arc.graphics.g2d.TextureAtlas.TextureAtlasData.AtlasPage;
import arc.graphics.g2d.TextureAtlas.TextureAtlasData.Region;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/** Unpacks the default Mindustry atlas. Code taken and modified from libGDX source. */
public class TextureUnpacker{
    private static final int NINEPATCH_PADDING = 1;
    private static final String OUTPUT_TYPE = "png";

    public void split(Fi atlasFile, Fi output){
        try{
            new TextureUnpacker().splitAtlas(new TextureAtlasData(atlasFile, atlasFile.parent(), false), output.file().getAbsolutePath());
        }catch(IOException e){
            throw new RuntimeException(e);
        }
    }

    /** Splits an atlas into seperate image and ninepatch files. */
    private void splitAtlas(TextureAtlasData atlas, String outputDir) throws IOException{
        // create the output directory if it did not exist yet
        File outputDirFile = new File(outputDir);
        if(!outputDirFile.exists()){
            outputDirFile.mkdirs();
        }

        for(AtlasPage page : atlas.getPages()){
            // load the image file belonging to this page as a Buffered Image
            File file = page.textureFile.file();
            if(!file.exists()) throw new RuntimeException("Unable to find atlas image: " + file.getAbsolutePath());
            BufferedImage img = ImageIO.read(file);
            for(Region region : atlas.getRegions()){

                // check if the page this region is in is currently loaded in a Buffered Image
                if(region.page == page){
                    BufferedImage splitImage;
                    String extension;

                    // check if the region is a ninepatch or a normal image and delegate accordingly
                    if(region.splits == null){
                        splitImage = extractImage(img, region, 0);
                        if(region.width != region.originalWidth || region.height != region.originalHeight){
                            BufferedImage originalImg = new BufferedImage(region.originalWidth, region.originalHeight, img.getType());
                            Graphics2D g2 = originalImg.createGraphics();
                            g2.drawImage(splitImage, (int)region.offsetX, (int)(region.originalHeight - region.height - region.offsetY), null);
                            g2.dispose();
                            splitImage = originalImg;
                        }
                        extension = OUTPUT_TYPE;
                    }else{
                        splitImage = extractNinePatch(img, region, outputDirFile);
                        extension = String.format("9.%s", OUTPUT_TYPE);
                    }

                    // check if the parent directories of this image file exist and create them if not
                    File imgOutput = new File(outputDirFile, String.format("%s.%s", region.index == -1 ? region.name : region.name + "_" + region.index, extension));
                    File imgDir = imgOutput.getParentFile();
                    if(!imgDir.exists()){
                        imgDir.mkdirs();
                    }

                    ImageIO.write(splitImage, OUTPUT_TYPE, imgOutput);
                }
            }
        }
    }

    /**
     * Extract an image from a texture atlas.
     * @param page The image file related to the page the region is in
     * @param region The region to extract
     * @param padding padding (in pixels) to apply to the image
     * @return The extracted image
     */
    private BufferedImage extractImage(BufferedImage page, Region region, int padding){
        BufferedImage splitImage = null;

        // get the needed part of the page and rotate if needed
        if(region.rotate){
            BufferedImage srcImage = page.getSubimage(region.left, region.top, region.height, region.width);
            splitImage = new BufferedImage(region.width, region.height, page.getType());

            AffineTransform transform = new AffineTransform();
            transform.rotate(Math.toRadians(90.0));
            transform.translate(0, -region.width);
            AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BILINEAR);
            op.filter(srcImage, splitImage);
        }else{
            splitImage = page.getSubimage(region.left, region.top, region.width, region.height);
        }

        // draw the image to a bigger one if padding is needed
        if(padding > 0){
            BufferedImage paddedImage = new BufferedImage(splitImage.getWidth() + padding * 2, splitImage.getHeight() + padding * 2,
            page.getType());
            Graphics2D g2 = paddedImage.createGraphics();
            g2.drawImage(splitImage, padding, padding, null);
            g2.dispose();
            return paddedImage;
        }else{
            return splitImage;
        }
    }

    /**
     * Extract a ninepatch from a texture atlas, according to the android specification.
     * @param page The image file related to the page the region is in
     * @param region The region to extract
     * @see <a href="http://developer.android.com/guide/topics/graphics/2d-graphics.html#nine-patch">ninepatch specification</a>
     */
    private BufferedImage extractNinePatch(BufferedImage page, Region region, File outputDirFile){
        BufferedImage splitImage = extractImage(page, region, NINEPATCH_PADDING);
        Graphics2D g2 = splitImage.createGraphics();
        g2.setColor(Color.BLACK);

        // Draw the four lines to save the ninepatch's padding and splits
        int startX = region.splits[0] + NINEPATCH_PADDING;
        int endX = region.width - region.splits[1] + NINEPATCH_PADDING - 1;
        int startY = region.splits[2] + NINEPATCH_PADDING;
        int endY = region.height - region.splits[3] + NINEPATCH_PADDING - 1;
        if(endX >= startX) g2.drawLine(startX, 0, endX, 0);
        if(endY >= startY) g2.drawLine(0, startY, 0, endY);
        if(region.pads != null){
            int padStartX = region.pads[0] + NINEPATCH_PADDING;
            int padEndX = region.width - region.pads[1] + NINEPATCH_PADDING - 1;
            int padStartY = region.pads[2] + NINEPATCH_PADDING;
            int padEndY = region.height - region.pads[3] + NINEPATCH_PADDING - 1;
            g2.drawLine(padStartX, splitImage.getHeight() - 1, padEndX, splitImage.getHeight() - 1);
            g2.drawLine(splitImage.getWidth() - 1, padStartY, splitImage.getWidth() - 1, padEndY);
        }
        g2.dispose();

        return splitImage;
    }
}
