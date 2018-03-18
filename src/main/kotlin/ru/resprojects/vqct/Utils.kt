package ru.resprojects.vqct

import ru.resprojects.ArgumentsData
import mu.KotlinLogging
import org.apache.commons.io.FileUtils
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.Properties
import kotlin.math.roundToInt

private val logger = KotlinLogging.logger {}

enum class TypeOfOS {
    LINUX, WINDOWS
}

enum class Browsers {
    CHROME, FIREFOX, OPERA, IE
}

/**
 * Get OS name from system settings.
 */
fun getOSName(): TypeOfOS {
    val osName = System.getProperties().getProperty("os.name").toLowerCase().split(" ")
    return if (!osName.isEmpty()) {
        when (osName[0]) {
            "linux" -> TypeOfOS.LINUX
            "windows" -> TypeOfOS.WINDOWS
            else -> {
                throw IllegalStateException("Unsupported OS")
            }
        }
    } else {
        throw IllegalStateException("Unknown OS")
    }
}

/**
 * Generating output file.
 * @param inputFile source input file with the full path and name
 * @param outputDir output directory
 * @param postfix addition line for output file name
 * @param extension extension for output file name
 * @param return output file
 */
fun outputFileConstructor(inputFile: File, outputDir: String,
                          postfix: String = "", extension: String = "png"): File {
    return File(outputDir, "${inputFile.name.replace('.', '_')}$postfix.$extension")
}

/**
 * Class for storage program settings.
 * @param setupWizard run setup wizard
 * @param settingsFile load custom settings file if setupWizard = false
 * If setupWizard = true and settingsFile is not null then settings file
 * created in custom else settings file created in default file.
 */
class ProgramSettings(setupWizard: Boolean = false, settingsFile: File? = null) {

    private val prop = Properties()
    private val defaultSettingsName = "vqct.properties"

    init {
        if (setupWizard) {
            if (settingsFile != null) {
                resetSettings(settingsFile, setupWizard)
            } else {
                resetSettings(File(defaultSettingsName), setupWizard)
            }
        } else {
            if (settingsFile != null) {
                if (settingsFile.exists()) {
                    loadSettings(settingsFile)
                } else {
                    resetSettings(settingsFile)
                }
            } else {
                if (File(defaultSettingsName).exists()) {
                    loadSettings()
                } else {
                    resetSettings(File(defaultSettingsName))
                }
            }
        }
    }

    /**
     * Get file from settings.
     * @param name settings key
     * @throws FileNotFoundException if keys value is empty or file from settings value not found
     */
    private fun getFile(name: String): File {
        return if (prop.getProperty(name) != null && prop.getProperty(name).isNotEmpty()) {
            val file = File(prop.getProperty(name))
            if (file.exists()) {
                file
            } else {
                throw FileNotFoundException("File ${file.absolutePath} not found")
            }
        } else {
            throw FileNotFoundException("Can't get file because parameter \"$name\" in settings file is empty.")
        }
    }

    /**
     * Get browser execute file from settings.
     * @param browsers type of browser
     * @throws IllegalStateException if settings file is empty or not found
     */
    fun getBrowser(browsers: Browsers): File {
        if (prop.isEmpty) {
            throw IllegalStateException("Property file is empty or not found.")
        }
        return when(browsers) {
            Browsers.CHROME -> getFile(browsers.name)
            Browsers.FIREFOX -> getFile(browsers.name)
            Browsers.OPERA -> getFile(browsers.name)
            Browsers.IE -> getFile(browsers.name)
        }
    }

    /**
     * Get selenium browser execute file from settings.
     * @param browsers type of browser
     * @throws IllegalStateException if settings file is empty or not found
     */
    fun getWebDriver(browsers: Browsers): File {
        if (prop.isEmpty) {
            throw IllegalStateException("Property file is empty or not found.")
        }
        return when(browsers) {
            Browsers.CHROME -> getFile("${browsers.name}_WEBDRIVER")
            Browsers.FIREFOX -> getFile("${browsers.name}_WEBDRIVER")
            Browsers.OPERA -> getFile("${browsers.name}_WEBDRIVER")
            Browsers.IE -> getFile("${browsers.name}_WEBDRIVER")
        }
    }

