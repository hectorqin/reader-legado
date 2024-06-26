package io.legado.app.model.localBook

import io.legado.app.data.entities.Book
import io.legado.app.data.entities.BookChapter
import io.legado.app.utils.*
import okhttp3.internal.closeQuietly
import java.io.File
import java.io.InputStream
import java.util.*
import java.nio.file.Paths
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineNode

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
        if (book.tocUrl.isEmpty()) {
            book.tocUrl = "page"
        }
        if (book.tocUrl == "page") {
            return getChapterListByPage()
        }
        return getChapterListByOutline()
    }

    private fun getChapterListByPage(): ArrayList<BookChapter> {
        val chapterList = ArrayList<BookChapter>()
        val document = PDDocument.load(book.getLocalFile())
        for (pageIndex in 0 until document.numberOfPages) {
            val name = "output-$pageIndex.png";
            val chapter = BookChapter()
            chapter.title = name
            chapter.index = pageIndex
            chapter.bookUrl = book.bookUrl
            chapter.url = name
            chapter.start = pageIndex.toLong()
            chapter.end = pageIndex.toLong()
            chapterList.add(chapter)
        }
        book.latestChapterTitle = chapterList.lastOrNull()?.title
        book.totalChapterNum = chapterList.size
        document.closeQuietly()
        return chapterList
    }

    private fun getChapterListByOutline(): ArrayList<BookChapter> {
        val chapterList = ArrayList<BookChapter>()
        val document = PDDocument.load(book.getLocalFile())
        val outline = document.getDocumentCatalog().getDocumentOutline();
        if (outline == null) {
            return chapterList;
        }
        processOutline(document, chapterList, outline);
        if (chapterList.size > 0) {
            // 处理尾章的结束
            chapterList.get(chapterList.size - 1).end = document.numberOfPages.toLong();
        }
        document.closeQuietly()
        return chapterList;
    }

    private fun processOutline(document: PDDocument, chapterList: ArrayList<BookChapter>, outline: PDOutlineNode) {
        var current = outline.getFirstChild()
        while (current != null) {
            var page = current.findDestinationPage(document)
            val pageIndex = document.documentCatalog.pages.indexOf(page)
            if (chapterList.size == 0) {
                // 判断是否要加首章
                if (pageIndex >= 1) {
                    val chapter = BookChapter()
                    chapter.title = "首章"
                    chapter.index = 0
                    chapter.bookUrl = book.bookUrl
                    chapter.url = "chapter-0"
                    chapter.start = 0
                    chapter.end = pageIndex.toLong()
                    chapterList.add(chapter)
                }
            }

            if (chapterList.size > 0) {
                // 判断开始页是否和上一章开始页相同
                if (chapterList.get(chapterList.size - 1).start == pageIndex.toLong()) {
                    current = current.getNextSibling()
                    continue
                }
                val chapter = BookChapter()
                chapter.title = current.getTitle()
                chapter.index = chapterList.size
                chapter.bookUrl = book.bookUrl
                chapter.url = "chapter-" + chapterList.size
                chapter.start = pageIndex.toLong()
                chapterList.get(chapterList.size - 1).end = pageIndex.toLong() - 1;
                chapterList.add(chapter)
            }

            if (current.hasChildren()) {
                processOutline(document, chapterList, current)
            }

            current = current.getNextSibling()
        }
    }
}