package io.legado.app.model.localBook

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.utils.*
import java.io.File
import java.io.InputStream
import java.util.*
import java.nio.file.Paths
import org.apache.pdfbox.pdmodel.PDDocument

class PdfFile(var book: Book) {
    var info: MutableMap<String, Any>? = null
    var cover: InputStream? = null

    companion object {
        private var cFile: PdfFile? = null

        @Synchronized
        private fun getPdfFile(book: Book): PdfFile {
            if (cFile == null || cFile?.book?.bookUrl != book.bookUrl) {
                cFile = PdfFile(book)
                return cFile!!
            }
            cFile?.book = book
            return cFile!!
        }

        @Synchronized
        fun getChapterList(book: Book): ArrayList<BookChapter> {
            return getPdfFile(book).getChapterList()
        }

        @Synchronized
        fun getContent(book: Book, chapter: BookChapter): String? {
            return getPdfFile(book).getContent(chapter)
        }

        @Synchronized
        fun upBookInfo(book: Book, onlyCover: Boolean = false) {
            if (onlyCover) {
                return getPdfFile(book).updateCover()
            }
            return getPdfFile(book).upBookInfo()
        }
    }

    init {
    }

    private fun parseBookInfo(): Pair<MutableMap<String, Any>?, InputStream?> {
        // TODO
        return Pair(info, cover)
    }

    private fun upBookInfo() {
        val result = parseBookInfo()
        if (result.first != null) {
            val bookInfo = result.first as Map<String, Any>
            val info = bookInfo.get("ComicInfo") as Map<String, Any>? ?: null
            book.name = (info?.get("Title") ?: book.name) as String
            book.author = (info?.get("Writer") ?: book.author) as String
        }
        updateCover()
    }

    private fun updateCover() {
        // val coverFile = "${MD5Utils.md5Encode16(book.bookUrl)}.jpg"
        // val relativeCoverUrl = Paths.get("assets", book.getUserNameSpace(), "covers", coverFile).toString()
        // book.coverUrl = "/" + relativeCoverUrl.replace("\\", "/")
        // val coverUrl = Paths.get(book.workRoot(), "storage", relativeCoverUrl).toString()
        // if (!File(coverUrl).exists()) {
        //     val result = parseBookInfo()
        //     if (result.second != null) {
        //         val coverStream = result.second as InputStream
        //         FileUtils.writeInputStream(coverUrl, coverStream)
        //     }
        // }
    }

    private fun getContent(chapter: BookChapter): String? {
        return ""
    }

    private fun getChapterList(): ArrayList<BookChapter> {
        val chapterList = ArrayList<BookChapter>()
        val document = PDDocument.load(book.getLocalFile())
        for (pageIndex in 0 until document.numberOfPages) {
            val name = "output-$pageIndex.png";
            val chapter = BookChapter()
            chapter.title = name
            chapter.index = pageIndex
            chapter.bookUrl = book.bookUrl
            chapter.url = name
            chapterList.add(chapter)
        }
        book.latestChapterTitle = chapterList.lastOrNull()?.title
        book.totalChapterNum = chapterList.size
        return chapterList
    }
}