    /**
     * Get ImageMagick execute file from settings.
     * @throws IllegalStateException if settings file is empty or not found
     * @throws FileNotFoundException if keys value is empty or file from settings value not found
     */
    fun getImageMagick():File {
        if (prop.isEmpty) {
            throw IllegalStateException("Property file is empty or not found.")
        }
        return if (prop.getProperty("IMAGEMAGICK") != null && prop.getProperty("IMAGEMAGICK").isNotEmpty()) {
            val file = File(prop.getProperty("IMAGEMAGICK"))
            if (file.exists()) {
                file
            } else {
                throw FileNotFoundException("File ${file.absolutePath} not found.")
            }
        } else {
            throw FileNotFoundException("Can't get file because parameter \"IMAGEMAGICK\" in settings file is empty.")
        }
    }

    /**
     * ImageMagick compare value fuzz. Fuzz is "Fuzz Factor" using by ImageMagick to
     * ignore minor differences between the two images.
     */
    fun getImFuzz(): Double {
        if (prop.isEmpty) {
            throw IllegalStateException("Property file is empty or not found.")
        }
        return if (prop.getProperty("imCompareFuzzValue") != null && prop.getProperty("imCompareFuzzValue").isNotEmpty()) {
            try {
                prop.getProperty("imCompareFuzzValue").toDouble()
            } catch (err: NumberFormatException) {
                throw IllegalStateException("Value of parameter \"imCompareFuzzValue\" is not Double.")
            }
        } else {
            throw IllegalStateException("Parameter \"imCompareFuzzValue\" in settings file is empty.")
        }
    }

    /**
     * Threshold value for count of the actual number of pixels that were masked,
     * at the current "Fuzz Factor". This value used by ImageMagick for compare images.
     */
    fun getImErrPixelCountThreshold(): Int {
        if (prop.isEmpty) {
            throw IllegalStateException("Property file is empty or not found.")
        }
        return if (prop.getProperty("imCompareErrCountPixelThreshold") != null && prop.getProperty("imCompareErrCountPixelThreshold").isNotEmpty()) {
            try {
                prop.getProperty("imCompareErrCountPixelThreshold").toInt()
            } catch (err: NumberFormatException) {
                throw IllegalStateException("Value of parameter \"imCompareErrCountPixelThreshold\" is not Integer.")
            }
        } else {
            throw IllegalStateException("Parameter \"imCompareErrCountPixelThreshold\" in settings file is empty.")
        }
    }

    /**
     * Load settings from file. If file not found then created new settings file
     * in current directory.
     * @param settingsFile full path to settings file. If the file is not specified,
     * then an attempt is made to load the settings from the vqct.properties file,
     * which is in the same directory as the executable file.
     */
    private fun loadSettings(settingsFile: File = File(defaultSettingsName)) {
        prop.clear()
        if (settingsFile.exists()) {
            prop.load(settingsFile.inputStream())
        } else {
            throw FileNotFoundException("Settings file ${settingsFile.absolutePath} is not found.")
        }
    }

