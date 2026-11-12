package tkd.apptkd.export

import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.util.ReflectionHelpers
import java.io.OutputStream

@Implements(PdfDocument::class)
class ShadowPdfDocument {

    private val pages = mutableListOf<PdfDocument.Page>()

    @Implementation
    fun __constructor__() {
        // Native init bypassed in Robolectric
    }

    @Implementation
    fun startPage(pageInfo: PdfDocument.PageInfo): PdfDocument.Page {
        val canvas = Canvas()
        val page = ReflectionHelpers.callConstructor(
            PdfDocument.Page::class.java,
            ReflectionHelpers.ClassParameter.from(Canvas::class.java, canvas),
            ReflectionHelpers.ClassParameter.from(PdfDocument.PageInfo::class.java, pageInfo),
        )
        pages.add(page)
        return page
    }

    @Implementation
    @Suppress("UnusedParameter")
    fun finishPage(page: PdfDocument.Page) {
        // No-op
    }

    @Implementation
    fun writeTo(out: OutputStream) {
        out.write("%PDF-1.4\n%MockPdfContent\n%%EOF\n".toByteArray(Charsets.UTF_8))
    }

    @Implementation
    fun close() {
        pages.clear()
    }
}
