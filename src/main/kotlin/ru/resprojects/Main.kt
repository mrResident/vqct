/*
MIT License

Copyright (c) 2018 Aleksandr

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/

package ru.resprojects

import ru.resprojects.vqct.*
import mu.KotlinLogging
import java.io.File

/**
 * @author Aleksandr Aleksandrov aka mrResident
 */
private val logger = KotlinLogging.logger {}
val usageMessage = """
        Argument list error!

        Usage: [keys] <path_to_svg_file> <path_to_base_png_file>

        Overview:

        [keys]

        --setup - running setup wizard.

        --load-settings=<path_to_file_settings> - start the program with an alternative path to the settings file.

        --browsers=<list_browsers> - run the program with a list of browsers that should be launched to load svg and taking screenshot.
        Browsers list are separated by commas. Valid values for this key: chrome, firefox, opera, ie. Example: --browsers=opera,ie

        --out=<path_to_output_screenshot_dir> - full path to output directory for screenshots.
        If directory is not existing then directory and subdirectories automatically is created.
        If path is not specified then screenshots will be storage in {current_execute_dir}/{file_name_with_ext}_screenshots

        <path_to_svg_file> - path to SVG file. Example: /home/user/pics/svg_file.svg.
        If full path is not specified then used {current_execute_dir}/svg_file.svg.

        <path_to_base_png_file> - path to base (control) PNG file. Example: /home/user/pics/base_png_file.png.
        If full path is not specified then used {current_execute_dir}/base_png_file.png.
    """.trimIndent()

/**
 * Entry point for program.
 * @param args arguments
 */
fun main(args: Array<String>) {
    println("Starting work...")
    try {
        VQCT(parseArguments(args)).launch()
    } catch (err: IllegalArgumentException) {
        println("${err.message}")
        println("Working stopped with ERROR!")
    } catch (err: Exception) {
        logger.error(err) { "$err" }
        println("Working stopped with ERROR! For more information see logger.log file!")
    }
}

/**
 * Input argument list parse.
 * @param args input program argument list
 * @return ArgumentsData - parsed input data
 */