    /**
     * Reset the program settings. If the settings file is not found,
     * then a new file is created, otherwise the new data is saved
     * in the current file.
     * @param settingsFile full path to settings file. If file is not specified,
     * then settings save to default file vqct.properties, which is in the same
     * directory as the executable file.
     */
    private fun resetSettings(settingsFile: File, setupWizard: Boolean = false) {
        prop.clear()
        var str: String
        var file: File
        if (setupWizard) {
            println("Start setup wizard. Current OS ${getOSName()}")
        }
        when(getOSName()) {
            TypeOfOS.LINUX -> {
                if (setupWizard) {
                    file = File("/usr/bin/google-chrome")
                    println("1. Path to CHROME browser [default ${if (file.exists())
                        file.absolutePath else "not found"}]. Enter for use default path or input yor path: ")
                    str = readLine()!!
                    prop.setProperty(Browsers.CHROME.name, if (str.isNotEmpty()) str else if (file.exists()) file.absolutePath else "")
                    file = File("/usr/bin/opera")
                    println("2. Path to OPERA browser [default ${if (file.exists())
                        file.absolutePath else "not found"}]. Enter for use default path or input yor path: ")
                    str = readLine()!!
                    prop.setProperty(Browsers.OPERA.name, if (str.isNotEmpty()) str else if (file.exists()) file.absolutePath else "")
                    file = File("/usr/bin/firefox")
                    println("3. Path to FIREFOX browser [default ${if (file.exists())
                        file.absolutePath else "not found"}]. Enter for use default path or input yor path: ")
                    str = readLine()!!
                    prop.setProperty(Browsers.FIREFOX.name, if (str.isNotEmpty()) str else if (file.exists()) file.absolutePath else "")
                    file = File("chromedriver")
                    println("4. Path to selenium webdriver for CHROME [default ${if (file.exists())
                        file.absolutePath else "not found"}]. Enter for use default path or input yor path: ")
                    str = readLine()!!
                    prop.setProperty("${Browsers.CHROME.name}_WEBDRIVER", if (str.isNotEmpty()) str else if (file.exists()) file.absolutePath else "")
                    file = File("operadriver")
                    println("5. Path to selenium webdriver for OPERA [default ${if (file.exists())
                        file.absolutePath else "not found"}]. Enter for use default path or input yor path: ")
                    str = readLine()!!
                    prop.setProperty("${Browsers.OPERA.name}_WEBDRIVER", if (str.isNotEmpty()) str else if (file.exists()) file.absolutePath else "")
                    file = File("geckodriver")
                    println("6. Path to selenium webdriver for FIREFOX [default ${if (file.exists())
                        file.absolutePath else "not found"}]. Enter for use default path or input yor path: ")
                    str = readLine()!!
                    prop.setProperty("${Browsers.FIREFOX.name}_WEBDRIVER", if (str.isNotEmpty()) str else if (file.exists()) file.absolutePath else "")
                    file = File("/usr/bin/convert")
                    println("7. Path to ImageMagick [default ${if (file.exists())
                        file.absolutePath else "not found"}]. Enter for use default path or input yor path: ")
                    str = readLine()!!
                    prop.setProperty("IMAGEMAGICK", if (str.isNotEmpty()) str else if (file.exists()) "/usr/bin" else "")
                } else {
                    prop.setProperty(Browsers.CHROME.name, if (File("/usr/bin/google-chrome").exists()) "/usr/bin/google-chrome" else "")
                    prop.setProperty(Browsers.OPERA.name, if (File("/usr/bin/opera").exists()) "/usr/bin/opera" else "")
                    prop.setProperty(Browsers.FIREFOX.name, if (File("/usr/bin/firefox").exists()) "/usr/bin/firefox" else "")
                    file = File("chromedriver")
                    prop.setProperty("${Browsers.CHROME.name}_WEBDRIVER", if (file.exists()) file.absolutePath else "")
                    file = File("operadriver")
                    prop.setProperty("${Browsers.OPERA.name}_WEBDRIVER", if (file.exists()) file.absolutePath else "")
                    file = File("geckodriver")
                    prop.setProperty("${Browsers.FIREFOX.name}_WEBDRIVER", if (file.exists()) file.absolutePath else "")
                    prop.setProperty("IMAGEMAGICK", if (File("/usr/bin/convert").exists()) "/usr/bin" else "")
                }
            }
            TypeOfOS.WINDOWS -> {
                if (setupWizard) {
                    file = File("C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe")
                    if (!file.exists())
                        file = File("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe")
                    println("1. Path to CHROME browser [default ${if (file.exists())
                        file.absolutePath else "not found"}]. Enter for use default path or input yor path: ")
                    str = readLine()!!
                    prop.setProperty(Browsers.CHROME.name, if (str.isNotEmpty()) str else if (file.exists()) file.absolutePath else "")
                    file = File("C:\\Program Files (x86)\\Opera\\launcher.exe")
                    if (!file.exists())
                        file = File("C:\\Program Files\\Opera\\launcher.exe")
                    println("2. Path to OPERA browser [default ${if (file.exists())
                        file.absolutePath else "not found"}]. Enter for use default path or input yor path: ")
                    str = readLine()!!
                    prop.setProperty(Browsers.OPERA.name, if (str.isNotEmpty()) str else if (file.exists()) file.absolutePath else "")
                    file = File("C:\\Program Files (x86)\\Mozilla Firefox\\firefox.exe")
                    if (!file.exists())
                        file = File("C:\\Program Files\\Mozilla Firefox\\firefox.exe")
                    println("3. Path to FIREFOX browser [default ${if (file.exists())
                        file.absolutePath else "not found"}]. Enter for use default path or input yor path: ")
                    str = readLine()!!
                    prop.setProperty(Browsers.FIREFOX.name, if (str.isNotEmpty()) str else if (file.exists()) file.absolutePath else "")
                    file = File("C:\\Program Files\\Internet Explorer\\iexplore.exe")
                    println("4. Path to IE browser [default ${if (file.exists())
                        file.absolutePath else "not found"}]. Enter for use default path or input yor path: ")
                    str = readLine()!!
                    prop.setProperty(Browsers.IE.name, if (str.isNotEmpty()) str else if (file.exists()) file.absolutePath else "")
                    file = File("chromedriver.exe")
                    println("5. Path to selenium webdriver for CHROME [default ${if (file.exists())
                        file.absolutePath else "not found"}]. Enter for use default path or input yor path: ")
                    str = readLine()!!
                    prop.setProperty("${Browsers.CHROME.name}_WEBDRIVER", if (str.isNotEmpty()) str else if (file.exists()) file.absolutePath else "")
                    file = File("operadriver.exe")
                    println("6. Path to selenium webdriver for OPERA [default ${if (file.exists())
                        file.absolutePath else "not found"}]. Enter for use default path or input yor path: ")
                    str = readLine()!!
                    prop.setProperty("${Browsers.OPERA.name}_WEBDRIVER", if (str.isNotEmpty()) str else if (file.exists()) file.absolutePath else "")
                    file = File("geckodriver.exe")
                    println("7. Path to selenium webdriver for FIREFOX [default ${if (file.exists())
                        file.absolutePath else "not found"}]. Enter for use default path or input yor path: ")
                    str = readLine()!!
                    prop.setProperty("${Browsers.FIREFOX.name}_WEBDRIVER", if (str.isNotEmpty()) str else if (file.exists()) file.absolutePath else "")
                    file = File("IEDriverServer.exe")
                    println("8. Path to selenium webdriver for IE [default ${if (file.exists())
                        file.absolutePath else "not found"}]. Enter for use default path or input yor path: ")
                    str = readLine()!!
                    prop.setProperty("${Browsers.IE.name}_WEBDRIVER", if (str.isNotEmpty()) str else if (file.exists()) file.absolutePath else "")
                    file = File("C:\\Program Files (x86)\\ImageMagick-6.9.9-Q16")
                    if (!file.exists())
                        file = File("C:\\Program Files\\ImageMagick-6.9.9-Q16")
                    println("9. Path to ImageMagick [default ${if (file.exists())
                        file.absolutePath else "not found"}]. Enter for use default path or input yor path: ")
                    str = readLine()!!
                    prop.setProperty("IMAGEMAGICK", if (str.isNotEmpty()) str else if (file.exists()) file.absolutePath else "")
                } else {
                    file = File("C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe")
                    if (!file.exists())
                        file = File("C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe")
                    prop.setProperty(Browsers.CHROME.name, if (file.exists()) file.absolutePath else "")
                    file = File("C:\\Program Files (x86)\\Opera\\launcher.exe")
                    if (!file.exists())
                        file = File("C:\\Program Files\\Opera\\launcher.exe")
                    prop.setProperty(Browsers.OPERA.name, if (file.exists()) file.absolutePath else "")
                    file = File("C:\\Program Files (x86)\\Mozilla Firefox\\firefox.exe")
                    if (!file.exists())
                        file = File("C:\\Program Files\\Mozilla Firefox\\firefox.exe")
                    prop.setProperty(Browsers.FIREFOX.name, if (file.exists()) file.absolutePath else "")
                    file = File("C:\\Program Files\\Internet Explorer\\iexplore.exe")
                    prop.setProperty(Browsers.IE.name, if (file.exists()) file.absolutePath else "")
                    file = File("chromedriver.exe")
                    prop.setProperty("${Browsers.CHROME.name}_WEBDRIVER", if (file.exists()) file.absolutePath else "")
                    file = File("operadriver.exe")
                    prop.setProperty("${Browsers.OPERA.name}_WEBDRIVER", if (file.exists()) file.absolutePath else "")
                    file = File("geckodriver.exe")
                    prop.setProperty("${Browsers.FIREFOX.name}_WEBDRIVER", if (file.exists()) file.absolutePath else "")
                    file = File("IEDriverServer.exe")
                    prop.setProperty("${Browsers.IE.name}_WEBDRIVER", if (file.exists()) file.absolutePath else "")
                    file = File("C:\\Program Files\\ImageMagick-6.9.9-Q16")
                    prop.setProperty("IMAGEMAGICK", if (file.exists()) file.absolutePath else "")
                }
            }
        }
        if (setupWizard) {
            println("Enter compare Fuzz value [default 15]. Enter for use default path or use own value: ")
            str = readLine()!!
            prop.setProperty("imCompareFuzzValue", if (str.isNotEmpty()) str else "15")
            println("Enter compare compare error count pixel threshold value [default 500]. Enter for use default path or use own value: ")
            str = readLine()!!
            prop.setProperty("imCompareErrCountPixelThreshold", if (str.isNotEmpty()) str else "500")
        } else {
            prop.setProperty("imCompareFuzzValue", "15")
            prop.setProperty("imCompareErrCountPixelThreshold", "500")
        }
        try {
            prop.store(settingsFile.outputStream(), null)
        } catch (err: IOException) {
            throw IllegalStateException("Can't save program settings in file ${settingsFile.absolutePath}")
        }
    }
}

