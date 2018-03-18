package ru.resprojects

import ru.resprojects.vqct.Browsers
import ru.resprojects.vqct.ProgramSettings
import org.junit.Assume
import org.openqa.selenium.Dimension
import ru.resprojects.vqct.IMUtils
import ru.resprojects.vqct.ImageInfo
import java.io.*
import java.util.*
import kotlin.test.*

class MainTest {

    private val testSvg = Thread.currentThread().contextClassLoader.getResource("gallardo.svg").toURI().path
    private val testPng = Thread.currentThread().contextClassLoader.getResource("gallardo.png").toURI().path
    private val testPng1 = Thread.currentThread().contextClassLoader.getResource("flag_top.png").toURI().path
    private val testProperties = Thread.currentThread().contextClassLoader.getResource("vqct.properties").toURI().path
    private val testFile = Thread.currentThread().contextClassLoader.getResource("simplelogger.properties").toURI().path
    private val testOtherFile = Thread.currentThread().contextClassLoader.getResource("logger.log").toURI().path
    private val launchTest = try {
        ProgramSettings().getBrowser(Browsers.CHROME)
        true
    } catch (err: Exception) {
        false
    }
    private val errorMessage1 = "ERROR: Not specified SVG file or Base PNG file! Must be specified minimum two arguments <path_to_svg_file> <path_to_png_file>"

    @Test
    fun `run program without argument's`() {
        Assume.assumeTrue(launchTest)
        try {
            main(arrayOf())
        } catch (err: Exception) {
            assertEquals(usageMessage, err.message)
        }
    }

    @Test
    fun `run program with minimal arguments`() {
        Assume.assumeTrue(launchTest)
        main(arrayOf(testSvg, testPng))
    }

    @Test
    fun `program stopped with error message`() {
        val baOut = ByteArrayOutputStream()
        val out = PrintStream(baOut)
        val oldout = System.out
        val olderr = System.err
        System.setOut(out)
        System.setErr(out)
        main(arrayOf(testSvg, "123.png"))
        System.setOut(oldout)
        System.setErr(olderr)
        val s = String(baOut.toByteArray())
        assertTrue {
            s.contains("Working stopped with ERROR!")
        }
        baOut.reset()
        System.setOut(out)
        System.setErr(out)
        main(arrayOf("--load-settings=$testProperties",testSvg, testPng))
        System.setOut(oldout)
        System.setErr(olderr)
        val s1 = String(baOut.toByteArray())
        assertTrue {
            s1.contains("Working stopped with ERROR! For more information see logger.log file!")
        }
    }

    @Test
    fun `run programm with --setup key`() {
        val `in` = ByteArrayInputStream("\n\n\n\n\n\n\n\n\n".toByteArray())
        val old = System.`in`
        System.setIn(`in`)
        main(arrayOf("--setup"))
        System.setIn(old)
    }

    @Test
    fun `run programm with --setup key and minimal arguments`() {
        Assume.assumeTrue(launchTest)
        val `in` = ByteArrayInputStream("\n\n\n\n\n\n\n\n\n".toByteArray())
        val old = System.`in`
        System.setIn(`in`)
        main(arrayOf("--setup", testSvg, testPng))
        System.setIn(old)
    }