fun parseArguments(args: Array<String>): ArgumentsData {
    if (args.isEmpty()) {
        logger.error { "Arguments count error. Must be specified minimum two arguments <path_to_svg_file> <path_to_png_file>" }
        throw IllegalArgumentException(usageMessage)
    }
    val argumentsData = ArgumentsData()
    args.forEach {
        if (it.toLowerCase() == "--help") {
            throw IllegalArgumentException(usageMessage)
        }
        if (it.toLowerCase() == "--setup") {
            argumentsData.isSetup = true
        }
        if (it.endsWith(".svg", true)) {
            argumentsData.svgFile = File(it)
            if (!argumentsData.svgFile!!.exists()) {
                logger.error { "ERROR: File ${argumentsData.svgFile!!.absolutePath} is not found!" }
                throw IllegalArgumentException("ERROR: File ${argumentsData.svgFile!!.absolutePath} is not found!")
            }
        }
        if (it.endsWith(".png", true)) {
            argumentsData.basePngFile = File(it)
            if (!argumentsData.basePngFile!!.exists()) {
                logger.error { "ERROR: File ${argumentsData.basePngFile!!.absolutePath} is not found!" }
                throw IllegalArgumentException("ERROR: File ${argumentsData.basePngFile!!.absolutePath} is not found!")
            }
        }
    }
    if ((argumentsData.svgFile == null || argumentsData.basePngFile == null) && !argumentsData.isSetup) {
        logger.error { "ERROR: Not specified SVG file or Base PNG file! Must be specified minimum two arguments <path_to_svg_file> <path_to_png_file>" }
        throw IllegalArgumentException("ERROR: Not specified SVG file or Base PNG file! Must be specified minimum two arguments <path_to_svg_file> <path_to_png_file>")
    } else {
        if ((argumentsData.svgFile == null && argumentsData.basePngFile == null) && argumentsData.isSetup) {
            return argumentsData
        } else if ((argumentsData.svgFile == null || argumentsData.basePngFile == null) && argumentsData.isSetup) {
            logger.error { "ERROR: Not specified SVG file or Base PNG file! Must be specified minimum two arguments <path_to_svg_file> <path_to_png_file>" }
            throw IllegalArgumentException("ERROR: Not specified SVG file or Base PNG file! Must be specified minimum two arguments <path_to_svg_file> <path_to_png_file>")
        }
    }
    args.forEach {
        if (!(it.endsWith(".png", true) || it.endsWith(".svg", true))) {
            if (it.startsWith("--")) {
                if (it.startsWith("--load-settings", true)) {
                    if (it.split("=").size == 2 && it.split("=")[1].isNotEmpty()) {
                        argumentsData.settingsFile = File(it.split("=")[1])
                        if (argumentsData.settingsFile!!.exists()) {
                            if (argumentsData.settingsFile!!.extension != "properties") {
                                logger.error { "Invalid properties file ${argumentsData.settingsFile!!.absolutePath}!" }
                                throw IllegalArgumentException("ERROR: Invalid properties file ${argumentsData.settingsFile!!.absolutePath}!")
                            }
                        } else {
                            logger.error { "Properties file ${argumentsData.settingsFile!!.absolutePath} is not found!" }
                            throw IllegalArgumentException("ERROR: Properties file ${argumentsData.settingsFile!!.absolutePath} is not found!")
                        }
                    } else {
                        logger.error { "Invalid key value --load-settings. Must be --load-settings=<path_to_properties_file>" }
                        throw IllegalArgumentException("ERROR: Invalid key value --load-settings. Must be --load-settings=<path_to_properties_file>")
                    }
                }
                if (it.startsWith("--browsers", true)) {
                    if (it.split("=").size == 2 && it.split("=")[1].isNotEmpty()) {
                        argumentsData.browsers = it.split("=")[1].toUpperCase().split(",")
                        for (i in 0 .. (argumentsData.browsers!!.size - 1)) {
                            try {
                                Browsers.valueOf(argumentsData.browsers!![i])
                            } catch (err: Exception) {
                                logger.error { "Invalid value \"${argumentsData.browsers!![i]}\" for key --browsers. Must be --browsers=<list_of_browsers>. Browsers list are separated by commas. Valid values for this key: chrome, firefox, opera, ie." }
                                throw IllegalArgumentException("ERROR: Invalid value \"${argumentsData.browsers!![i]}\" for key --browsers. Must be --browsers=<list_of_browsers>. Browsers list are separated by commas. Valid values for this key: chrome, firefox, opera, ie.")
                            }
                        }
                    } else {
                        logger.error { "Invalid key value --browsers. Must be --browsers=<list_of_browsers>. Browsers list are separated by commas. Valid values for this key: chrome, firefox, opera, ie." }
                        throw IllegalArgumentException("ERROR: Invalid key value --browsers. Must be --browsers=<list_of_browsers>. Browsers list are separated by commas. Valid values for this key: chrome, firefox, opera, ie.")
                    }
                }
                if (it.startsWith("--out", true)) {
                    if (it.toLowerCase().split("=").size == 2 && it.split("=")[1].isNotEmpty()) {
                        argumentsData.outputDirectory = if (it.split("=")[1].endsWith("/") || it.split("=")[1].endsWith("\\")) {
                            File("${it.split("=")[1]}${argumentsData.svgFile!!.name.replace('.', '_')}_screenshots")
                        } else {
                            File("${it.split("=")[1]}${System.getProperty("file.separator")}${argumentsData.svgFile!!.name.replace('.', '_')}_screenshots")
                        }
                    } else {
                        logger.error { "Invalid key value --out. Must be --out=<path_to_output_screenshot_dir>." }
                        throw IllegalArgumentException("ERROR: Invalid key value --out. Must be --out=<path_to_output_screenshot_dir>.")
                    }
                }
            } else {
                logger.error { "Invalid input data \"$it\"" }
                throw IllegalArgumentException("Invalid input data \"$it\".\n$usageMessage")
            }
        }
    }
    if (argumentsData.outputDirectory == null) {
        argumentsData.outputDirectory = File("${System.getProperties().getProperty("user.dir")}${System.getProperty("file.separator")}${argumentsData.svgFile!!.name.replace('.', '_')}_screenshots")
    }
    if (argumentsData.browsers == null) {
        val tmp = mutableListOf<String>()
        Browsers.values().forEach {
            tmp.add(it.name)
        }
        argumentsData.browsers = tmp.toList()
    }
    return argumentsData
}

/**
 * Data storage for parse program argument list.
 * @param svgFile - input svg file. This is file will be rendered in browser.
 * @param basePngFile - input png file. This is base file for comparing with output screenshots.
 * @param outputDirectory - full path to output directory for screenshots.
 * @param settingsFile - settings file.
 * @param browsers -  list of browsers that should be launched to load svg and taking screenshot.
 * @param isSetup - flag for setup wizard.
 */
data class ArgumentsData(
        var svgFile: File? = null,
        var basePngFile: File? = null,
        var outputDirectory: File? = null,
        var settingsFile: File? = null,
        var isSetup: Boolean = false,
        var browsers: List<String>? = null
)