class VQCT(private val argumentsData: ArgumentsData) {

    private val settings = ProgramSettings(argumentsData.isSetup, argumentsData.settingsFile)
    private val imUtils = IMUtils(settings)

    init {
        logger.info { "Init" }
        if (!((argumentsData.svgFile == null || argumentsData.basePngFile == null) && argumentsData.isSetup)) {
            if (imUtils.getImageInfo(argumentsData.svgFile!!).imageFile.extension.toLowerCase() != "svg") {
                throw IllegalStateException("Can not get info from ${argumentsData.svgFile!!.absolutePath}. May be file is not svg format or file is corrupted!")
            }
            if (imUtils.getImageInfo(argumentsData.basePngFile!!).imageFile.extension.toLowerCase() != "png") {
                throw IllegalStateException("Can not get info from ${argumentsData.basePngFile!!.absolutePath}. May be file is not png format or file is corrupted!")
            }
        }
    }

    fun launch() {
        if ((argumentsData.svgFile == null || argumentsData.basePngFile == null) && argumentsData.isSetup) {
            return
        }
        logger.info { "Start processing" }
        logger.info { "Checking the base png file. If the base png file is larger than the screen resolution, then need to change the picture size of the base png file to the current screen size." }
        val outFile = prepareBasePNG(argumentsData.basePngFile!!)
        val resultMessages = mutableListOf<String>()
        var comparingResult: Boolean
        argumentsData.browsers!!.forEach {
            when(it.toUpperCase()) {
                Browsers.CHROME.name -> {
                    logger.info { "1. Open the svg file in the Chrome / Chromium browser and take a screenshot from this browser." }
                    try {
                        val screenByChrome = outputFileConstructor(argumentsData.svgFile!!, argumentsData.outputDirectory!!.absolutePath, "-screen-by-chrome")
                        FileUtils.copyFile(
                                SvgRenderByChrome(settings, argumentsData.svgFile!!).getScreenshot(imUtils.getImageInfo(outFile).imageSize),
                                screenByChrome
                        )
                        comparingResult = comparing(outFile, screenByChrome)
                        logger.info { "Result of the comparing base file ${argumentsData.basePngFile!!.name} with screenshot getting from CHROME: ${if (comparingResult) "is equals" else "is not equals"}" }
                        resultMessages.add("Result of the comparing base file ${argumentsData.basePngFile!!.name} with screenshot getting from CHROME: ${if (comparingResult) "is equals" else "is not equals"}")
                    } catch (err: Exception) {
                        logger.error(err) { "Result of the comparing base file ${argumentsData.basePngFile!!.name} with screenshot getting from OPERA: ERROR! Info: ${err.message}" }
                        resultMessages.add("Result of the comparing base file ${argumentsData.basePngFile!!.name} with screenshot getting from OPERA: ERROR! For more information see logger.log file.")
                    }
                }
                Browsers.FIREFOX.name -> {
                    logger.info {"2. Open the svg file in the Firefox browser and take a screenshot from this browser."}
                    try {
                        val screenByFirefox = outputFileConstructor(argumentsData.svgFile!!, argumentsData.outputDirectory!!.absolutePath, "-screen-by-firefox")
                        FileUtils.copyFile(
                                SvgRenderByFirefox(settings, argumentsData.svgFile!!).getScreenshot(imUtils.getImageInfo(outFile).imageSize),
                                screenByFirefox
                        )
                        comparingResult = comparing(outFile, screenByFirefox)
                        logger.info { "Result of the comparing base file ${argumentsData.basePngFile!!.name} with screenshot getting from FIREFOX: ${if (comparingResult) "is equals" else "is not equals"}" }
                        resultMessages.add("Result of the comparing base file ${argumentsData.basePngFile!!.name} with screenshot getting from FIREFOX: ${if (comparingResult) "is equals" else "is not equals"}")
                    } catch (err: Exception) {
                        logger.error(err) { "Result of the comparing base file ${argumentsData.basePngFile!!.name} with screenshot getting from OPERA: ERROR! Info: ${err.message}" }
                        resultMessages.add("Result of the comparing base file ${argumentsData.basePngFile!!.name} with screenshot getting from OPERA: ERROR! For more information see logger.log file.")
                    }
                }
                Browsers.OPERA.name -> {
                    logger.info { "3. Open the svg file in the Opera browser and take a screenshot from this browser." }
                    try {
                        val screenByOpera = outputFileConstructor(argumentsData.svgFile!!, argumentsData.outputDirectory!!.absolutePath, "-screen-by-opera")
                        FileUtils.copyFile(
                                SvgRenderByOpera(settings, argumentsData.svgFile!!).getScreenshot(imUtils.getImageInfo(outFile).imageSize),
                                screenByOpera
                        )
                        comparingResult = comparing(outFile, screenByOpera)
                        logger.info { "Result of the comparing base file ${argumentsData.basePngFile!!.name} with screenshot getting from OPERA: ${if (comparingResult) "is equals" else "is not equals"}" }
                        resultMessages.add("Result of the comparing base file ${argumentsData.basePngFile!!.name} with screenshot getting from OPERA: ${if (comparingResult) "is equals" else "is not equals"}")
                    } catch (err: Exception) {
                        logger.error(err) { "Result of the comparing base file ${argumentsData.basePngFile!!.name} with screenshot getting from OPERA: ERROR! Info: ${err.message}" }
                        resultMessages.add("Result of the comparing base file ${argumentsData.basePngFile!!.name} with screenshot getting from OPERA: ERROR! For more information see logger.log file.")
                    }
                }
                Browsers.IE.name -> {
                    if (getOSName() == TypeOfOS.WINDOWS) {
                        logger.info { "4. Open the svg file in the IE browser and take a screenshot from this browser." }
                        try {
                            val screenByIE = outputFileConstructor(argumentsData.svgFile!!, argumentsData.outputDirectory!!.absolutePath, "-screen-by-ie")
                            FileUtils.copyFile(
                                    SvgRenderByIE(settings, argumentsData.svgFile!!).getScreenshot(imUtils.getImageInfo(outFile).imageSize),
                                    screenByIE
                            )
                            comparingResult = comparing(outFile, screenByIE)
                            logger.info { "Result of the comparing base file ${argumentsData.basePngFile!!.name} with screenshot getting from IE: ${if (comparingResult) "is equals" else "is not equals"}" }
                            resultMessages.add("Result of the comparing base file ${argumentsData.basePngFile!!.name} with screenshot getting from IE: ${if (comparingResult) "is equals" else "is not equals"}")
                        } catch (err: Exception) {
                            logger.error(err) { "Result of the comparing base file ${argumentsData.basePngFile!!.name} with screenshot getting from IE: ERROR! Info: ${err.message}" }
                            resultMessages.add("Result of the comparing base file ${argumentsData.basePngFile!!.name} with screenshot getting from IE: ERROR! For more information see logger.log file.")
                        }
                    }
                }
            }
        }
        resultMessages.add("Output directory: ${argumentsData.outputDirectory!!.absolutePath}")
        resultMessages.forEach { println(it) }
    }

