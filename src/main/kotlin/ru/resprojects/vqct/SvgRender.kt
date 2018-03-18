package ru.resprojects.vqct

import org.apache.commons.io.FileUtils
import org.openqa.selenium.*
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.firefox.FirefoxOptions
import org.openqa.selenium.ie.InternetExplorerDriver
import org.openqa.selenium.opera.OperaDriver
import org.openqa.selenium.opera.OperaOptions
import java.io.File
import java.io.FileNotFoundException
import java.io.FileWriter
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO


/**
 * Render input svg file in browser and taking screenshot
 */
abstract class SvgRender(val settings: ProgramSettings) {

    abstract fun getScreenshot(dimension: Dimension): File

    /**
     * HTML wrapper for SVG-file
     * @param inputFile SVG-file
     * @param dimension dimension for wrapped svg-file.
     * @param patch place slash at the start for svg-file path. This is patch actual for windows-version of the Firefox browser.
     * @return html file for SVG-file
     */
    protected fun htmlWrapperCreate(inputFile: File, dimension: Dimension, patch: Boolean = false): File {
        val outFile = File("${FileUtils.getTempDirectory()}${System.getProperty("file.separator")}wrapper.html")
        val outHTML = FileWriter(outFile)
        val htmlDoc = """
        <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">
        <html>
            <head>
                <title>Getting screenshot from svg file ${inputFile.name}</title>
            </head>
            <body>
                <img name="svg_file" src="${if (patch) "/" else ""}${inputFile.absolutePath}" width="${dimension.getWidth()}" height="${dimension.getHeight()}" alt="svg_file">
            </body>
        </html>
    """.trimIndent()
        try {
            outHTML.write(htmlDoc)
            return outFile
        } catch (err: IOException) {
            throw IllegalStateException("Can not write to file ${outFile.absolutePath}")
        } finally {
            try {
                outHTML.close()
            } catch (err: IOException) {
                throw IllegalStateException("Can not write to file ${outFile.absolutePath}")
            }
        }
    }

    /**
     * Taking screenshot from browser.
     * @param driver selenium WebDriver for browser.
     * @return screenshot file
     */
    protected fun shoot(driver: WebDriver): File {
        return try {
            val screen = (driver as TakesScreenshot).getScreenshotAs(OutputType.FILE)
            val webElement = driver.findElement(By.name("svg_file"))
            val img = ImageIO.read(screen)
            val dest = img.getSubimage(webElement.location.getX(), webElement.location.getY(), webElement.size.getWidth(), webElement.size.getHeight())
            ImageIO.write(dest, "png", screen)
            driver.manage().window().size = Dimension(640, 480)
            driver.quit()
            screen
        } catch (err: Exception) {
            driver.quit()
            throw IllegalStateException("Can not get correct screenshot! Cause: ${err.message}")
        }
    }
}

/**
 * Opening svg file and taking screenshot in chrome/chromium browser.
 */
class SvgRenderByChrome(settings: ProgramSettings, private val svgFile: File): SvgRender(settings) {
    override fun getScreenshot(dimension: Dimension): File {
        try {
            System.setProperty("webdriver.chrome.driver", settings.getWebDriver(Browsers.CHROME).absolutePath)
        } catch (err: FileNotFoundException) {
            throw IllegalStateException("Web driver for Google Chrome / Chromium is not found. Webdriver may be downloaded from https://sites.google.com/a/chromium.org/chromedriver/downloads")
        }
        val options = ChromeOptions()
        try {
            options.setBinary(settings.getBrowser(Browsers.CHROME).absolutePath)
        } catch (err: FileNotFoundException) {
            throw IllegalStateException("Browser Google Chrome / Chromium is not found")
        }
        val driver = ChromeDriver(options)
        driver.manage().window().fullscreen()
        driver.get(htmlWrapperCreate(svgFile, dimension).toURI().toString())
        return shoot(driver)
    }
}

/**
 * Opening svg file and taking screenshot in Firefox browser.
 */
class SvgRenderByFirefox(settings: ProgramSettings, private val svgFile: File): SvgRender(settings) {
    override fun getScreenshot(dimension: Dimension): File {
        try {
            System.setProperty("webdriver.firefox.driver", settings.getWebDriver(Browsers.FIREFOX).absolutePath)
        } catch (err: FileNotFoundException) {
            throw IllegalStateException("Web driver for Firefox is not found. Web driver can download from https://github.com/mozilla/geckodriver/releases")
        }
        val options = FirefoxOptions()
        try {
            options.setBinary(settings.getBrowser(Browsers.FIREFOX).absolutePath)
        } catch (err: FileNotFoundException) {
            throw IllegalStateException("Browser Firefox is not found")
        }
        val driver = FirefoxDriver(options)
        driver.manage().window().fullscreen()
        if (getOSName() == TypeOfOS.WINDOWS) {
            driver.get(htmlWrapperCreate(svgFile, dimension, patch = true).toURI().toString())
        } else {
            driver.get(htmlWrapperCreate(svgFile, dimension).toURI().toString())
        }
        return shoot(driver)
    }
}

/**
 * Opening svg file and taking screenshot in Opera browser.
 */
class SvgRenderByOpera(settings: ProgramSettings, private val svgFile: File): SvgRender(settings) {
    override fun getScreenshot(dimension: Dimension): File {
        try {
            System.setProperty("webdriver.opera.driver", settings.getWebDriver(Browsers.OPERA).absolutePath)
        } catch (err: FileNotFoundException) {
            throw IllegalStateException("Web driver for Opera is not found. Webdriver may be downloaded from https://github.com/operasoftware/operachromiumdriver/releases")
        }
        val options = OperaOptions()
        try {
            options.setBinary(settings.getBrowser(Browsers.OPERA).absolutePath)
        } catch (err: FileNotFoundException) {
            throw IllegalStateException("Browser Opera is not found")
        }
        val driver = OperaDriver(options)

        driver.manage().window().fullscreen()
        driver.get(htmlWrapperCreate(svgFile, dimension).toURI().toString())
        val result = shoot(driver)
        if (getOSName() == TypeOfOS.WINDOWS) {
            Runtime.getRuntime().exec("taskkill /f /im opera.exe")
        }
        return result
    }
}

/**
 * Opening svg file and taking screenshot in IE browser.
 */
class SvgRenderByIE(settings: ProgramSettings, private val svgFile: File): SvgRender(settings) {
    override fun getScreenshot(dimension: Dimension): File {
        try {
            System.setProperty("webdriver.ie.driver", settings.getWebDriver(Browsers.IE).absolutePath)
        } catch (err: FileNotFoundException) {
            throw IllegalStateException("Web driver for IE is not found. Webdriver may be downloaded from https://github.com/SeleniumHQ/selenium/wiki/InternetExplorerDriver")
        }
        try {
            settings.getBrowser(Browsers.IE).absolutePath
        } catch (err: FileNotFoundException) {
            throw IllegalStateException("Browser IE is not found")
        }
        val driver = InternetExplorerDriver()
        driver.manage().window().fullscreen()
        driver.get(htmlWrapperCreate(svgFile, dimension).toURI().toString())
        driver.manage().timeouts().implicitlyWait(30,TimeUnit.SECONDS);
        return shoot(driver)
    }
}