    @Test
    fun `run program with --help key`() {
        val baOut = ByteArrayOutputStream()
        val out = PrintStream(baOut)
        val oldout = System.out
        val olderr = System.err
        System.setOut(out)
        System.setErr(out)
        main(arrayOf("--help"))
        System.setOut(oldout)
        System.setErr(olderr)
        val s = String(baOut.toByteArray())
        assertTrue {
            s.contains(usageMessage)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `throws exception if input data for parseArguments is empty`() {
        parseArguments(arrayOf())
    }

    @Test
    fun `parseArguments return correct data`() {
        val argData = parseArguments(arrayOf("--setup", "--load-settings=$testProperties", "--browsers=chrome,opera", "--out=/out/dir", testSvg, testPng))
        assertNotNull(argData.svgFile)
        assertNotNull(argData.basePngFile)
        assertNotNull(argData.outputDirectory)
        assertNotNull(argData.browsers)
        assertNotNull(argData.settingsFile)
        assertTrue {
            argData.isSetup
        }
    }

    @Test
    fun `parseArguments return correct data if used key --out with short path or fullt path`() {
        val argData = parseArguments(arrayOf("--out=/out/dir", testSvg, testPng))
        assertNotNull(argData.outputDirectory)
        val argData1 = parseArguments(arrayOf("--out=dir/path", testSvg, testPng))
        assertNotNull(argData1.outputDirectory)
        val argData2 = parseArguments(arrayOf("--out=dir/path/", testSvg, testPng))
        assertNotNull(argData2.outputDirectory)
    }

    @Test
    fun `parseArguments return correct data if input only one key --setup`() {
        val argData = parseArguments(arrayOf("--setup"))
        assertNull(argData.svgFile)
        assertNull(argData.basePngFile)
        assertNull(argData.outputDirectory)
        assertNull(argData.browsers)
        assertNull(argData.settingsFile)
        assertTrue {
            argData.isSetup
        }
    }

    @Test
    fun `parseArguments throws if not specified minimal arguments`() {
        try {
            parseArguments(arrayOf(testSvg))
        } catch (err: Exception) {
            assertEquals(errorMessage1, err.message)
        }
        try {
            parseArguments(arrayOf(testPng))
        } catch (err: Exception) {
            assertEquals(errorMessage1, err.message)
        }
        try {
            parseArguments(arrayOf("123.svg", testPng))
        } catch (err: Exception) {
            assertTrue {
                err.message!!.startsWith("ERROR: File")
            }
        }
        try {
            parseArguments(arrayOf(testSvg, "123.png"))
        } catch (err: Exception) {
            assertTrue {
                err.message!!.startsWith("ERROR: File")
            }
        }
    }

    @Test
    fun `parseArguments throws if key value --load-settings is invalid`() {
        try {
            parseArguments(arrayOf("--load-settings=$testOtherFile", testSvg, testPng))
        } catch (err: Exception) {
            assertTrue {
                err.message!!.startsWith("ERROR: Invalid properties file")
            }
        }
        try {
            parseArguments(arrayOf("--load-settings=/123/file.properties", testSvg, testPng))
        } catch (err: Exception) {
            assertTrue {
                err.message!!.startsWith("ERROR: Properties file")
            }
        }
        try {
            parseArguments(arrayOf("--load-settings", testSvg, testPng))
        } catch (err: Exception) {
            assertTrue {
                err.message!!.startsWith("ERROR: Invalid key value")
            }
        }
        try {
            parseArguments(arrayOf("--load-settings=", testSvg, testPng))
        } catch (err: Exception) {
            assertTrue {
                err.message!!.startsWith("ERROR: Invalid key value")
            }
        }
    }

    @Test
    fun `parseArguments throws if key value --browsers is invalid`() {
        try {
            parseArguments(arrayOf("--browsers", testSvg, testPng))
        } catch (err: Exception) {
            assertTrue {
                err.message!!.startsWith("ERROR: Invalid key value --browsers.")
            }
        }
        try {
            parseArguments(arrayOf("--browsers=", testSvg, testPng))
        } catch (err: Exception) {
            assertTrue {
                err.message!!.startsWith("ERROR: Invalid key value --browsers.")
            }
        }
        try {
            parseArguments(arrayOf("--browsers=123,chrome", testSvg, testPng))
        } catch (err: Exception) {
            assertTrue {
                err.message!!.startsWith("ERROR: Invalid value")
            }
        }
    }

    @Test
    fun `parseArguments throws if key value --out is invalid`() {
        try {
            parseArguments(arrayOf("--out", testSvg, testPng))
        } catch (err: Exception) {
            assertTrue {
                err.message!!.startsWith("ERROR: Invalid key value --out.")
            }
        }
        try {
            parseArguments(arrayOf("--out=", testSvg, testPng))
        } catch (err: Exception) {
            assertTrue {
                err.message!!.startsWith("ERROR: Invalid key value --out.")
            }
        }
    }

    @Test
    fun `parseArguments throws if not specified minimal arguments with key --setup`() {
        try {
            parseArguments(arrayOf(testSvg, "--setup"))
        } catch (err: Exception) {
            assertEquals(errorMessage1, err.message)
        }
        try {
            parseArguments(arrayOf("--setup", testPng))
        } catch (err: Exception) {
            assertEquals(errorMessage1, err.message)
        }
    }

    @Test
    fun `parseArguments throws if unexpected key value`() {
        try {
            parseArguments(arrayOf(testSvg, testPng, "-key"))
        } catch (err: Exception) {
            println(err.message)
            assertTrue {
                err.message!!.startsWith("Invalid input", ignoreCase = true)
            }
        }
    }

    @Test
    fun `create settings file`() {
        ProgramSettings()
    }

    @Test
    fun `load settings from custom file`() {
        val file = File("custom.properties")
        val prop = Properties()
        prop.setProperty(Browsers.CHROME.name, "")
        prop.store(file.outputStream(), null)
        ProgramSettings(false, File("custom.properties"))
    }

    @Test
    fun `auto creating settings in custom file`() {
        ProgramSettings(false, File("custom_2.properties"))
    }

    @Test
    fun `throws exception if parameter value is not found`() {
        val file = File("vqct_test.properties")
        val prop = Properties()
        prop.setProperty("any_data", "any_data")
        prop.store(file.outputStream(), null)
        val ps = ProgramSettings(false, file)
        try {
            ps.getBrowser(Browsers.CHROME)
        } catch (err: Exception) {
            assertEquals("Can't get file because parameter \"CHROME\" in settings file is empty.", err.message.toString())
        }
    }

    @Test(expected = FileNotFoundException::class)
    fun `throws exception if browser not found`() {
        val file = File("vqct_test.properties")
        val prop = Properties()
        prop.setProperty(Browsers.CHROME.name, "")
        prop.store(file.outputStream(), null)
        val ps = ProgramSettings(false, file)
        ps.getBrowser(Browsers.CHROME)
    }

    @Test(expected = FileNotFoundException::class)
    fun `throws exception if webdriver not found`() {
        val file = File("vqct_test.properties")
        val prop = Properties()
        prop.setProperty("${Browsers.CHROME.name}_WEBDRIVER", "")
        prop.store(file.outputStream(), null)
        val ps = ProgramSettings(false, file)
        ps.getWebDriver(Browsers.CHROME)
    }

    @Test(expected = FileNotFoundException::class)
    fun `throws exception if imagemagick not found`() {
        val file = File("vqct_test.properties")
        val prop = Properties()
        prop.setProperty("IMAGEMAGICK", "")
        prop.store(file.outputStream(), null)
        val ps = ProgramSettings(false, file)
        ps.getImageMagick()
    }

    @Test(expected = IllegalStateException::class)
    fun `throws exception while save settings file`() {
        val file = File("/prop.properties")
        ProgramSettings(false, file)
    }

    @Test
    fun `Get default imCompareFuzzValue value`() {
        val ps = ProgramSettings()
        assertEquals(15.0, ps.getImFuzz())
    }

    @Test
    fun `Get default imCompareErrCountPixelThreshold value`() {
        val ps = ProgramSettings()
        assertEquals(500, ps.getImErrPixelCountThreshold())
    }

    @Test
    fun `Throws exception while get value from parameter imCompareFuzzValue`() {
        val file = File("vqct_test.properties")
        val prop = Properties()
        prop.setProperty("imCompareFuzzValue", "12d")
        prop.store(file.outputStream(), null)
        val ps = ProgramSettings(false, file)
        try {
            ps.getImFuzz()
        } catch (err: IllegalStateException) {
            assertEquals("Value of parameter \"imCompareFuzzValue\" is not Double.", err.message.toString())
        }
    }

    @Test
    fun `Throws exception while get value from parameter imCompareErrCountPixelThreshold`() {
        val file = File("vqct_test.properties")
        val prop = Properties()
        prop.setProperty("imCompareErrCountPixelThreshold", "12d")
        prop.store(file.outputStream(), null)
        val ps = ProgramSettings(false, file)
        try {
            ps.getImErrPixelCountThreshold()
        } catch (err: IllegalStateException) {
            assertEquals("Value of parameter \"imCompareErrCountPixelThreshold\" is not Integer.", err.message.toString())
        }
    }

    @Test
    fun `Getting Image Info from file`() {
        val imageUtils = IMUtils(ProgramSettings())
        val imageInfo = imageUtils.getImageInfo(File(testPng))
        println("Image size = ${imageInfo.imageSize}")
        println("Image quality = ${imageInfo.imageQuality}")
        println("Image format = ${imageInfo.imageFormat}")
        println("Image file path = ${imageInfo.imageFile.absolutePath}")
    }

    @Test(expected = FileNotFoundException::class)
    fun `Throws exception while getting image info if file not exists`() {
        IMUtils(ProgramSettings()).getImageInfo(File("/123"))
    }

    @Test(expected = IllegalStateException::class)
    fun `Throws exception while getting image info if file not image`() {
        IMUtils(ProgramSettings()).getImageInfo(File(testFile))
    }

    @Test(expected = IllegalStateException::class)
    fun `Throws exception while getting Image Info`() {
        ImageInfo(listOf())
    }

    @Test
    fun `Cropping image`() {
        val imUtils = IMUtils(ProgramSettings())
        val testPngFile = File(testPng)
        assertEquals(true, imUtils.cropImage(
                testPngFile,
                File("testpng_cropping.png"),
                imUtils.getImageInfo(testPngFile).imageSize,
                -100, -100
        ))
    }

    @Test
    fun `Error while cropping image`() {
        val imUtils = IMUtils(ProgramSettings())
        val testPngFile = File(testFile)
        assertEquals(false, imUtils.cropImage(
                testPngFile,
                File("testpng_cropping.png"),
                Dimension(2048, 2048),
                0, 0
        ))
    }

    @Test
    fun `Trimming image`() {
        val imUtils = IMUtils(ProgramSettings())
        val testPngFile = File(testPng1)
        assertEquals(true, imUtils.trimImage(
                testPngFile,
                File("testpng1_trimming.png")
        ))
    }

    @Test
    fun `Error while trimming image`() {
        val imUtils = IMUtils(ProgramSettings())
        val testPngFile = File(testFile)
        assertEquals(false, imUtils.trimImage(
                testPngFile,
                File("testpng1_trimming.png")
        ))
    }

    @Test
    fun `Resizing image`() {
        val imUtils = IMUtils(ProgramSettings())
        val testPngFile = File(testPng)
        assertEquals(true, imUtils.resizeImage(
                testPngFile,
                File("testpng_resising.png"),
                256, 256
        ))
    }

    @Test
    fun `Error while resizing image`() {
        val imUtils = IMUtils(ProgramSettings())
        val testPngFile = File(testFile)
        assertEquals(false, imUtils.resizeImage(
                testPngFile,
                File("testpng_resising.png"),
                256, 256
        ))
    }
}