    /**
     * Analized base png file and if need this file may be modified.
     * If dimension of base png file more than current screen resolution, then
     * base png file dimension will be resized to the current screen size.
     * @param basePNG - base PNG file
     * @return modified base PNG file.
     */
    private fun prepareBasePNG(basePNG: File): File {
        val basePNGInfo = imUtils.getImageInfo(basePNG)
        val widthPercentage = if (basePNGInfo.imageSize.getWidth() > getCurrentScreenResolution().getWidth()) {
            (((getCurrentScreenResolution().getWidth() - 100) / (basePNGInfo.imageSize.getWidth() * 1.0f)) * 100).roundToInt()
        }  else {
            100
        }
        val heightPercentage = if (basePNGInfo.imageSize.getHeight() > getCurrentScreenResolution().getHeight()) {
            (((getCurrentScreenResolution().getHeight() - 100) / (basePNGInfo.imageSize.getHeight() * 1.0f)) * 100).roundToInt()
        } else {
            100
        }
        val percentage = when {
            widthPercentage < heightPercentage -> widthPercentage
            widthPercentage > heightPercentage -> heightPercentage
            widthPercentage == heightPercentage -> widthPercentage
            else -> 100
        }
        FileUtils.forceMkdir(argumentsData.outputDirectory!!)
        return when {
            percentage == 100 -> {
                logger.info { "Size picture of the base png file staying without changes." }
                basePNG
            }
            percentage != 100 -> {
                val outFile = outputFileConstructor(basePNG, argumentsData.outputDirectory!!.absolutePath, postfix = "-resize")
                if (imUtils.resizeImage(
                                basePNG,
                                outFile,
                                width = (basePNGInfo.imageSize.getWidth() * percentage / (100 * 1.0f)).roundToInt(),
                                height = (basePNGInfo.imageSize.getHeight() * percentage / (100 * 1.0f)).roundToInt()
                        )) {
                    logger.info { "Size picture of the base png file changed from ${basePNGInfo.imageSize} to the ${imUtils.getImageInfo(outFile).imageSize}" }
                    outFile
                } else {
                    logger.info { "Size picture of the base png file staying without changes." }
                    basePNG
                }
            }
            else -> {
                logger.info { "Size picture of the base png file staying without changes." }
                basePNG
            }
        }
    }

    /**
     * Comparing base image with screenshots from browsers.
     * @param basePNG
     * @param screenshot
     * @return result png file.
     */
    private fun comparing(basePNG: File, screenshot: File): Boolean {
        val result = imUtils.compareImage(
                basePNG,
                screenshot,
                outputFileConstructor(screenshot, argumentsData.outputDirectory!!.absolutePath, "-compare-result")
        )
        logger.info { "Comparing base png file ${basePNG.absolutePath} with screenshot file ${screenshot.absolutePath}. Current absolute error count (number of different pixels) is equals to $result (current threshold = ${settings.getImErrPixelCountThreshold()})" }
        return result in 0..settings.getImErrPixelCountThreshold()
    }
}