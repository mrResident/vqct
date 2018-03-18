package ru.resprojects.vqct

import mu.KotlinLogging
import org.im4java.core.ImageCommand
import org.im4java.core.ConvertCmd
import org.im4java.core.IdentifyCmd
import org.im4java.core.CompositeCmd
import org.im4java.core.CompareCmd
import org.im4java.core.IMOperation
import org.im4java.process.ArrayListErrorConsumer
import org.im4java.process.ArrayListOutputConsumer
import org.openqa.selenium.Dimension
import java.awt.Toolkit
import java.io.File
import java.io.FileNotFoundException

private val logger = KotlinLogging.logger {}

/**
 * ImageMagick utilities
 */
private enum class CommandType {
    CONVERT, IDENTIFY, COMPOSITECMD, COMPARE
}

/**
 * ImgaeMagick tools.
 * @param settings program settings
 */
class IMUtils(private val settings: ProgramSettings) {

    /**
     * Getting IM utilities.
     * @param commandType ImageMagick utilities
     * @return IM command
     */
    private fun getImageCommand(commandType: CommandType): ImageCommand {
        try {
            val imPath = settings.getImageMagick().absolutePath
            val imageCommand = when (commandType) {
                CommandType.CONVERT -> ConvertCmd(false)
                CommandType.IDENTIFY -> IdentifyCmd(false)
                CommandType.COMPOSITECMD -> CompositeCmd(false)
                CommandType.COMPARE -> CompareCmd(false)
            }
            imageCommand.searchPath = imPath
            return imageCommand
        } catch (err: FileNotFoundException) {
            throw FileNotFoundException("${err.message}. ImageMagick v 6.x.x is not found! For download ImageMagick please visit https://legacy.imagemagick.org/script/download.php")
        }
    }

    /**
     * Getting info from graphic file.
     * @param imageFile graphic file
     * @return image information
     */
    fun getImageInfo(imageFile: File): ImageInfo {
        if (!imageFile.exists()) {
            throw FileNotFoundException("Image file ${imageFile.absolutePath} not found!")
        }
        try {
            val operation = IMOperation()
            operation.format("%w,%h,%Q,%m")
            operation.addImage()
            val identyfyCmd = getImageCommand(CommandType.IDENTIFY)
            val output = ArrayListOutputConsumer()
            identyfyCmd.setOutputConsumer(output)
            identyfyCmd.run(operation, imageFile.absolutePath)
            val list = mutableListOf<String>()
            list.addAll(output.output[0].split(","))
            list.add(imageFile.absolutePath)
            return ImageInfo(list)
        } catch (err: Exception) {
            throw IllegalStateException("Error while reading image file ${imageFile.absolutePath}. ${err.message}")
        }
    }

    /**
     * Cropping input image image. For more information see IM manual https://www.imagemagick.org/Usage/crop/#crop
     * @param inputImage input image that needes cropping
     * @param outputImage output cropping image
     * @param dimension dimension (part or all image) for cropping
     * @param x count of pixels for cropping
     * @param y count of pixels for cropping
     * @return false if appeared error while image cropping
     */
    fun cropImage(inputImage: File, outputImage: File, dimension: Dimension, x: Int, y: Int): Boolean {
        if (!inputImage.exists()) {
            throw FileNotFoundException("Input file ${inputImage.absolutePath} not found!")
        }
        return try {
            val operation = IMOperation()
            operation.addImage(inputImage.absolutePath)
            operation.crop(dimension.getWidth(), dimension.getHeight(), x, y)
            operation.p_repage()
            operation.addImage(outputImage.absolutePath)
            val convert = getImageCommand(CommandType.CONVERT)
            convert.run(operation)
            true
        } catch (err: Exception) {
            logger.error(err) { "Error while cropping image: ${err.message}" }
            false
        }
    }

