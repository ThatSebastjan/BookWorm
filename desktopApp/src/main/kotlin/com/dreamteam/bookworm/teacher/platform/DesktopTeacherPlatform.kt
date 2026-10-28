package com.dreamteam.bookworm.teacher.platform

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.awtTransferable
import com.dreamteam.bookworm.domain.model.PickedFile
import com.dreamteam.bookworm.teacher.platform.TeacherPlatform
import java.awt.FileDialog
import java.awt.Font
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.print.PageFormat
import java.awt.print.Printable
import java.awt.print.PrinterException
import java.awt.print.PrinterJob
import java.io.File
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** File picking, drag and drop, clipboard, printing and sound for the teacher app on desktop. */
class DesktopTeacherPlatform(private val window: Frame) : TeacherPlatform {

    override suspend fun pickFiles(): List<PickedFile> {
        val dialog = FileDialog(window, null, FileDialog.LOAD).apply {
            isMultipleMode = true
            // Honoured on Windows and Linux; macOS shows every file.
            setFilenameFilter { _, name -> name.substringAfterLast('.', "").lowercase() in SUPPORTED_EXTENSIONS }
        }
        dialog.isVisible = true // Blocks until the teacher picks or cancels.
        return dialog.files.orEmpty().map(::pickedFile)
    }

    @OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
    @Composable
    override fun fileDropTarget(onDrop: (List<PickedFile>) -> Unit): Modifier {
        val currentOnDrop by rememberUpdatedState(onDrop)
        val target = remember {
            object : DragAndDropTarget {
                override fun onDrop(event: DragAndDropEvent): Boolean {
                    val transferable = event.awtTransferable
                    if (!transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return false
                    val files = (transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*>)
                        .orEmpty()
                        .filterIsInstance<File>()
                        .filter { it.isFile }
                    if (files.isEmpty()) return false
                    currentOnDrop(files.map(::pickedFile))
                    return true
                }
            }
        }
        return Modifier.dragAndDropTarget(shouldStartDragAndDrop = { true }, target = target)
    }

    override fun copyToClipboard(text: String): Boolean = try {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        true
    } catch (e: IllegalStateException) {
        false
    }

    override fun printJoinCode(className: String, code: String, link: String) {
        val job = PrinterJob.getPrinterJob()
        job.setPrintable(JoinCodePage(className, code, link))
        if (!job.printDialog()) return
        thread(name = "print-join-code", isDaemon = true) {
            try {
                job.print()
            } catch (e: PrinterException) {
                System.err.println("Printing the join code failed: ${e.message}")
            }
        }
    }

    private class JoinCodePage(val className: String, val code: String, val link: String) : Printable {
        override fun print(graphics: java.awt.Graphics, format: PageFormat, pageIndex: Int): Int {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE
            val g = graphics as java.awt.Graphics2D
            g.translate(format.imageableX, format.imageableY)
            val width = format.imageableWidth.toInt()
            fun centered(text: String, font: Font, y: Int) {
                g.font = font
                g.drawString(text, (width - g.fontMetrics.stringWidth(text)) / 2, y)
            }
            centered(className, Font(Font.SANS_SERIF, Font.BOLD, 28), 120)
            centered(code, Font(Font.SANS_SERIF, Font.BOLD, 96), 260)
            centered(link, Font(Font.SANS_SERIF, Font.PLAIN, 16), 320)
            return Printable.PAGE_EXISTS
        }
    }

    private companion object {
        val SUPPORTED_EXTENSIONS = setOf(
            "pdf", "doc", "docx", "odt", "rtf", "ppt", "pptx", "odp", "key", "txt", "md", "jpg", "jpeg", "png", "heic", "webp",
        )
    }
}

private fun pickedFile(file: File) = PickedFile(
    name = file.name,
    sizeBytes = file.length(),
    readBytes = { withContext(Dispatchers.IO) { file.readBytes() } },
)