    /**
     * Trimming input image image. For more information see IM manual https://www.imagemagick.org/Usage/crop/#trim
     * @param inputImage input image that needes trimming
     * @param outputImage output trimmed image
     * @return false if appeared error while image trimming
     */
    fun trimImage(inputImage: File, outputImage: File): Boolean {
        if (!inputImage.exists()) {
            throw FileNotFoundException("Input file ${inputImage.absolutePath} not found!")
        }
        return try {
            val operation = IMOperation()
            operation.addImage(inputImage.absolutePath)
            operation.trim()
            operation.p_repage()
            operation.addImage(outputImage.absolutePath)
            val convert = getImageCommand(CommandType.CONVERT)
            convert.run(operation)
            true
        } catch (err: Exception) {
            logger.error(err) { "Error while trimming image: ${err.message}" }
            false
        }
    }

    /**
     * Resizing input image file. For more information see IM manual https://www.imagemagick.org/Usage/resize/#resize
     * @param inputImage input image that needes resizing
     * @param outputImage output resized image
     * @param width new image width
     * @param height new image height
     * @return false if appeared error while image resized
     */
    fun resizeImage(inputImage: File, outputImage: File, width: Int, height: Int): Boolean {
        if (!inputImage.exists()) {
            throw FileNotFoundException("Input file ${inputImage.absolutePath} not found!")
        }
        return try {
            val operation = IMOperation()
            operation.addImage(inputImage.absolutePath)
            operation.resize(width, height)
            operation.addImage(outputImage.absolutePath)
            val convert = getImageCommand(CommandType.CONVERT)
            convert.run(operation)
            true
        } catch (err: Exception) {
            logger.error(err) { "Error while resizing image: ${err.message}" }
            false
        }
    }

    /**
     * Comparing two image. For more information see IM manual https://www.imagemagick.org/Usage/compare/
     * @param inputImage_1 first image for comparing
     * @param inputImage_2 second image for comparing
     * @param outputImage output image with comparing result
     * @return absolute error count, number of different pixels
     */
    fun compareImage(inputImage_1: File, inputImage_2: File, outputImage: File): Int {
        if (!inputImage_1.exists()) {
            throw FileNotFoundException("Input file ${inputImage_1.absolutePath} not found!")
        }
        if (!inputImage_2.exists()) {
            throw FileNotFoundException("Input file ${inputImage_2.absolutePath} not found!")
        }
        val operation =  IMOperation()
        val compareCmd = getImageCommand(CommandType.COMPARE)
        val error = ArrayListErrorConsumer()
        compareCmd.setErrorConsumer(error)
        operation.metric("ae")
        operation.fuzz(settings.getImFuzz(), true)
        operation.addImage(inputImage_1.absolutePath)
        operation.addImage(inputImage_2.absolutePath)
        operation.addImage(outputImage.absolutePath)
        val isEquals = try {
            compareCmd.run(operation)
            true
        } catch (err: Exception) {
            false
        }
        return if (!isEquals) {
            if (!error.output.isEmpty()) {
                try {
                    error.output[0].toInt()
                } catch (err: NumberFormatException) {
                    -1
                }
            } else {
                -1
            }
        } else {
            0
        }
    }
}

/**
 * Image file information.
 * @param inputData image information (returned from IM)
 */
data class ImageInfo(private val inputData: List<String>) {

    /**
     * Image size (width and height).
     */
    val imageSize: Dimension
    /**
     * Image compression quality.
     */
    val imageQuality: Double
    /**
     * Image file format.
     */
    val imageFormat: String
    /**
     * Image file.
     */
    val imageFile: File

    init {
        try {
            imageSize = Dimension(inputData[0].toInt(), inputData[1].toInt())
            imageQuality = inputData[2].toDouble()
            imageFormat = inputData[3]
            imageFile = File(inputData[4])
        } catch (err: Exception) {
            throw IllegalStateException("Can't get image info.")
        }
    }

}

/**
 * Return current screen resolution.
 */
fun getCurrentScreenResolution(): Dimension {
    return Dimension(Toolkit.getDefaultToolkit().screenSize.width, Toolkit.getDefaultToolkit().screenSize.height)
}